package com.stockpredictor.app.data.remote.wallet

import android.app.Application
import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.presets.AppKitChainsPresets
import com.stockpredictor.app.BuildConfig
import com.stockpredictor.app.data.local.dao.SettingsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How the user's wallet address got here. Both are read-only: the app never holds keys. */
enum class WalletSource { WALLETCONNECT, WATCH_ONLY }

data class WalletSession(val address: String, val source: WalletSource)

/**
 * The single source of truth for "which wallet is the Portfolio showing".
 *
 *  * **WalletConnect (Reown AppKit)** -- the user taps "Connect MetaMask" (or any other wallet), approves in the
 *    wallet app, and AppKit hands back their *public address*. Needs a free Reown project id
 *    (`REOWN_PROJECT_ID` in local.properties); when it's blank this path is simply unavailable and
 *    [isWalletConnectConfigured] is false.
 *  * **Watch-only address** -- the user pastes/types any public address. Needs nothing at all, so the
 *    real-balances feature works even with no project id and no wallet app installed.
 *
 * In both cases the app only ever learns a public address -- never a key or seed phrase -- and
 * balances are read from public RPC endpoints (see [EvmRpcClient]). The WalletConnect session wins
 * over a watch-only address when both exist.
 */
object WalletSessionManager {
    private const val TAG = "WalletSession"
    private const val KEY_WATCH_ADDRESS = "wallet_watch_address"

    /** Shown by the wallet app when it asks to connect. Set this to the domain registered for the
     *  project in the Reown dashboard so wallets can mark the request "verified". */
    private const val APP_URL = "https://aicryptopredictor.app"
    /** MetaMask's id in the WalletConnect explorer registry: listed first ('recommended') in the connect sheet. */
    private const val METAMASK_WALLET_ID = "c57ca95b47569778a828d19178114f4db188b89b763c899ba0be274e97267d96"
    private const val APP_REDIRECT = "stockpredictor-wc://request" // matches the manifest intent filter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _session = MutableStateFlow<WalletSession?>(null)
    val session: StateFlow<WalletSession?> = _session.asStateFlow()

    private val _walletConnectReady = MutableStateFlow(false)
    /** True once AppKit finished initialising and the connect sheet can be opened. */
    val walletConnectReady: StateFlow<Boolean> = _walletConnectReady.asStateFlow()

    val isWalletConnectConfigured: Boolean get() = BuildConfig.REOWN_PROJECT_ID.isNotBlank()

    @Volatile private var started = false
    @Volatile private var wcAddress: String? = null
    @Volatile private var watchAddress: String? = null

    /** Idempotent; called once from MainActivity. Never throws -- a wallet problem must not stop the app launching. */
    fun initialize(app: Application) {
        if (started) return
        started = true

        scope.launch {
            watchAddress = runCatching { SettingsDao(app).getString(KEY_WATCH_ADDRESS) }.getOrNull()
                ?.takeIf(::isValidEvmAddress)
            publish()
        }

        if (!isWalletConnectConfigured) return
        runCatching {
            CoreClient.initialize(
                application = app,
                projectId = BuildConfig.REOWN_PROJECT_ID,
                metaData = Core.Model.AppMetaData(
                    name = "AI Crypto Predictor",
                    description = "Crypto market data and AI price predictions",
                    url = APP_URL,
                    icons = emptyList(),
                    redirect = APP_REDIRECT,
                ),
                connectionType = ConnectionType.AUTOMATIC,
                onError = { error -> Log.w(TAG, "WalletConnect core init failed: ${error.throwable.message}") },
            )
            AppKit.initialize(
                init = Modal.Params.Init(core = CoreClient, recommendedWalletsIds = listOf(METAMASK_WALLET_ID)),
                onSuccess = {
                    configureChains()
                    AppKit.setDelegate(delegate)
                    wcAddress = AppKit.getAccount()?.address?.takeIf(::isValidEvmAddress) // restores a previous session
                    _walletConnectReady.value = true
                    publish()
                },
                onError = { error -> Log.w(TAG, "AppKit init failed: ${error.throwable.message}") },
            )
        }.onFailure { Log.w(TAG, "WalletConnect unavailable", it) }
    }

    private fun configureChains() {
        val wanted = EvmChain.entries.map { it.chainId.toString() }.toSet()
        val presets = AppKitChainsPresets.ethChains.values.toList()
        AppKit.setChains(presets.filter { it.chainReference in wanted }.ifEmpty { presets })
    }

    private val delegate = object : AppKit.ModalDelegate {
        override fun onSessionApproved(approvedSession: Modal.Model.ApprovedSession) {
            wcAddress = AppKit.getAccount()?.address?.takeIf(::isValidEvmAddress)
            publish()
        }
        override fun onSessionDelete(deletedSession: Modal.Model.DeletedSession) {
            wcAddress = null
            publish()
        }
        override fun onSessionRejected(rejectedSession: Modal.Model.RejectedSession) = Unit
        override fun onSessionUpdate(updatedSession: Modal.Model.UpdatedSession) {
            wcAddress = AppKit.getAccount()?.address?.takeIf(::isValidEvmAddress) ?: wcAddress
            publish()
        }
        override fun onSessionEvent(sessionEvent: Modal.Model.SessionEvent) = Unit
        override fun onSessionExtend(session: Modal.Model.Session) = Unit
        override fun onSessionRequestResponse(response: Modal.Model.SessionRequestResponse) = Unit
        override fun onProposalExpired(proposal: Modal.Model.ExpiredProposal) = Unit
        override fun onRequestExpired(request: Modal.Model.ExpiredRequest) = Unit
        override fun onConnectionStateChange(state: Modal.Model.ConnectionState) = Unit
        override fun onError(error: Modal.Model.Error) {
            Log.w(TAG, "AppKit error: ${error.throwable.message}")
        }
    }

    private fun publish() {
        _session.value = wcAddress?.let { WalletSession(it, WalletSource.WALLETCONNECT) }
            ?: watchAddress?.let { WalletSession(it, WalletSource.WATCH_ONLY) }
    }

    /** Saves a pasted public address as a watch-only wallet. Returns false if it isn't a valid EVM address. */
    fun setWatchAddress(app: Application, address: String): Boolean {
        val trimmed = address.trim()
        if (!isValidEvmAddress(trimmed)) return false
        watchAddress = trimmed
        publish()
        scope.launch { runCatching { SettingsDao(app).setString(KEY_WATCH_ADDRESS, trimmed) } }
        return true
    }

    /** Ends the WalletConnect session (if any) and forgets any watch-only address. Used by both the
     *  Wallet screen's Disconnect button and logout, so one account's wallet never leaks to the next. */
    fun disconnect(app: Application) {
        wcAddress = null
        watchAddress = null
        publish()
        scope.launch { runCatching { SettingsDao(app).setString(KEY_WATCH_ADDRESS, "") } }
        if (_walletConnectReady.value) {
            runCatching { AppKit.disconnect(onSuccess = {}, onError = { Log.w(TAG, "disconnect failed: ${it.message}") }) }
        }
    }
}
