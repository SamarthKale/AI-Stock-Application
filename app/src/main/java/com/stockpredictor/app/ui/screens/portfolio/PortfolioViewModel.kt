package com.stockpredictor.app.ui.screens.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.remote.wallet.EvmChain
import com.stockpredictor.app.data.remote.wallet.WalletSession
import com.stockpredictor.app.data.remote.wallet.WalletSessionManager
import com.stockpredictor.app.data.repository.WalletDataException
import com.stockpredictor.app.data.repository.WalletPortfolio
import com.stockpredictor.app.data.repository.WalletPortfolioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the Portfolio tab shows. Replaces the old mock-driven UiState: a wallet has an extra
 *  "not connected yet" state that isn't an error and isn't empty data. */
sealed interface PortfolioScreenState {
    data class NotConnected(val walletConnectAvailable: Boolean) : PortfolioScreenState
    data class Loading(val session: WalletSession) : PortfolioScreenState
    data class Loaded(
        val session: WalletSession,
        val portfolio: WalletPortfolio,
        val refreshing: Boolean,
    ) : PortfolioScreenState
    data class Error(val session: WalletSession, val message: String) : PortfolioScreenState
}

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WalletPortfolioRepository(application)

    private val _state = MutableStateFlow<PortfolioScreenState>(
        PortfolioScreenState.NotConnected(WalletSessionManager.isWalletConnectConfigured)
    )
    val state: StateFlow<PortfolioScreenState> = _state.asStateFlow()

    private val _chainFilter = MutableStateFlow<EvmChain?>(null)
    val chainFilter: StateFlow<EvmChain?> = _chainFilter.asStateFlow()

    val walletConnectReady = WalletSessionManager.walletConnectReady
    val walletConnectConfigured: Boolean get() = WalletSessionManager.isWalletConnectConfigured

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            WalletSessionManager.session.collect { session ->
                if (session == null) {
                    loadJob?.cancel()
                    _chainFilter.value = null
                    _state.value = PortfolioScreenState.NotConnected(WalletSessionManager.isWalletConnectConfigured)
                } else {
                    load(session, keepExisting = false)
                }
            }
        }
    }

    /** Re-reads balances + prices for the connected address, keeping the old numbers on screen
     *  (with a refreshing indicator) rather than flashing back to a full-screen spinner. */
    fun refresh() {
        val session = WalletSessionManager.session.value ?: return
        load(session, keepExisting = true)
    }

    private fun load(session: WalletSession, keepExisting: Boolean) {
        loadJob?.cancel() // a newer load supersedes an in-flight one (never let a slow old one overwrite a new result)
        val previous = (_state.value as? PortfolioScreenState.Loaded)?.takeIf { keepExisting && it.session == session }
        _state.value = previous?.copy(refreshing = true) ?: PortfolioScreenState.Loading(session)
        loadJob = viewModelScope.launch {
            try {
                val portfolio = repository.load(session.address)
                _state.value = PortfolioScreenState.Loaded(session, portfolio, refreshing = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: WalletDataException) {
                _state.value = previous?.copy(refreshing = false) ?: PortfolioScreenState.Error(session, e.message.orEmpty())
            } catch (e: Exception) {
                _state.value = previous?.copy(refreshing = false)
                    ?: PortfolioScreenState.Error(session, "Something went wrong reading your wallet. Please try again.")
            }
        }
    }

    fun setChainFilter(chain: EvmChain?) {
        _chainFilter.value = chain
    }

    /** @return false if [address] isn't a valid EVM address. */
    fun connectWithAddress(address: String): Boolean =
        WalletSessionManager.setWatchAddress(getApplication(), address)

    fun disconnect() {
        WalletSessionManager.disconnect(getApplication())
    }
}
