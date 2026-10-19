package com.phani.widgets.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.LruCache
import java.io.File
import java.util.UUID
import kotlin.math.max

/** Photos people pick for photo widgets: copied into the app (small and private), and cached for drawing. */
object Photos {
    var dir: File? = null
        private set
    private val cache = LruCache<String, Bitmap>(6)

    fun init(ctx: Context) {
        if (dir == null) dir = File(ctx.applicationContext.filesDir, "photos").apply { mkdirs() }
    }

    /** Copy a picked photo in, scaled to at most 1280 px on its long side. Returns its name, or null. */
    fun save(ctx: Context, uri: Uri): String? {
        init(ctx)
        return try {
            val bmp: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { dec, info, _ ->
                    val long = max(info.size.width, info.size.height)
                    if (long > 1280) dec.setTargetSampleSize(Integer.highestOneBit(long / 1280).coerceAtLeast(1))
                    dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
                val long = max(o.outWidth, o.outHeight)
                val opts = BitmapFactory.Options().apply { inSampleSize = Integer.highestOneBit((long / 1280).coerceAtLeast(1)) }
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            }
            val name = UUID.randomUUID().toString().take(12) + ".jpg"
            File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            name
        } catch (e: Exception) { null }
    }

    fun delete(name: String?) {
        if (name == null) return
        cache.remove(name)
        try { File(dir ?: return, name).delete() } catch (_: Exception) {}
    }

    fun get(name: String?): Bitmap? {
        if (name == null) return null
        cache.get(name)?.let { return it }
        val f = File(dir ?: return null, name)
        if (!f.exists()) return null
        return try { BitmapFactory.decodeFile(f.path)?.also { cache.put(name, it) } } catch (e: Exception) { null }
    }
}
