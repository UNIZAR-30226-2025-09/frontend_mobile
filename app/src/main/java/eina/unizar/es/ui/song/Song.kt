package eina.unizar.es.ui.song

data class Song(
    val id: Int,
    val name: String,
    val duration: Int,
    val photo_video: String,
    val url_mp3: String,
    val letra: String
){}