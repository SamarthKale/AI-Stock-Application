package com.stockpredictor.app.data.remote.wallet

/**
 * EVM networks the Wallet screen reads balances from, via free public JSON-RPC endpoints
 * (publicnode.com -- no API key, no account). [nativeCoinId] is the CoinGecko id used to price the
 * chain's native asset.
 */
enum class EvmChain(
    val displayName: String,
    val chainId: Int,
    val rpcUrl: String,
    val nativeSymbol: String,
    val nativeName: String,
    val nativeCoinId: String,
) {
    ETHEREUM("Ethereum", 1, "https://ethereum-rpc.publicnode.com", "ETH", "Ethereum", "ethereum"),
    POLYGON("Polygon", 137, "https://polygon-bor-rpc.publicnode.com", "POL", "POL (ex-MATIC)", "polygon-ecosystem-token"),
    BNB("BNB Chain", 56, "https://bsc-rpc.publicnode.com", "BNB", "BNB", "binancecoin"),
    ARBITRUM("Arbitrum", 42161, "https://arbitrum-one-rpc.publicnode.com", "ETH", "Ethereum", "ethereum"),
    BASE("Base", 8453, "https://base-rpc.publicnode.com", "ETH", "Ethereum", "ethereum"),
}

/** An ERC-20 token we know how to read and price. [coinId] is the CoinGecko id. */
data class Erc20Token(
    val symbol: String,
    val name: String,
    val contract: String,
    val decimals: Int,
    val coinId: String,
)

/**
 * Curated list of widely held tokens per chain. A wallet can hold any ERC-20, but reading *every*
 * token needs an indexer (paid / key-gated); this registry keeps the feature free and keyless, at
 * the cost of only showing these tokens plus each chain's native coin. Every contract address and
 * its decimals below was verified against the live chains (symbol() / decimals() eth_call) when
 * this list was written -- re-verify any new entry the same way, because a wrong address does not
 * error, it just silently reads as zero.
 */
object TokenRegistry {
    private val tokens: Map<EvmChain, List<Erc20Token>> = mapOf(
        EvmChain.ETHEREUM to listOf(
            Erc20Token("USDT", "Tether", "0xdAC17F958D2ee523a2206206994597C13D831ec7", 6, "tether"),
            Erc20Token("USDC", "USD Coin", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", 6, "usd-coin"),
            Erc20Token("DAI", "Dai", "0x6B175474E89094C44Da98b954EedeAC495271d0F", 18, "dai"),
            Erc20Token("WETH", "Wrapped Ether", "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2", 18, "weth"),
            Erc20Token("WBTC", "Wrapped Bitcoin", "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599", 8, "wrapped-bitcoin"),
            Erc20Token("LINK", "Chainlink", "0x514910771AF9Ca656af840dff83E8264EcF986CA", 18, "chainlink"),
            Erc20Token("UNI", "Uniswap", "0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984", 18, "uniswap"),
            Erc20Token("SHIB", "Shiba Inu", "0x95aD61b0a150d79219dCF64E1E6Cc01f0B64C4cE", 18, "shiba-inu"),
        ),
        EvmChain.POLYGON to listOf(
            Erc20Token("USDT", "Tether", "0xc2132D05D31c914a87C6611C10748AEb04B58e8F", 6, "tether"),
            Erc20Token("USDC", "USD Coin", "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359", 6, "usd-coin"),
            Erc20Token("DAI", "Dai", "0x8f3Cf7ad23Cd3CaDbD9735AFf958023239c6A063", 18, "dai"),
            Erc20Token("WETH", "Wrapped Ether", "0x7ceB23fD6bC0adD59E62ac25578270cFf1b9f619", 18, "weth"),
        ),
        EvmChain.BNB to listOf(
            Erc20Token("USDT", "Tether", "0x55d398326f99059fF775485246999027B3197955", 18, "tether"),
            Erc20Token("USDC", "USD Coin", "0x8AC76a51cc950d9822D68b83fE1Ad97B32Cd580d", 18, "usd-coin"),
            Erc20Token("BTCB", "Bitcoin BEP2", "0x7130d2A12B9BCbFAe4f2634d864A1Ee1Ce3Ead9c", 18, "bitcoin"),
            Erc20Token("ETH", "Ethereum (BEP-20)", "0x2170Ed0880ac9A755fd29B2688956BD959F933F8", 18, "ethereum"),
            Erc20Token("CAKE", "PancakeSwap", "0x0E09FaBB73Bd3Ade0a17ECC321fD13a19e81cE82", 18, "pancakeswap-token"),
        ),
        EvmChain.ARBITRUM to listOf(
            Erc20Token("USDT", "Tether", "0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9", 6, "tether"),
            Erc20Token("USDC", "USD Coin", "0xaf88d065e77c8cC2239327C5EDb3A432268e5831", 6, "usd-coin"),
            Erc20Token("ARB", "Arbitrum", "0x912CE59144191C1204E64559FE8253a0e49E6548", 18, "arbitrum"),
        ),
        EvmChain.BASE to listOf(
            Erc20Token("USDC", "USD Coin", "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", 6, "usd-coin"),
            Erc20Token("DAI", "Dai", "0x50c5725949A6F0c72E6C4a641F24049A917DB0Cb", 18, "dai"),
        ),
    )

    fun tokensFor(chain: EvmChain): List<Erc20Token> = tokens[chain].orEmpty()

    /** Every CoinGecko id the registry can ever ask a price for (one batched markets call). */
    val allCoinIds: List<String> by lazy {
        (EvmChain.entries.map { it.nativeCoinId } + tokens.values.flatten().map { it.coinId }).distinct()
    }
}
