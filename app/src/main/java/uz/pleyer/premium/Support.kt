package uz.pleyer.premium

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

object Store {
    lateinit var sp: SharedPreferences
    fun init(c: Context) { if (!::sp.isInitialized) sp = c.applicationContext.getSharedPreferences("pleyer", 0) }
    var favs: Set<Long>
        get() = sp.getStringSet("favs", emptySet())!!.map { it.toLong() }.toSet()
        set(v) { sp.edit().putStringSet("favs", v.map { it.toString() }.toSet()).apply() }
    var minSec: Int
        get() = sp.getInt("min", 30)
        set(v) { sp.edit().putInt("min", v).apply() }
    fun playlists(): Map<String, List<Long>> {
        val o = JSONObject(sp.getString("pl", "{}")!!)
        return o.keys().asSequence().associateWith { k ->
            val a = o.getJSONArray(k); (0 until a.length()).map { a.getLong(it) }
        }
    }
    fun savePl(m: Map<String, List<Long>>) {
        val o = JSONObject(); m.forEach { (k, v) -> o.put(k, JSONArray(v)) }
        sp.edit().putString("pl", o.toString()).apply()
    }
    fun lyric(id: Long): String? = sp.getString("ly$id", null)?.takeIf { it.isNotBlank() }
    fun setLyric(id: Long, t: String) { sp.edit().putString("ly$id", t).apply() }
}

object Fx {
    var eq: Equalizer? = null
    var bb: BassBoost? = null
    var ver by mutableIntStateOf(0)
    var on by mutableStateOf(false)
    var bass by mutableIntStateOf(0)
    val levels = mutableStateListOf<Int>()

    fun attach(sid: Int) {
        release()
        if (sid <= 0) return
        try { eq = Equalizer(0, sid); bb = BassBoost(0, sid) } catch (e: Exception) { return }
        val n = eq!!.numberOfBands.toInt()
        val saved = Store.sp.getString("lv", "")!!.split(",").mapNotNull { it.toIntOrNull() }
        levels.clear(); repeat(n) { levels.add(saved.getOrElse(it) { 0 }) }
        on = Store.sp.getBoolean("eqon", false); bass = Store.sp.getInt("bass", 0)
        apply(); ver++
    }
    fun apply() {
        try {
            eq?.let { e ->
                e.enabled = on
                levels.forEachIndexed { i, l -> if (i < e.numberOfBands) e.setBandLevel(i.toShort(), l.toShort()) }
            }
            bb?.let { b -> b.enabled = bass > 0; if (bass > 0) b.setStrength(bass.toShort()) }
        } catch (e: Exception) { }
        Store.sp.edit().putString("lv", levels.joinToString(",")).putBoolean("eqon", on).putInt("bass", bass).apply()
    }
    fun release() { try { eq?.release(); bb?.release() } catch (e: Exception) { }; eq = null; bb = null }
}

object Sleep {
    private val h = Handler(Looper.getMainLooper())
    var endAt by mutableLongStateOf(0L)
    private val r = Runnable { PlayerService.player?.pause(); endAt = 0L }
    fun start(min: Int) { cancel(); endAt = System.currentTimeMillis() + min * 60000L; h.postDelayed(r, min * 60000L) }
    fun cancel() { h.removeCallbacks(r); endAt = 0L }
}

object Lyrics {
    fun load(t: Track): List<Pair<Long, String>> {
        val raw = Store.lyric(t.id) ?: sidecar(t.path) ?: return emptyList()
        return parse(raw)
    }
    private fun sidecar(p: String): String? {
        for (e in listOf("lrc", "txt")) {
            try { val f = java.io.File(p.substringBeforeLast('.') + "." + e); if (f.canRead()) return f.readText() } catch (_: Exception) { }
        }
        return null
    }
    fun parse(raw: String): List<Pair<Long, String>> {
        val r = Regex("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]")
        val out = ArrayList<Pair<Long, String>>(); var timed = false
        for (line in raw.lines()) {
            val ms = r.findAll(line).toList(); val txt = line.replace(r, "").trim()
            if (ms.isNotEmpty()) {
                timed = true
                ms.forEach { out.add(((it.groupValues[1].toLong() * 60 + it.groupValues[2].toDouble()) * 1000).toLong() to txt) }
            } else if (!(line.startsWith("[") && line.endsWith("]") && line.contains(":"))) out.add(-1L to line)
        }
        return if (timed) out.filter { it.first >= 0 }.sortedBy { it.first } else out
    }
}
