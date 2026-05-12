package fumi.day.literalplayer.util

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

fun deleteAudioFile(context: Context, filePath: String): Boolean {
    if (File(filePath).delete()) return true

    val extRoot = Environment.getExternalStorageDirectory().absolutePath
    for (perm in context.contentResolver.persistedUriPermissions) {
        if (!perm.isWritePermission) continue
        val treeUri = perm.uri
        val docId = try { DocumentsContract.getTreeDocumentId(treeUri) } catch (e: Exception) { continue } ?: continue
        val parts = docId.split(":", limit = 2)
        if (parts.size < 2) continue
        val volume = parts[0]
        val rel = parts[1]
        val basePath = if (volume.equals("primary", ignoreCase = true)) {
            if (rel.isEmpty()) extRoot else "$extRoot/$rel"
        } else {
            if (rel.isEmpty()) "/storage/$volume" else "/storage/$volume/$rel"
        }
        if (!filePath.startsWith(basePath)) continue
        val volumeRoot = if (volume.equals("primary", ignoreCase = true)) extRoot else "/storage/$volume"
        val fileRel = filePath.removePrefix(volumeRoot).trimStart('/')
        val fileDocId = "$volume:$fileRel"
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, fileDocId)
        return try { context.contentResolver.delete(docUri, null, null) > 0 } catch (e: Exception) { false }
    }
    return false
}

fun mediaStoreDeleteRequest(context: Context, filePaths: List<String>): PendingIntent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    val uris = filePaths.mapNotNull { path ->
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        @Suppress("DEPRECATION")
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, arrayOf(path), null
        )?.use { cursor ->
            if (cursor.moveToFirst())
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0))
            else null
        }
    }
    if (uris.isEmpty()) return null
    return MediaStore.createDeleteRequest(context.contentResolver, uris)
}
