package com.stockpredictor.app.data.remote.firebase

import android.content.Context
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.stockpredictor.app.data.local.dao.WatchlistDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Bounds how long a background Firestore push may run before we give up on it for this
 *  attempt — a write made while offline must never hang the background sync coroutine forever. */
private const val PUSH_TIMEOUT_MS = 10_000L

/**
 * Firestore schema (mirrors the SQLite entity shapes, per Phase 2.5):
 *   users/{uid}/watchlist/{coinId} — coin_id, symbol, name, image_url, added_at, sort_order,
 *     updated_at (server timestamp). Documents are keyed by CoinGecko coin id, not ticker
 *     symbol (Crypto Predictor migration) — a symbol alone isn't a safe cross-device key.
 *   users/{uid}/portfolio/{holdingId} — reserved for when Portfolio gets local SQLite persistence;
 *     not implemented here since Phase 2 never added a PortfolioDao to sync against.
 *
 * SQLite (via [WatchlistDao]) stays the offline cache; Firestore is the source of truth once
 * online. Every write goes through this repository — never through the DAO directly and never
 * from a ViewModel — so local + remote never drift out of sync with each other.
 *
 * Conflict resolution — server timestamps only, never device clocks: [WatchlistDao.insert] and
 * [WatchlistDao.updateSortOrders] stamp a fresh local row's `updated_at` with the device clock
 * purely as a placeholder, because something has to be there before the row has ever reached
 * the server. The instant a push to Firestore succeeds, [pushWatchlistItem] reads the row back
 * and overwrites that placeholder with the real, server-resolved timestamp — so by the time
 * this device (or [startListening]'s merge logic) ever compares two `updated_at` values against
 * each other, both sides are guaranteed to be server timestamps. A skewed device clock can only
 * ever affect the local placeholder in the brief window before its own push confirms; it can
 * never masquerade as the authority a last-write-wins comparison relies on.
 */
class FirestoreSyncRepository private constructor(context: Context) {
    private val watchlistDao = WatchlistDao(context.applicationContext)
    private val firestore = FirebaseFirestore.getInstance().apply {
        // Explicit, even though it's Firestore's Android SDK default: this is what queues
        // local writes made while offline and replays them once connectivity returns.
        firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
    }
    private var listenerRegistration: ListenerRegistration? = null

    private fun watchlistCollection(uid: String) =
        firestore.collection("users").document(uid).collection("watchlist")

