package com.stockpredictor.app.ui.screens.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.components.StockListTile
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun WatchlistScreen(
    onStockClick: (String) -> Unit,
    viewModel: WatchlistViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = "Watchlist")
        when (val s = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
            is UiState.Empty -> EmptyState(message = "Your watchlist is empty.", modifier = Modifier.weight(1f))
            is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.weight(1f))
            is UiState.Success -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(ClaySpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(ClaySpacing.Md),
            ) {
                items(s.data, key = { it.symbol }) { stock ->
                    WatchlistRow(
                        stock = stock,
                        onClick = { onStockClick(stock.symbol) },
                        onRemove = { viewModel.remove(stock.symbol) },
                        onMoveUp = { viewModel.moveUp(stock.symbol) },
                        onMoveDown = { viewModel.moveDown(stock.symbol) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistRow(
    stock: Stock,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ClayColor.AccentCoral.copy(alpha = 0.15f), ClayShapes.Medium)
                    .padding(horizontal = ClaySpacing.Lg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = ClayColor.AccentCoral)
            }
        },
    ) {
        StockListTile(
            stock = stock,
            onClick = onClick,
            trailing = {
                Column {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up", tint = ClayColor.TextSecondary)
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down", tint = ClayColor.TextSecondary)
                    }
                }
            },
        )
    }
}
