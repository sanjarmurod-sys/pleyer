package uz.pleyer.premium

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.util.Locale

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val dur: Long,
    val path: String,
    val added: Long,
    val displayName: String = "",
    val mime: String = "",
    val customUri: Uri? = null
) {
    val uri: Uri get() = customUri ?: ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val art: Uri get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

object Library {
    var hidden = 0

    // Infinix HOT 60 Pro+ uchun boshlang'ich qo'ng'iroq yozuvlari papkalari.
    private val defaultHiddenPaths = setOf(
        "music/phonerecord",
        "music/phone record",
        "music/phone records",
        "recordings/call",
        "recordings/call recordings"
    )

    // Umumiy "Recordings" papkasini yashirmaymiz: u yerda foydalanuvchi yozgan ovozlar ham bo'lishi mumkin.
    private val callFolderNames = setOf(
        "call", "calls", "call recording", "call recordings", "call_recording", "call_recordings",
        "callrecord", "callrecords", "callrecorder", "callrecording", "callrecordings",
        "phonerecord", "phone record", "phone records", "phone_record", "phone_recording",
        "call_rec", "call_recorder", "запись звонков", "записи звонков",
        "запись разговоров", "записи разговоров", "звонки"
    )

    private val callName = Regex(
        "(^|[ ._-])(call[ ._-]?(record|recording|rec)|phone[ ._-]?(record|recording)|phonerecord)([ ._-]|$)",
        RegexOption.IGNORE_CASE
    )

    fun isCall(relativePath: String, displayName: String, customHidden: Set<String> = Store.hiddenPaths): Boolean {
        val normalized = normalize(relativePath)
        val hiddenPaths = defaultHiddenPaths + customHidden.map { normalize(it) }
        if (hiddenPaths.any { hidden -> hidden.isNotBlank() && (normalized == hidden || normalized.startsWith("$hidden/")) }) {
            return true
        }
        val segments = normalized.split('/').filter { it.isNotBlank() }
        if (segments.any { it in callFolderNames }) return true
        val fileStem = displayName.substringBeforeLast('.', displayName).lowercase(Locale.ROOT)
        return callName.containsMatchIn(fileStem)
    }

    fun load(context: Context, minSec: Int): List<Track> {
        hidden = 0
        val out = ArrayList<Track>()
        val selection = StringBuilder(
            "${MediaStore.Audio.Media.IS_RINGTONE}=0 AND " +
                "${MediaStore.Audio.Media.IS_NOTIFICATION}=0 AND " +
                "${MediaStore.Audio.Media.IS_ALARM}=0"
        )
        if (Build.VERSION.SDK_INT >= 31) {
            selection.append(" AND ${MediaStore.Audio.Media.IS_RECORDING}=0")
        }

        val columns = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns.add(MediaStore.MediaColumns.RELATIVE_PATH)
        }

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            columns.toTypedArray(),
            selection.toString(),
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else -1

            while (cursor.moveToNext()) {
                val relativePath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                val displayName = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                val mime = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else ""
                val duration = cursor.getLong(durationCol)

                // MIME/format is intentionally not a filter: AMR, 3GP, AWB, MP3, WAV, etc.
                // are excluded only when their folder/name identifies a call recording.
                if (duration < minSec * 1000L || isCall(relativePath, displayName)) {
                    hidden++
                    continue
                }

                val artist = cursor.getString(artistCol)
                out.add(
                    Track(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                            ?: displayName.substringBeforeLast('.', displayName),
                        artist = if (artist.isNullOrBlank() || artist == "<unknown>") "Noma'lum ijrochi" else artist,
                        album = cursor.getString(albumCol).orEmpty(),
                        albumId = cursor.getLong(albumIdCol),
                        dur = duration,
                        path = relativePath,
                        added = cursor.getLong(addedCol),
                        displayName = displayName,
                        mime = mime
                    )
                )
            }
        }
        return out
    }

    private fun normalize(path: String): String =
        path.trim().replace("\\", "/").trim('/').lowercase(Locale.ROOT)
}
