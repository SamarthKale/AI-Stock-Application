package com.stockpredictor.app.data.remote.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Uploads the user's profile photo to Firestore (`users/{uid}.photo_b64`) as a small
 * downscaled JPEG, so it syncs across devices without needing Firebase Storage (which
 * requires the Blaze billing plan). Capped at [MAX_EDGE_PX] so the document stays far
 * under Firestore's 1MiB limit.
 */
class ProfilePhotoRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun doc() = auth.currentUser?.uid?.let { firestore.collection("users").document(it) }

    /** Reads the picked image, downscales + compresses it, uploads it, and returns the bitmap. */
    suspend fun upload(context: Context, uri: Uri): Result<Bitmap> = runCatching {
        val doc = doc() ?: error("Not signed in")
        val (bitmap, bytes) = withContext(Dispatchers.IO) {
            val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: error("Could not read image")
            val scale = MAX_EDGE_PX.toFloat() / maxOf(source.width, source.height)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
            } else source
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            scaled to out.toByteArray()
        }
        doc.set(mapOf("photo_b64" to Base64.encodeToString(bytes, Base64.NO_WRAP)), SetOptions.merge()).await()
        bitmap
    }

    suspend fun load(): Bitmap? = runCatching {
        val b64 = doc()?.get()?.await()?.getString("photo_b64") ?: return null
        withContext(Dispatchers.IO) {
            val bytes = Base64.decode(b64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }.getOrNull()

    private companion object {
        const val MAX_EDGE_PX = 256
        const val JPEG_QUALITY = 85
    }
}
