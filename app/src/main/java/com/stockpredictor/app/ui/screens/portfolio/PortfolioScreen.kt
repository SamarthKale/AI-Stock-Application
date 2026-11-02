package com.stockpredictor.app.ui.screens.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reown.appkit.ui.components.internal.AppKitComponent
import com.stockpredictor.app.data.remote.wallet.EvmChain
import com.stockpredictor.app.data.remote.wallet.WalletSession
import com.stockpredictor.app.data.remote.wallet.WalletSource
import com.stockpredictor.app.data.repository.WalletHolding
import com.stockpredictor.app.data.repository.WalletPortfolio
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayButtonVariant
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.ClayTextField
import com.stockpredictor.app.ui.components.CoinAvatar
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.components.PriceChangeChip
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.clayShadow

/**
 * Portfolio tab = the user's real on-chain wallet. Connect with MetaMask (or any WalletConnect wallet)
 * or paste a public address; balances come from free public RPC endpoints and are priced via
 * CoinGecko. The app only ever sees a public address -- never keys or a seed phrase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onCoinClick: (String) -> Unit,
    viewModel: PortfolioViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val chainFilter by viewModel.chainFilter.collectAsStateWithLifecycle()
    val wcReady by viewModel.walletConnectReady.collectAsStateWithLifecycle()

    var showConnectSheet by remember { mutableStateOf(false) }
    var showAddressSheet by remember { mutableStateOf(false) }

    // Once a wallet is connected, any open connect sheet is done.
    LaunchedEffect(state) {
        if (state !is PortfolioScreenState.NotConnected) {
            showConnectSheet = false
            showAddressSheet = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(
            title = "My Wallet",
            trailingIcon = if (state !is PortfolioScreenState.NotConnected) Icons.Filled.Refresh else null,
            onTrailingClick = if (state !is PortfolioScreenState.NotConnected) viewModel::refresh else null,
        )
        when (val s = state) {
            is PortfolioScreenState.NotConnected -> ConnectHero(
                walletConnectAvailable = s.walletConnectAvailable && wcReady,
                walletConnectConfigured = viewModel.walletConnectConfigured,
                onConnectMetaMask = { showConnectSheet = true },
                onEnterAddress = { showAddressSheet = true },
                modifier = Modifier.weight(1f),
            )
            is PortfolioScreenState.Loading -> LoadingState(modifier = Modifier.weight(1f))
            is PortfolioScreenState.Error -> ErrorState(message = s.message, onRetry = viewModel::refresh, modifier = Modifier.weight(1f))
            is PortfolioScreenState.Loaded -> WalletContent(
                session = s.session,
                portfolio = s.portfolio,
                refreshing = s.refreshing,
                chainFilter = chainFilter,
                onChainFilter = viewModel::setChainFilter,
                onCoinClick = onCoinClick,
                onDisconnect = viewModel::disconnect,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showConnectSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showConnectSheet = false },
            sheetState = sheetState,
            containerColor = ClayColor.ClayBase,
        ) {
            // Reown's own connect UI: wallet list (MetaMask featured), QR code, deep link.
            AppKitComponent(shouldOpenChooseNetwork = false, closeModal = { showConnectSheet = false })
        }
    }
    if (showAddressSheet) {
        AddressSheet(
            onDismiss = { showAddressSheet = false },
            onSubmit = { address -> viewModel.connectWithAddress(address) },
        )
    }
}

// ================================================================================================
// Not connected
// ================================================================================================

@Composable
private fun ConnectHero(
    walletConnectAvailable: Boolean,
    walletConnectConfigured: Boolean,
    onConnectMetaMask: () -> Unit,
    onEnterAddress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ClaySpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.Lg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clayShadow(shape = ClayShapes.Hero, elevation = ClayElevation.Default)
                .background(Brush.verticalGradient(listOf(ClayColor.AccentGold, ClayColor.AccentPrimary)), ClayShapes.Hero)
                .padding(ClaySpacing.Xl),
        ) {
            Column {
                Text("See your real crypto", color = ClayColor.OnDark, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(ClaySpacing.Sm))
                Text(
                    "Connect MetaMask to view the live balances and value of everything in your wallet " +
                        "across Ethereum, Polygon, BNB Chain, Arbitrum and Base.",
                    color = ClayColor.OnDarkMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(ClaySpacing.Lg))
                if (walletConnectConfigured) {
                    ClayButton(
                        text = if (walletConnectAvailable) "Connect MetaMask" else "Getting ready…",
                        onClick = onConnectMetaMask,
                        enabled = walletConnectAvailable,
                        variant = ClayButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(ClaySpacing.Sm))
                }
                ClayButton(
                    text = "Enter a wallet address",
                    onClick = onEnterAddress,
                    variant = if (walletConnectConfigured) ClayButtonVariant.Text else ClayButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        ClayCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Read-only and private", color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(ClaySpacing.Xs))
                Text(
                    "We only see your public wallet address. We never ask for your seed phrase or private key, " +
                        "and can't move your funds.",
                    color = ClayColor.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!walletConnectConfigured) {
                    Spacer(Modifier.height(ClaySpacing.Sm))
                    Text(
                        "One-tap MetaMask connect isn't set up in this build yet, but you can paste any public address.",
                        color = ClayColor.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSheet(onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
    var address by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ClayColor.ClayBase,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ClaySpacing.Lg).padding(bottom = ClaySpacing.Xl).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(ClaySpacing.Md),
        ) {
            Text("Wallet address", color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text(
                "Paste the public address from MetaMask (it starts with 0x). Only the address is stored on this device.",
                color = ClayColor.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            ClayTextField(
                value = address,
                onValueChange = { address = it; error = null },
                label = "0x…",
                keyboardType = KeyboardType.Ascii,
                errorText = error,
                modifier = Modifier.fillMaxWidth(),
            )
            ClayButton(
                text = "View portfolio",
                onClick = { if (!onSubmit(address)) error = "That isn't a valid wallet address (0x followed by 40 characters)." },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ================================================================================================
// Connected
// ================================================================================================

@Composable
private fun WalletContent(
    session: WalletSession,
    portfolio: WalletPortfolio,
    refreshing: Boolean,
    chainFilter: EvmChain?,
    onChainFilter: (EvmChain?) -> Unit,
    onCoinClick: (String) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = portfolio.holdings.filter { chainFilter == null || it.chain == chainFilter }
    val chainsWithHoldings = portfolio.holdings.map { it.chain }.distinct()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Generous bottom padding so the last item can scroll clear of the floating Ask-AI bubble.
        contentPadding = PaddingValues(start = ClaySpacing.Lg, end = ClaySpacing.Lg, top = ClaySpacing.Lg, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.Md),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayShadow(shape = ClayShapes.Hero, elevation = ClayElevation.Default)
                    .background(Brush.verticalGradient(listOf(ClayColor.AccentGold, ClayColor.AccentPrimary)), ClayShapes.Hero)
                    .padding(ClaySpacing.Xl),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = (if (session.source == WalletSource.WALLETCONNECT) "Connected · " else "Watching · ") + shortAddress(session.address),
                            color = ClayColor.OnDarkMuted,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (refreshing) CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), color = ClayColor.OnDark, strokeWidth = 2.dp)
                    }
                    Spacer(Modifier.height(ClaySpacing.Xs))
                    Text(formatUsd(portfolio.totalValueUsd), color = ClayColor.OnDark, style = MaterialTheme.typography.displaySmall)
                    portfolio.change24hPercent?.let {
                        Spacer(Modifier.height(ClaySpacing.Sm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Light pill: the usual coral/mint chip has poor contrast on the brown hero.
                            Text(
                                text = (if (it >= 0) "▲ " else "▼ ") + String.format("%.2f%%", kotlin.math.abs(it)),
                                color = ClayColor.OnDark,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .background(ClayColor.OnDark.copy(alpha = 0.22f), ClayShapes.Pill)
                                    .padding(horizontal = ClaySpacing.Md, vertical = ClaySpacing.Xs),
                            )
                            Spacer(Modifier.width(ClaySpacing.Sm))
                            Text("past 24h", color = ClayColor.OnDarkMuted, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        if (portfolio.failedChains.isNotEmpty()) {
            item {
                Text(
                    "Couldn't read ${portfolio.failedChains.joinToString { it.displayName }} — its assets aren't included. Tap refresh to retry.",
                    color = ClayColor.AccentCoral,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (portfolio.pricesUnavailable) {
            item { Text("Live prices are unavailable right now, so values show as $0.00.", color = ClayColor.AccentCoral, style = MaterialTheme.typography.bodySmall) }
        } else if (portfolio.pricesStale) {
            item { Text("Showing recently cached prices — reconnect to refresh.", color = ClayColor.TextSecondary, style = MaterialTheme.typography.labelMedium) }
        }
        if (chainsWithHoldings.size > 1) {
            item {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Sm)) {
                    ChainChip("All", selected = chainFilter == null) { onChainFilter(null) }
                    chainsWithHoldings.forEach { chain -> ChainChip(chain.displayName, selected = chainFilter == chain) { onChainFilter(chain) } }
                }
            }
        }
        if (portfolio.holdings.isEmpty()) {
            item {
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No supported assets found on this address yet. We read the native coin plus popular tokens " +
                            "(USDT, USDC, DAI, WETH, WBTC, LINK, UNI, SHIB, ARB, CAKE) on five networks.",
                        color = ClayColor.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        items(visible, key = { "${it.chain.chainId}_${it.symbol}_${it.coinId}" }) { holding ->
            HoldingRow(holding = holding, onClick = { onCoinClick(holding.coinId) })
        }
        item {
            ClayButton(
                text = if (session.source == WalletSource.WALLETCONNECT) "Disconnect wallet" else "Remove wallet",
                onClick = onDisconnect,
                variant = ClayButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().padding(top = ClaySpacing.Md),
            )
        }
    }
}

@Composable
private fun ChainChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) ClayColor.OnDark else ClayColor.TextPrimary,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(if (selected) ClayColor.AccentPrimary else ClayColor.ClayBase, ClayShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Sm),
    )
}

@Composable
private fun HoldingRow(holding: WalletHolding, onClick: () -> Unit) {
    ClayCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), padding = ClaySpacing.Md) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CoinAvatar(imageUrl = holding.image, symbol = holding.symbol)
            Spacer(Modifier.width(ClaySpacing.Md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(holding.symbol, color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(ClaySpacing.Sm))
                    Text(
                        holding.chain.displayName,
                        color = ClayColor.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.background(ClayColor.Sand, ClayShapes.Pill).padding(horizontal = ClaySpacing.Sm, vertical = 2.dp),
                    )
                }
                Text(
                    text = "${formatQuantity(holding.balance)} ${holding.symbol}",
                    color = ClayColor.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatUsd(holding.valueUsd), color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(ClaySpacing.Xs))
                holding.change24hPercent?.let { PriceChangeChip(changePercent = it) }
            }
        }
    }
}

private fun shortAddress(address: String): String = "${address.take(6)}…${address.takeLast(4)}"

private fun formatUsd(value: Double): String = "$" + String.format("%,.2f", value)

/** Crypto balances are fractional and span huge ranges (0.000012 BTC vs 4,500,000 SHIB): fewer
 *  decimals for big numbers, more for small, trailing zeros trimmed. */
private fun formatQuantity(quantity: Double): String {
    val decimals = when {
        quantity >= 1_000 -> 2
        quantity >= 1 -> 4
        else -> 6
    }
    return String.format("%,.${decimals}f", quantity).trimEnd('0').trimEnd('.')
}
