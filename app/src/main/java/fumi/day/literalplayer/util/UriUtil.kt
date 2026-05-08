package fumi.day.literalplayer.util

import android.net.Uri
import android.os.Environment

fun safUriToPath(uri: Uri): String? {
    val lastSegment = uri.lastPathSegment ?: return null
    val parts = lastSegment.removePrefix("tree/").split(":")
    if (parts.size < 2) return null
    val volume = parts[0]
    val relativePath = parts[1]
    return if (volume.equals("primary", ignoreCase = true)) {
        "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
    } else {
        "/storage/$volume/$relativePath"
    }
}
