package uz.pleyer.premium

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

data class Track(val id: Long, val title: String, val artist: String, val album: String,
                 val albumId: Long, val dur: Long, val path: String, val added: Long) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val art: Uri get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

object Library {
    var hidden = 0

    // Qo'ng'iroq yozuvlari odatda shu nomli papkalarda saqlanadi
    private val badDirs = setOf("call", "calls", "call recordings", "call recording", "callrecordings",
        "callrecorder", "call_recordings", "call_recording", "recordings", "recording", "recorder",
        "voice recorder", "voicerecorder", "sound recorder", "soundrecorder", "sound_recorder",
        "voice notes", "voicenotes", "whatsapp voice notes", "dictaphone", "callrec", "phonerecord",
        "phone record", "phone records", "диктофон", "звонки", "записи", "запись звонков",
        "запись разговоров", "разговоры", "записи разговоров")

    // Fayl nomi bo'yicha: Call_..., +99890..., uzun raqamlar, sana_vaqt shablonlari
    private val badName = Regex("(^(call[ _-]?rec|call recording|callrecord|recording[ _-]?\\d|record[ _-]?\\d|rec[ _-]?\\d|new recording|ptt-|aud-\\d|voice[ _-]?\\d|vn[ _-]?\\d|звонок|запись))|\\+\\d{7,}|\\d{9,}|\\d{8}[_-]\\d{6}")

    // Ovozli qo'ng'iroq kodeklari (musiqa emas)
    private val badMime = listOf("amr", "3gpp", "3gp", "awb")

    fun isCall(path: String, mime: String): Boolean {
        val segs = path.lowercase().split('/')
        if (segs.dropLast(1).any { it in badDirs }) return true
        val name = segs.last().substringBeforeLast('.')
        if (badName.containsMatchIn(name)) return true
        val m = mime.lowercase()
        return badMime.any { m.contains(it) }
    }

    fun load(c: Context, minSec: Int): List<Track> {
        hidden = 0
        val out = ArrayList<Track>()
        val a = MediaStore.Audio.Media.IS_RINGTONE
        val sel = StringBuilder("$a=0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION}=0 AND ${MediaStore.Audio.Media.IS_ALARM}=0")
        if (Build.VERSION.SDK_INT >= 31) sel.append(" AND ${MediaStore.Audio.Media.IS_RECORDING}=0")
        val m = MediaStore.Audio.Media
        val cols = arrayOf(m._ID, m.TITLE, m.ARTIST, m.ALBUM, m.ALBUM_ID, m.DURATION, m.DATA, m.DATE_ADDED, m.MIME_TYPE)
        c.contentResolver.query(m.EXTERNAL_CONTENT_URI, cols, sel.toString(), null, "${m.TITLE} COLLATE NOCASE")?.use { q ->
            while (q.moveToNext()) {
                val path = q.getString(6) ?: ""
                val mime = q.getString(8) ?: ""
                val dur = q.getLong(5)
                if (dur < minSec * 1000L || isCall(path, mime)) { hidden++; continue }
                val ar = q.getString(2)
                out.add(Track(q.getLong(0), q.getString(1) ?: "?",
                    if (ar == null || ar == "<unknown>") "Noma'lum ijrochi" else ar,
                    q.getString(3) ?: "", q.getLong(4), dur, path, q.getLong(7)))
            }
        }
        return out
    }
}
