package com.stockpredictor.app.ui.screens.learn

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.clayShadow

private const val MAX_ZOOM = 4f

/**
 * In-app PDF reader: pages render lazily as you scroll; pinch to zoom (1x-4x), drag to pan while
 * zoomed, double-tap to reset. A floating "page n of N" pill shows reading progress.
 */
@Composable
fun PdfReaderScreen(itemId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: PdfReaderViewModel = viewModel(
        key = "pdf_$itemId",
        factory = viewModelFactory {
            initializer { PdfReaderViewModel(context.applicationContext as Application, itemId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = (state as? PdfUiState.Ready)?.title ?: "Document", onBack = onBack)
        when (val s = state) {
            PdfUiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
            is PdfUiState.Error -> ErrorState(message = s.message, onRetry = null, modifier = Modifier.weight(1f))
            is PdfUiState.Ready -> PdfPages(viewModel, s.pageCount, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PdfPages(viewModel: PdfReaderViewModel, pageCount: Int, modifier: Modifier) {
    val listState = rememberLazyListState()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }
    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    // Render at ~2x the on-screen width (capped) so zooming stays reasonably sharp.
    val renderWidthPx = (screenWidthPx * 2).coerceAtMost(1800)
    // The page showing the most pixels (not merely the first partly-visible one) is "current".
    val currentPage by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportTop = info.viewportStartOffset
            val viewportBottom = info.viewportEndOffset
            val best = info.visibleItemsInfo.maxByOrNull { item ->
                (minOf(item.offset + item.size, viewportBottom) - maxOf(item.offset, viewportTop)).coerceAtLeast(0)
            }
            (best?.index ?: listState.firstVisibleItemIndex) + 1
        }
    }

    Box(modifier = modifier.fillMaxWidth().clipToBounds()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    // Keep the zoomed content from being dragged fully off-screen.
                    val maxX = (size.width * (scale - 1f)) / 2f
                    val maxY = (size.height * (scale - 1f)) / 2f
                    translationX = offset.x.coerceIn(-maxX, maxX)
                    translationY = offset.y.coerceIn(-maxY, maxY)
                }
                // Pan only claims single-finger drags once zoomed in; at 1x the list scrolls normally.
                .transformable(state = transformState, canPan = { scale > 1f })
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero })
                },
            contentPadding = PaddingValues(ClaySpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(ClaySpacing.Md),
            userScrollEnabled = scale <= 1f,
        ) {
            itemsIndexed(List(pageCount) { it }, key = { _, i -> i }) { _, index ->
                PdfPage(viewModel, index, renderWidthPx)
            }
        }
        Text(
            text = "Page $currentPage of $pageCount",
            color = ClayColor.OnDark,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = ClaySpacing.Lg)
                .clayShadow(shape = ClayShapes.Pill, elevation = ClayElevation.Small)
                .background(ClayColor.AccentPrimary, ClayShapes.Pill)
                .padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Sm),
        )
    }
}

@Composable
private fun PdfPage(viewModel: PdfReaderViewModel, index: Int, renderWidthPx: Int) {
    val aspect by produceState(initialValue = 1.414f, index) { value = viewModel.pageAspect(index) }
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, index, renderWidthPx) {
        value = viewModel.renderPage(index, renderWidthPx)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f / aspect)
            .clayShadow(shape = ClayShapes.Small, elevation = ClayElevation.Small)
            .clip(ClayShapes.Small)
            .background(ClayColor.ClayBase),
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        if (b == null) {
            CircularProgressIndicator(color = ClayColor.AccentPrimary)
        } else {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
