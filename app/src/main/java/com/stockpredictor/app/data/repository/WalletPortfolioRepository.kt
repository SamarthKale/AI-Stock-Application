package com.stockpredictor.app.data.repository

import android.app.Application
import com.stockpredictor.app.data.remote.wallet.EvmChain
import com.stockpredictor.app.data.remote.wallet.EvmRpcClient
import com.stockpredictor.app.data.remote.wallet.RawBalance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** One asset the connected address holds on one chain, valued in USD. */
data class WalletHolding(
    val coinId: String,
    val symbol: String,
    val name: String,
    val image: String?,
    val chain: EvmChain,
    val balance: Double,
    val priceUsd: Double?,
    val valueUsd: Double,
    val change24hPercent: Double?,
)

data class WalletPortfolio(
    val address: String,
    val holdings: List<WalletHolding>,
    val totalValueUsd: Double,
    /** Value-weighted 24h change of the whole portfolio (on-chain data has no cost basis, so this
     *  is the honest "gain/loss" figure -- not an invented profit-since-purchase number). */
    val change24hPercent: Double?,
    /** Chains whose RPC didn't answer; their holdings are missing, not zero. */
    val failedChains: List<EvmChain>,
    val pricesUnavailable: Boolean,
    val pricesStale: Boolean,
)

class WalletDataException(message: String) : Exception(message)

/**
 * Real wallet portfolio: on-chain balances ([EvmRpcClient], free public RPC) x live USD prices
 * ([CoinRepository], the same cache-then-network CoinGecko path every other screen uses, so
 * wallet pricing shares its TTL cache and rate-limit budget instead of adding a second price source).
 */
class WalletPortfolioRepository(
    application: Application,
    private val rpc: EvmRpcClient = EvmRpcClient(),
    private val coins: CoinRepository = CoinRepository.getInstance(application),
) {
    /** @throws WalletDataException when no chain could be read at all. */
    suspend fun load(address: String): WalletPortfolio = coroutineScope {
        val results = EvmChain.entries
            .map { chain -> chain to async { runCatching { rpc.fetchBalances(chain, address) } } }
            .map { (chain, deferred) -> chain to deferred.await() }

        // A cancelled load must propagate as cancellation, never be mistaken for "the chain failed".
        results.forEach { (_, r) -> r.exceptionOrNull()?.let { if (it is CancellationException) throw it } }

        val failedChains = results.filter { it.second.isFailure }.map { it.first }
        if (failedChains.size == EvmChain.entries.size) {
            throw WalletDataException("Couldn't reach the blockchain networks. Check your connection and try again.")
        }
        val balances: List<RawBalance> = results.flatMap { it.second.getOrNull().orEmpty() }

        if (balances.isEmpty()) {
            return@coroutineScope WalletPortfolio(address, emptyList(), 0.0, null, failedChains, false, false)
        }

        val ids = balances.map { it.token?.coinId ?: it.chain.nativeCoinId }.distinct()
        var pricesUnavailable = false
        var pricesStale = false
        val priceById = try {
            val fetched = coins.getMarkets(ids)
            pricesStale = fetched.isStale
            fetched.data.associateBy { it.id }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            pricesUnavailable = true
            emptyMap()
        }

        val holdings = balances.map { b ->
            val coinId = b.token?.coinId ?: b.chain.nativeCoinId
            val coin = priceById[coinId]
            val amount = b.amount.toDouble()
            WalletHolding(
                coinId = coinId,
                symbol = b.token?.symbol ?: b.chain.nativeSymbol,
                name = b.token?.name ?: b.chain.nativeName,
                image = coin?.image,
                chain = b.chain,
                balance = amount,
                priceUsd = coin?.currentPrice,
                valueUsd = amount * (coin?.currentPrice ?: 0.0),
                change24hPercent = coin?.priceChangePercentage24h,
            )
        }.sortedByDescending { it.valueUsd }

        val total = holdings.sumOf { it.valueUsd }
        // Value 24h ago per holding = now / (1 + change%); portfolio change compares the two sums.
        val valueYesterday = holdings.sumOf { h ->
            val change = h.change24hPercent
            if (change == null || change <= -100.0) h.valueUsd else h.valueUsd / (1 + change / 100.0)
        }
        val change = if (valueYesterday > 0.0) (total - valueYesterday) / valueYesterday * 100.0 else null

        WalletPortfolio(address, holdings, total, change, failedChains, pricesUnavailable, pricesStale)
    }
}
