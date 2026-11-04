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

    // Hot 60 Pro+ / XOS uchun boshlang'ich istisno. Qo'shimcha papkalarni foydalanuvchi tanlaydi.
    private val defaultHiddenPaths = setOf(
        "music/phonerecord",
        "music/phone record",
        "music/phone records",
        "recordings/call",
        "recordings/call recordings"
    )

    // Faqat qo'ng'iroq yozuviga xos papka nomlari. Umumiy "recordings"/"recorder" papkasi bloklanmaydi.
    private val callFolderNames = setOf(
        "call", "calls", "call recording", "call recordings", "call_recording", "call_recordings",
        "callrecord", "callrecords", "callrecorder", "callrecording", "callrecordings",
        "phonerecord", "phone record", "phone records", "phone_record", "phone_recording",
        "call_rec", "call_recorder", "call recordings", "запись звонков", "записи звонков",
        "запись разговоров", "записи разговоров", "звонки"
    )

    // Fayl nomi bo'yicha faqat aniq qo'ng'iroq yozuvi belgilari taniladi.
    // Uzun raqamlar yoki AMR/3GP/AWB MIME turi yolg'iz o'zi faylni yashirmaydi.
    private val callName = Regex(
        "(^|[ ._-])(call[ ._-]?(record|recording|rec)|phone[ ._-]?(record|recording)|phonerecord)([ ._-]|$)",
        RegexOption.IGNORE_CASE
    )

    fun isCall(relativePath: String, displayName: String, customHidden: Set<String> = Store.hiddenPaths): Boolean {
        val normalized = normalize(relativePath)
        val hiddenPaths = defaultHiddenPaths + customHidden.map(::normalize)
        if (hiddenPaths.any { it.isNotBlank() && (normalized == it || normalized.startsWith("$it/")) }) return true

        val segments = normalized.split('/').filter { it.isNotBlank() }
        if (segments.any { it in callFolderNames }) return true

        val fileStem = displayName.substringBeforeLast('.', displayName).lowercase(Locale.ROOT)
        return callName.containsMatchIn(fileStem)
    }

    fun load(c: Context, minSec: Int): List<Track> {
        hidden = 0
        val out = ArrayList<Track>()
        val m = MediaStore.Audio.Media
        val selection = StringBuilder(
            "${m.IS_RINGTONE}=0 AND ${m.IS_NOTIFICATION}=0 AND ${m.IS_ALARM}=0"
        )
        if (Build.VERSION.SDK_INT >= 31) selection.append(" AND ${m.IS_RECORDING}=0")

        val columns = mutableListOf(
            m._ID, m.TITLE, m.ARTIST, m.ALBUM, m.ALBUM_ID,
            m.DURATION, m.DATE_ADDED, m.MIME_TYPE, m.DISPLAY_NAME
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns.add(MediaStore.MediaColumns.RELATIVE_PATH)
        }

        c.contentResolver.query(
            m.EXTERNAL_CONTENT_URI,
            columns.toTypedArray(),
            selection.toString(),
            null,
            "${m.TITLE} COLLATE NOCASE ASC"
        )?.use { q ->
            val idCol = q.getColumnIndexOrThrow(m._ID)
            val titleCol = q.getColumnIndexOrThrow(m.TITLE)
            val artistCol = q.getColumnIndexOrThrow(m.ARTIST)
            val albumCol = q.getColumnIndexOrThrow(m.ALBUM)
            val albumIdCol = q.getColumnIndexOrThrow(m.ALBUM_ID)
            val durationCol = q.getColumnIndexOrThrow(m.DURATION)
            val addedCol = q.getColumnIndexOrThrow(m.DATE_ADDED)
            val mimeCol = q.getColumnIndex(m.MIME_TYPE)
            val nameCol = q.getColumnIndex(m.DISPLAY_NAME)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                q.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else -1

            while (q.moveToNext()) {
                val path = if (pathCol >= 0) q.getString(pathCol).orEmpty() else ""
                val name = if (nameCol >= 0) q.getString(nameCol).orEmpty() else ""
                val mime = if (mimeCol >= 0) q.getString(mimeCol).orEmpty() else ""
                val duration = q.getLong(durationCol)

                if (duration < minSec * 1000L || isCall(path, name)) {
                    hidden++
                    continue
                }

                val artist = q.getString(artistCol)
                out.add(
                    Track(
                        id = q.getLong(idCol),
                        title = q.getString(titleCol)?.takeIf { it.isNotBlank() } ?: name.substringBeforeLast('.', name),
                        artist = if (artist.isNullOrBlank() || artist == "<unknown>") "Noma'lum ijrochi" else artist,
                        album = q.getString(albumCol).orEmpty(),
                        albumId = q.getLong(albumIdCol),
                        dur = duration,
                        path = path,
                        added = q.getLong(addedCol),
                        displayName = name,
                        mime = mime
                    )
                )
            }
        }
        return out
    }
}
