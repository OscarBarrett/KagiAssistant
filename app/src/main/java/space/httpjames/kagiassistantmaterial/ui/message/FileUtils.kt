package space.httpjames.kagiassistantmaterial.ui.message

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Size
import java.io.File
import java.io.FileOutputStream

fun File.to84x84ThumbFile(): File {
    val thumb = ThumbnailUtils.createImageThumbnail(this, Size(84, 84), null)

    val outFile = createTempFile("thumb_", ".webp")   // /data/local/tmp/…  (world-writable)
    FileOutputStream(outFile).use { out ->
        thumb.compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, out)
    }
    return outFile
}

fun Uri.copyToTempFile(context: Context, ext: String): File {
    val temp = File.createTempFile("attach_", ext, context.cacheDir)
    context.contentResolver.openInputStream(this).use { ins ->
        temp.outputStream().use { outs ->
            ins?.copyTo(outs)
        }
    }
    return temp
}

fun Context.getFileName(uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)

    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex)
        }
    }

    return null
}