    /**
     * Starts (or restarts) a real-time listener on this user's watchlist collection. The first
     * callback delivers the full existing collection as a batch of ADDED changes, which doubles
     * as the "pull remote → merge into local" step Task 4 describes — no separate one-shot pull
     * is needed. Must be paired with [stopListening] on sign-out so a second account's session
     * never inherits the previous user's listener.
     */
    fun startListening(uid: String) {
        stopListening()
        listenerRegistration = watchlistCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (change in snapshot.documentChanges) {
                    // Skip the optimistic local echo of our own pending write — its updated_at
                    // is still unresolved (server timestamp fields read null while pending), so
                    // there's nothing valid to compare yet. pushWatchlistItem's own read-back
                    // is what reconciles the local row once the write is actually confirmed.
                    if (change.document.metadata.hasPendingWrites()) continue
                    val coinId = change.document.id
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            // Always a Firestore server timestamp, per this file's write path —
                            // never a value any client computed from its own device clock.
                            val remoteUpdatedAt = change.document.getTimestamp("updated_at")?.toDate()?.time ?: 0L
                            val local = watchlistDao.getByCoinId(coinId)
                            if (local == null || remoteUpdatedAt >= local.updatedAt) {
                                watchlistDao.upsertFromRemote(
                                    coinId = coinId,
                                    symbol = change.document.getString("symbol") ?: coinId,
                                    name = change.document.getString("name"),
                                    imageUrl = change.document.getString("image_url"),
                                    addedAt = change.document.getLong("added_at") ?: System.currentTimeMillis(),
                                    sortOrder = (change.document.getLong("sort_order") ?: 0L).toInt(),
                                    updatedAt = remoteUpdatedAt,
                                )
                            }
                        }
                        DocumentChange.Type.REMOVED -> watchlistDao.delete(coinId)
                    }
                }
                changeSignal.emit(Unit)
            }
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /**
     * Wipes the local watchlist cache. Call this on sign-out, after [stopListening] and before
     * a different account can sign in on this device — otherwise the previous user's rows stay
     * in SQLite forever, since a fresh account's Firestore collection never emits REMOVED
     * events for coins it never had in the first place (there's nothing to "remove"). On the
     * next sign-in, [startListening] repopulates the table from that user's own collection, so
     * this is a pure cache invalidation, not a real data loss — Firestore is still the source
     * of truth once online.
     */
    suspend fun clearLocalCache() {
        watchlistDao.clearAll()
        changeSignal.emit(Unit)
    }

    /**
     * Local write lands first and [changeSignal] fires immediately — the UI reflects the change
     * instantly regardless of connectivity. The Firestore push runs afterwards, in the
     * background, never gating this call: a slow or offline network must never stall the
     * watchlist toggle (see the FCM-token hang this exact pattern was already adopted for).
     */
    suspend fun addToWatchlist(uid: String?, coinId: String, symbol: String, name: String?, imageUrl: String?) {
        watchlistDao.insert(coinId, symbol, name, imageUrl)
        changeSignal.emit(Unit)
        if (uid != null) firePushInBackground(uid, coinId)
    }

    suspend fun removeFromWatchlist(uid: String?, coinId: String) {
        watchlistDao.delete(coinId)
        changeSignal.emit(Unit)
        if (uid != null) {
            val collection = watchlistCollection(uid)
            CoroutineScope(Dispatchers.IO).launch {
                withTimeoutOrNull(PUSH_TIMEOUT_MS) {
                    runCatching { collection.document(coinId).delete().await() }
                }
            }
        }
    }

    suspend fun reorderWatchlist(uid: String?, orderedCoinIds: List<String>) {
        watchlistDao.updateSortOrders(orderedCoinIds)
        changeSignal.emit(Unit)
        if (uid != null) orderedCoinIds.forEach { firePushInBackground(uid, it) }
    }

    private fun firePushInBackground(uid: String, coinId: String) {
        CoroutineScope(Dispatchers.IO).launch { pushWatchlistItem(uid, coinId) }
    }

    /**
     * Pushes the current local row to Firestore, then reads it back and corrects the local
     * `updated_at` placeholder to the real server-resolved timestamp — see the class doc for
     * why this is the step that keeps every future last-write-wins comparison skew-proof.
     * Bounded by [PUSH_TIMEOUT_MS] so an offline device abandons this attempt rather than
     * hanging; Firestore's own offline queue (enabled above) still redelivers the write once
     * connectivity returns, and this same correction runs again on the next successful push.
     */
    private suspend fun pushWatchlistItem(uid: String, coinId: String) {
        val entity = watchlistDao.getByCoinId(coinId) ?: return
        val docRef = watchlistCollection(uid).document(coinId)
        val data = mapOf(
            "coin_id" to entity.coinId,
            "symbol" to entity.symbol,
            "name" to entity.name,
            "image_url" to entity.imageUrl,
            "added_at" to entity.addedAt,
            "sort_order" to entity.sortOrder,
            "updated_at" to FieldValue.serverTimestamp(),
        )
        val resolvedUpdatedAt = withTimeoutOrNull(PUSH_TIMEOUT_MS) {
            runCatching {
                docRef.set(data).await()
                // set() only completes once the backend has committed the write, so this read
                // resolves the real server timestamp — not a client-side estimate.
                docRef.get().await().getTimestamp("updated_at")?.toDate()?.time
            }.getOrNull()
        } ?: return

        // Re-fetch rather than reuse `entity`: the row may have been removed or changed again
        // locally while this push was in flight.
        watchlistDao.getByCoinId(coinId)?.let { current ->
            watchlistDao.update(current.copy(updatedAt = resolvedUpdatedAt))
        }
    }

    companion object {
        /** Screens/ViewModels observe this to refresh after a background sync change (see WatchlistViewModel/HomeViewModel). */
        private val changeSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val changes = changeSignal.asSharedFlow()

        @Volatile private var instance: FirestoreSyncRepository? = null

        fun getInstance(context: Context): FirestoreSyncRepository =
            instance ?: synchronized(this) {
                instance ?: FirestoreSyncRepository(context).also { instance = it }
            }
    }
}
