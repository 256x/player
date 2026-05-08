package fumi.day.literalplayer.domain.model

data class Track(
    val id: String,
    val path: String,
    val fileName: String,
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val mimeType: String,
    val lastModified: Long,
)

val Track.displayTitle: String get() = title.ifBlank { fileName }
val Track.displayArtist: String get() = artist.ifBlank { "Unknown Artist" }
val Track.displayAlbum: String get() = album.ifBlank { "Unknown Album" }

fun Long.toDisplayDuration(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
