package fumi.day.literalplayer.domain.model

data class FavoritesList(
    val id: Long = 0,
    val name: String,
    val tracks: List<Track> = emptyList(),
)
