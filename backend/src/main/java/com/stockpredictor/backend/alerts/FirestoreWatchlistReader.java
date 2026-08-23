package com.stockpredictor.backend.alerts;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.stockpredictor.backend.config.FirebaseAppInitializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Component;

/**
 * Production {@link WatchlistReader} — reads Firestore directly via the Admin SDK's
 * {@link FirestoreClient}, already present in the {@code firebase-admin} dependency but unused
 * before Phase 5c. This is a deliberate architectural addition (flagged in the Phase 5c plan):
 * Android's real watchlist and FCM token live only in Firestore
 * ({@code users/{uid}/watchlist/{coinId}}, {@code users/{uid}.fcmToken} — see
 * FirestoreSyncRepository.kt/FcmTokenManager.kt), not in the unpopulated, wrongly-keyed Postgres
 * {@code watchlist} table from Phase 3, so a genuinely server-scheduled alert job has no other
 * source for this data.
 *
 * <p>One {@code collectionGroup("watchlist")} query reads every user's watched coin ids in a
 * single round-trip (not one query per user), then one lookup per distinct user for their
 * {@code fcmToken}. Users with no token are excluded — there is nowhere to push to.
 */
@Component
public class FirestoreWatchlistReader implements WatchlistReader {

    private final FirebaseAppInitializer appInitializer;

    public FirestoreWatchlistReader(FirebaseAppInitializer appInitializer) {
        this.appInitializer = appInitializer;
    }

    @Override
    public List<UserWatchlist> getAllUserWatchlists() {
        try {
            appInitializer.ensureInitialized();
            Firestore firestore = FirestoreClient.getFirestore();

            QuerySnapshot watchlistDocs = firestore.collectionGroup("watchlist").get().get();
            Map<String, Set<String>> coinIdsByUid = new LinkedHashMap<>();
            for (QueryDocumentSnapshot doc : watchlistDocs.getDocuments()) {
                // Document path is users/{uid}/watchlist/{coinId} -- the grandparent reference is
                // the owning user's document.
                DocumentReference userDoc = doc.getReference().getParent().getParent();
                if (userDoc == null) continue;
                String coinId = doc.getString("coin_id");
                if (coinId == null || coinId.isBlank()) continue;
                coinIdsByUid.computeIfAbsent(userDoc.getId(), key -> new LinkedHashSet<>()).add(coinId);
            }

            List<UserWatchlist> result = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : coinIdsByUid.entrySet()) {
                String uid = entry.getKey();
                DocumentSnapshot userSnapshot = firestore.collection("users").document(uid).get().get();
                String fcmToken = userSnapshot.getString("fcmToken");
                if (fcmToken == null || fcmToken.isBlank()) continue;
                result.add(new UserWatchlist(uid, entry.getValue(), fcmToken));
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlertDataUnavailableException("Interrupted reading Firestore watchlists", e);
        } catch (ExecutionException | IOException e) {
            throw new AlertDataUnavailableException("Failed to read Firestore watchlists", e);
        }
    }
}
