package com.stockpredictor.app.ui.screens.learn

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.learn.LearnContent
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface PdfUiState {
    data object Loading : PdfUiState
    data class Ready(val title: String, val pageCount: Int) : PdfUiState
    data class Error(val message: String) : PdfUiState
}

/**
 * Renders a bundled PDF with the platform [PdfRenderer] (no third-party dependency). PdfRenderer
 * only allows one page open at a time, so every render is serialized behind [renderLock]; all
 * decoding runs on Dispatchers.IO. Rendered pages are kept in a small size-bounded LRU so
 * scrolling back doesn't re-render.
 */
class PdfReaderViewModel(application: Application, itemId: String) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<PdfUiState>(PdfUiState.Loading)
    val state: StateFlow<PdfUiState> = _state.asStateFlow()

    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private val renderLock = Mutex()

    // ~24 MB of bitmaps at most.
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    init {
        val item = LearnContent.byId(itemId)
        val asset = item?.assetPath
        if (item == null || asset == null) {
            _state.value = PdfUiState.Error("Document not found.")
        } else {
            viewModelScope.launch {
                _state.value = try {
                    withContext(Dispatchers.IO) {
                        // PdfRenderer needs a seekable file descriptor, which a packaged asset isn't,
                        // so copy it into the cache dir once.
                        val out = File(application.cacheDir, "learn_" + asset.substringAfterLast('/'))
                        if (!out.exists() || out.length() == 0L) {
                            application.assets.open(asset).use { input -> out.outputStream().use { input.copyTo(it) } }
                        }
                        val pfd = ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_ONLY)
                        descriptor = pfd
                        renderer = PdfRenderer(pfd)
                    }
                    PdfUiState.Ready(item.title, renderer!!.pageCount)
                } catch (e: Exception) {
                    PdfUiState.Error("This document couldn't be opened.")
                }
            }
        }
    }

    /** Renders [index] to a bitmap [widthPx] wide (height follows the page's aspect ratio). */
    suspend fun renderPage(index: Int, widthPx: Int): Bitmap? {
        val key = "$index@$widthPx"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            renderLock.withLock {
                cache.get(key)?.let { return@withLock it }
                val r = renderer ?: return@withLock null
                if (index !in 0 until r.pageCount) return@withLock null
                r.openPage(index).use { page ->
                    val height = (widthPx.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE) // PDF pages are transparent by default
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put(key, bitmap)
                    bitmap
                }
            }
        }
    }

    /** Page aspect ratio (height / width) for placeholder sizing before the bitmap is ready. */
    suspend fun pageAspect(index: Int): Float = withContext(Dispatchers.IO) {
        renderLock.withLock {
            val r = renderer ?: return@withLock 1.414f
            r.openPage(index).use { it.height.toFloat() / it.width }
        }
    }

    override fun onCleared() {
        runCatching { renderer?.close() }
        runCatching { descriptor?.close() }
        cache.evictAll()
        super.onCleared()
    }
}
