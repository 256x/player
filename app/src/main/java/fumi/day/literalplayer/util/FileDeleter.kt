package fumi.day.literalplayer.util

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume


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
        val deleted = try { context.contentResolver.delete(docUri, null, null) > 0 } catch (e: Exception) { false }
        if (deleted) return true
    }
    return false
}

private fun findInMediaStore(context: Context, path: String): Uri? {
    @Suppress("DEPRECATION")
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    val tables = listOf(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
    )
    for (tableUri in tables) {
        context.contentResolver.query(
            tableUri, arrayOf(MediaStore.MediaColumns._ID), selection, arrayOf(path), null,
        )?.use { cursor ->
            if (cursor.moveToFirst())
                return ContentUris.withAppendedId(tableUri, cursor.getLong(0))
        }
    }
    return null
}

private suspend fun scanIntoMediaStore(context: Context, paths: List<String>) {
    if (paths.isEmpty()) return
    suspendCancellableCoroutine<Unit> { cont ->
        val remaining = AtomicInteger(paths.size)
        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
            if (remaining.decrementAndGet() == 0 && cont.isActive) cont.resume(Unit)
        }
    }
}

suspend fun mediaStoreDeleteRequest(context: Context, filePaths: List<String>): PendingIntent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    var uris = filePaths.mapNotNull { findInMediaStore(context, it) }
    if (uris.isEmpty()) {
        // Files not yet indexed — scan then retry once
        scanIntoMediaStore(context, filePaths)
        uris = filePaths.mapNotNull { findInMediaStore(context, it) }
    }
    if (uris.isEmpty()) return null
    return MediaStore.createDeleteRequest(context.contentResolver, uris)
}
