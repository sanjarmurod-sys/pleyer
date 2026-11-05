@file:OptIn(ExperimentalMaterial3Api::class)

package uz.pleyer.premium

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.*

var Ink by mutableStateOf(Color(0xFF0B0B0F))
var Ink2 by mutableStateOf(Color(0xFF16161D))
var Gold by mutableStateOf(Color(0xFFD8B878))
var Cream by mutableStateOf(Color(0xFFF1EBDD))
var Mute by mutableStateOf(Color(0xFF8E8A82))
const val FAV = "Sevimlilar"

fun fmt(ms: Long): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

@Composable
fun Art(t: Track?, size: Dp, px: Int, round: Dp = 10.dp) {
    val c = LocalContext.current
    val bmp by produceState<Bitmap?>(null, t?.id) {
        value = t?.let {
            withContext(Dispatchers.IO) { try { c.contentResolver.loadThumbnail(it.uri, Size(px, px), null) } catch (e: Exception) { null } }
        }
    }
    val b = bmp
    Box(Modifier.size(size).clip(RoundedCornerShape(round)).background(Ink2), contentAlignment = Alignment.Center) {
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Rounded.MusicNote, null, tint = Gold.copy(alpha = 0.5f))
    }
}

@Composable
fun AudioVisualizer(track: Track, playing: Boolean) {
    val bars by AudioViz.levels.collectAsState()
    val aqua = Store.theme == "aqua"
    val accent2 = if (aqua) Color(0xFF36D9E8) else Color(0xFF9B5CFF)
    val accent3 = if (aqua) Color(0xFF8AFFF1) else Color(0xFFFF4DB8)
    val pulse = rememberInfiniteTransition(label = "playerPulse")
    val breathe by pulse.animateFloat(0.94f, 1.035f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val rotation by pulse.animateFloat(0f, 360f,
        infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart), label = "orbit")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                val min = size.minDimension
                val base = min * 0.315f
                drawCircle(Brush.radialGradient(listOf(accent2.copy(alpha = 0.18f), Color.Transparent), c, min * 0.49f), min * 0.49f, c)
                drawCircle(accent2.copy(alpha = 0.22f), base + 24.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                drawCircle(accent3.copy(alpha = 0.28f), base + 35.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                drawArc(Brush.sweepGradient(listOf(Color.Transparent, accent2, accent3, Color.Transparent)),
                    rotation, 112f, false,
                    androidx.compose.ui.geometry.Offset(c.x - base - 29.dp.toPx(), c.y - base - 29.dp.toPx()),
                    androidx.compose.ui.geometry.Size((base + 29.dp.toPx()) * 2, (base + 29.dp.toPx()) * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                val count = bars.size.coerceAtLeast(1)
                bars.forEachIndexed { i, raw ->
                    val a = (i * 360f / count - 90f) * Math.PI.toFloat() / 180f
                    val level = if (playing) raw.coerceIn(0.035f, 1f) else 0.035f
                    val inner = base + 10.dp.toPx()
                    val outer = inner + 7.dp.toPx() + level * 35.dp.toPx()
                    val p1 = androidx.compose.ui.geometry.Offset(c.x + kotlin.math.cos(a) * inner, c.y + kotlin.math.sin(a) * inner)
                    val p2 = androidx.compose.ui.geometry.Offset(c.x + kotlin.math.cos(a) * outer, c.y + kotlin.math.sin(a) * outer)
                    drawLine(Brush.linearGradient(listOf(accent2, accent3)), p1, p2,
                        if (i % 3 == 0) 3.2.dp.toPx() else 2.2.dp.toPx(), cap = StrokeCap.Round)
                }
                drawCircle(accent2.copy(alpha = 0.16f), base - 2.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
            Box(Modifier.size(210.dp).graphicsLayer {
                scaleX = if (playing) breathe else 1f; scaleY = if (playing) breathe else 1f
                shadowElevation = 26.dp.toPx(); shape = CircleShape; clip = true
            }.background(Ink2, CircleShape).padding(5.dp)) {
                Art(track, 200.dp, 640, 100.dp)
            }
            Box(Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 14.dp)
                .clip(CircleShape).background(Ink2.copy(alpha = 0.88f))
                .border(1.dp, accent2.copy(alpha = 0.45f), CircleShape).padding(horizontal = 11.dp, vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).background(if (playing) accent3 else Mute, CircleShape))
                    Text(if (playing) "LIVE AUDIO" else "PAUSED", color = Cream, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            bars.forEach { value ->
                Box(Modifier.padding(horizontal = 1.5.dp).width(3.dp)
                    .height((if (playing) 5f + value.coerceIn(0f, 1f) * 35f else 5f).dp)
                    .clip(CircleShape).background(Brush.verticalGradient(listOf(accent3, accent2.copy(alpha = 0.75f)))))
            }
        }
        Text(if (playing) "SOUND • IN MOTION" else "PLAY TO ACTIVATE THE VISUALIZER",
            color = Mute, fontSize = 9.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

class MainActivity : ComponentActivity() {
    private var ctl by mutableStateOf<MediaController?>(null)
    private var tracks by mutableStateOf<List<Track>>(emptyList())
    private var ready by mutableStateOf(false)
    private var curId by mutableStateOf<Long?>(null)
    private var playing by mutableStateOf(false)
    private var shuffle by mutableStateOf(false)
    private var rep by mutableIntStateOf(0)
    private var favs by mutableStateOf<Set<Long>>(emptySet())
    private var pls by mutableStateOf<Map<String, List<Long>>>(emptyMap())
    private var selectedTheme by mutableStateOf("neon")
    private var appearance by mutableStateOf("system")
    private lateinit var rq: ActivityResultLauncher<Array<String>>
    private lateinit var perm: Array<String>

    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Store.addCustomUri(uri.toString())
            scan()
            Toast.makeText(this, "Audio fayl kutubxonaga qo'shildi", Toast.LENGTH_SHORT).show()
        }
    }

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val path = runCatching {
                DocumentsContract.getTreeDocumentId(uri).substringAfter(':', "").trim('/')
            }.getOrDefault("")
            if (path.isNotBlank()) {
                Store.addHiddenPath(path)
                scan()
                Toast.makeText(this, "Papka musiqalar ro'yxatidan chiqarildi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Papka yo'lini aniqlab bo'lmadi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Store.init(this); favs = Store.favs; pls = Store.playlists(); selectedTheme = Store.theme; appearance = Store.appearance
        perm = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.RECORD_AUDIO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
        rq = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            scan()
            if (result[Manifest.permission.RECORD_AUDIO] == true) PlayerService.refreshVisualizer()
        }
        scan()
        val need = perm.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) rq.launch(need.toTypedArray())
        connect()
        setContent { App() }
    }

    override fun onResume() { super.onResume(); if (::perm.isInitialized) scan() }

    private fun scan() {
        if (checkSelfPermission(perm[0]) != PackageManager.PERMISSION_GRANTED) return
        ready = true
        Thread {
            val indexed = Library.load(this, Store.minSec)
            val picked = Store.customUris.mapNotNull { raw ->
                runCatching { Library.fromDocument(this, Uri.parse(raw)) }.getOrNull()
            }
            val merged = (indexed + picked).distinctBy { it.uri.toString() }
            runOnUiThread { tracks = merged }
        }.start()
    }

    private fun sync(c: Player) { curId = c.currentMediaItem?.mediaId?.toLongOrNull(); playing = c.isPlaying; shuffle = c.shuffleModeEnabled; rep = c.repeatMode }

    private fun connect() {
        val f = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlayerService::class.java))).buildAsync()
        f.addListener({
            try {
                val c = f.get(); ctl = c; sync(c)
                c.addListener(object : Player.Listener { override fun onEvents(p: Player, e: Player.Events) { sync(c) } })
            } catch (e: Exception) { }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun item(t: Track) = MediaItem.Builder().setMediaId(t.id.toString()).setUri(t.uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).setArtworkUri(t.art).build()).build()

    private fun play(l: List<Track>, i: Int) { ctl?.run { setMediaItems(l.map { item(it) }, i, 0L); prepare(); play() } }

    @Composable
    fun App() {
        val systemDark = isSystemInDarkTheme()
        val dark = when (appearance) { "light" -> false; "dark" -> true; else -> systemDark }
        SideEffect {
            when (selectedTheme) {
                "aqua" -> {
                    Ink = if (dark) Color(0xFF06171D) else Color(0xFFF0FBFC)
                    Ink2 = if (dark) Color(0xFF0D2932) else Color(0xFFDDF4F5)
                    Gold = if (dark) Color(0xFF42E6E0) else Color(0xFF087F89)
                    Cream = if (dark) Color(0xFFE4FAFA) else Color(0xFF12343A)
                    Mute = if (dark) Color(0xFF83AEB2) else Color(0xFF5D858A)
                }
                else -> {
                    Ink = if (dark) Color(0xFF100B1C) else Color(0xFFF8F3FF)
                    Ink2 = if (dark) Color(0xFF211632) else Color(0xFFEDE2FA)
                    Gold = if (dark) Color(0xFFFF5BC8) else Color(0xFF8C39B8)
                    Cream = if (dark) Color(0xFFF8EFFF) else Color(0xFF281638)
                    Mute = if (dark) Color(0xFFB69CC7) else Color(0xFF806B91)
                }
            }
        }
        MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Gold, onPrimary = Ink, secondary = Gold, background = Ink, surface = Ink2,
            onBackground = Cream, onSurface = Cream, surfaceVariant = Ink2, onSurfaceVariant = Mute)
            else lightColorScheme(primary = Gold, onPrimary = Color.White, secondary = Gold, background = Ink, surface = Ink2,
            onBackground = Cream, onSurface = Cream, surfaceVariant = Ink2, onSurfaceVariant = Mute)) {
            var tab by remember { mutableIntStateOf(0) }
            var full by remember { mutableStateOf(false) }
            val cur = tracks.find { it.id == curId }
            Box(Modifier.fillMaxSize().background(Ink)) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when {
                            !ready -> Perm()
                            tab == 0 -> Songs()
                            tab == 1 -> Groups()
                            tab == 2 -> Lists()
                            else -> Tools()
                        }
                    }
                    if (cur != null) Mini(cur) { full = true }
                    NavigationBar(containerColor = Ink2) {
                        listOf("Qo'shiqlar" to Icons.Rounded.MusicNote, "Albom" to Icons.Rounded.Album,
                            "Pleylist" to Icons.Rounded.QueueMusic, "Sozlamalar" to Icons.Rounded.Tune).forEachIndexed { i, p ->
                            NavigationBarItem(tab == i, { tab = i }, { Icon(p.second, null) }, label = { Text(p.first, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Ink, selectedTextColor = Gold,
                                    indicatorColor = Gold, unselectedIconColor = Mute, unselectedTextColor = Mute))
                        }
                    }
                }
                if (full && cur != null) Full(cur) { full = false }
            }
        }
    }

    @Composable
    fun Perm() {
        Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("Premium Pleyer", fontFamily = FontFamily.Serif, fontSize = 30.sp, color = Gold)
            Spacer(Modifier.height(12.dp))
            Text("Musiqalaringizni ko'rsatish uchun audio fayllarga ruxsat kerak.", color = Cream, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button({ rq.launch(perm) }) { Text("Ruxsat berish") }
        }
    }

    @Composable
    fun Header(t: String, s: String, back: (() -> Unit)? = null) {
        Row(Modifier.fillMaxWidth().padding(16.dp, 14.dp, 16.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (back != null) IconButton(back) { Icon(Icons.Rounded.ArrowBack, null, tint = Cream) }
            Column {
                Text(t, fontFamily = FontFamily.Serif, fontSize = 28.sp, color = Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s, color = Mute, fontSize = 13.sp)
            }
        }
    }

    @Composable
    fun PlayBar(l: List<Track>) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ if (l.isNotEmpty()) { ctl?.shuffleModeEnabled = false; play(l, 0) } }) { Text("Ijro etish") }
            OutlinedButton({ if (l.isNotEmpty()) { ctl?.shuffleModeEnabled = true; play(l, l.indices.random()) } }) { Text("Aralash", color = Gold) }
        }
    }

    @Composable
    fun TrackList(l: List<Track>, pl: String?) {
        LazyColumn(Modifier.fillMaxSize()) { itemsIndexed(l) { i, t -> TrackRow(t, l, i, pl) } }
    }

    @Composable
    fun TrackRow(t: Track, l: List<Track>, i: Int, pl: String?) {
        var m by remember { mutableStateOf(false) }
        var sub by remember { mutableStateOf(false) }
        var edit by remember { mutableStateOf(false) }
        val on = t.id == curId
        val fav = t.id in favs
        Row(Modifier.fillMaxWidth().clickable { play(l, i) }.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Art(t, 48.dp, 128)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, color = if (on) Gold else Cream, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(t.artist + " • " + fmt(t.dur), color = Mute, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton({ favs = if (fav) favs - t.id else favs + t.id; Store.favs = favs }) {
                Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (fav) Gold else Mute)
            }
            Box {
                IconButton({ m = true }) { Icon(Icons.Rounded.MoreVert, null, tint = Mute) }
                DropdownMenu(m, { m = false; sub = false }) {
                    if (!sub) {
                        DropdownMenuItem({ Text("Keyin ijro etish") }, { ctl?.let { it.addMediaItem(it.currentMediaItemIndex + 1, item(t)) }; m = false })
                        DropdownMenuItem({ Text("Pleylistga qo'shish") }, { sub = true })
                        DropdownMenuItem({ Text("Matnni tahrirlash") }, { edit = true; m = false })
                        if (t.isFromPicker) DropdownMenuItem({ Text("Kutubxonadan olib tashlash") }, {
                            Store.removeCustomUri(t.uri.toString()); scan(); m = false
                        })
                        if (pl != null && pl != FAV) DropdownMenuItem({ Text("Pleylistdan olib tashlash") }, {
                            pls = pls + (pl to pls[pl].orEmpty().filter { it != t.id }); Store.savePl(pls); m = false
                        })
                    } else {
                        pls.keys.forEach { n ->
                            DropdownMenuItem({ Text(n) }, { pls = pls + (n to (pls[n].orEmpty() + t.id)); Store.savePl(pls); m = false; sub = false })
                        }
                        if (pls.isEmpty()) DropdownMenuItem({ Text("Avval pleylist yarating") }, { m = false; sub = false })
                    }
                }
            }
        }
        if (edit) LyricDialog(t) { edit = false }
    }

    @Composable
    fun Songs() {
        var q by remember { mutableStateOf("") }
        var sort by remember { mutableIntStateOf(0) }
        val l = remember(tracks, q, sort) {
            tracks.filter { q.isBlank() || (it.title + it.artist + it.album).contains(q, true) }.let {
                when (sort) {
                    1 -> it.sortedBy { x -> x.title.lowercase() }
                    2 -> it.sortedBy { x -> x.artist.lowercase() }
                    3 -> it.sortedByDescending { x -> x.dur }
                    else -> it
                }
            }
        }
        Column {
            Header("Mediateka", "${l.size} ta trek")
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Qidirish") },
                singleLine = true, leadingIcon = { Icon(Icons.Rounded.Search, null) })
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Yangi", "A–Z", "Ijrochi", "Davomiylik").forEachIndexed { i, s -> FilterChip(sort == i, { sort = i }, { Text(s) }) }
            }
            PlayBar(l)
            TrackList(l, null)
        }
    }

    @Composable
    fun Groups() {
        var byAlbum by remember { mutableStateOf(true) }
        var sel by remember { mutableStateOf<String?>(null) }
        val g = remember(tracks, byAlbum) {
            tracks.groupBy { if (byAlbum) it.album.ifBlank { "Noma'lum albom" } else it.artist }.toList().sortedBy { it.first.lowercase() }
        }
        BackHandler(sel != null) { sel = null }
        if (sel != null) {
            val n = sel!!
            val l = g.firstOrNull { it.first == n }?.second.orEmpty()
            Column { Header(n, "${l.size} ta trek") { sel = null }; PlayBar(l); TrackList(l, null) }
        } else Column {
            Header(if (byAlbum) "Albomlar" else "Ijrochilar", "${g.size} ta")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(byAlbum, { byAlbum = true }, { Text("Albomlar") })
                FilterChip(!byAlbum, { byAlbum = false }, { Text("Ijrochilar") })
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(g) { p ->
                    Row(Modifier.fillMaxWidth().clickable { sel = p.first }.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Art(p.second[0], 52.dp, 128)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(p.first, color = Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${p.second.size} ta trek", color = Mute, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Lists() {
        var sel by remember { mutableStateOf<String?>(null) }
        var dlg by remember { mutableStateOf(false) }
        val byId = remember(tracks) { tracks.associateBy { it.id } }
        fun of(n: String): List<Track> = if (n == FAV) tracks.filter { it.id in favs } else pls[n].orEmpty().mapNotNull { byId[it] }
        BackHandler(sel != null) { sel = null }
        if (sel != null) {
            val n = sel!!
            val l = of(n)
            Column {
                Header(n, "${l.size} ta trek") { sel = null }
                PlayBar(l)
                if (n != FAV) TextButton({ pls = pls - n; Store.savePl(pls); sel = null }) { Text("Pleylistni o'chirish", color = Mute) }
                TrackList(l, n)
            }
        } else Column {
            Header("Pleylistlar", "${pls.size + 1} ta")
            Button({ dlg = true }, Modifier.padding(horizontal = 16.dp)) { Icon(Icons.Rounded.Add, null); Text(" Yangi pleylist") }
            LazyColumn(Modifier.fillMaxSize()) {
                items(listOf(FAV) + pls.keys.toList()) { n ->
                    Row(Modifier.fillMaxWidth().clickable { sel = n }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (n == FAV) Icons.Rounded.Favorite else Icons.Rounded.QueueMusic, null, tint = Gold)
                        Spacer(Modifier.width(14.dp))
                        Column { Text(n, color = Cream); Text("${of(n).size} ta trek", color = Mute, fontSize = 12.sp) }
                    }
                }
            }
        }
        if (dlg) {
            var s by remember { mutableStateOf("") }
            AlertDialog({ dlg = false },
                confirmButton = { TextButton({ val n = s.trim(); if (n.isNotEmpty() && n != FAV && n !in pls) { pls = pls + (n to emptyList()); Store.savePl(pls) }; dlg = false }) { Text("Yaratish") } },
                dismissButton = { TextButton({ dlg = false }) { Text("Bekor") } },
                title = { Text("Yangi pleylist") },
                text = { OutlinedTextField(s, { s = it }, singleLine = true, placeholder = { Text("Nomi") }) })
        }
    }

    @Composable
    fun Sec(t: String) { Text(t, color = Gold, fontFamily = FontFamily.Serif, fontSize = 20.sp, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)) }

    @Composable
    fun Tools() {
        var sp by remember { mutableFloatStateOf(1f) }
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
        val v = Fx.ver
        val e = Fx.eq
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Header("Sozlamalar", "Neon Pulse • Aqua")
            Sec("Interfeys mavzusi")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selectedTheme == "neon", { selectedTheme = "neon"; Store.theme = "neon" }, { Text("Neon Pulse") })
                FilterChip(selectedTheme == "aqua", { selectedTheme = "aqua"; Store.theme = "aqua" }, { Text("Aqua") })
            }
            Sec("Ko'rinish rejimi")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("system" to "Avtomatik", "light" to "Kunduzgi", "dark" to "Tungi").forEach { (key, label) ->
                    FilterChip(appearance == key, { appearance = key; Store.appearance = key }, { Text(label) })
                }
            }
            Text("Avtomatik rejim telefoningizning kunduzgi/tungi sozlamasiga moslashadi.", color = Mute, fontSize = 12.sp)
            Sec("Mediateka")
            Text("Mediatekada ${tracks.size} ta trek • yashirilgan: ${Library.hidden}", color = Mute, fontSize = 12.sp)
            Sec("Ijro tezligi: " + "%.2f".format(sp) + "x")
            Slider(sp, { sp = it; ctl?.setPlaybackSpeed(it) }, valueRange = 0.5f..2f)

            Sec("Ekvalayzer")
            if (e == null || v < 0) Text("Ekvalayzer uchun avval biror qo'shiqni ijro eting.", color = Mute)
            else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Yoqish", color = Cream, modifier = Modifier.weight(1f))
                    Switch(Fx.on, { Fx.on = it; Fx.apply() })
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in 0 until e.numberOfPresets) AssistChip({
                        e.usePreset(p.toShort())
                        for (b in 0 until e.numberOfBands) Fx.levels[b] = e.getBandLevel(b.toShort()).toInt()
                        Fx.on = true; Fx.apply()
                    }, { Text(e.getPresetName(p.toShort())) })
                }
                val r = e.bandLevelRange
                for (b in 0 until e.numberOfBands) {
                    val hz = e.getCenterFreq(b.toShort()) / 1000
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (hz >= 1000) "%.1f kHz".format(hz / 1000f) else "$hz Hz", color = Mute, fontSize = 12.sp, modifier = Modifier.width(64.dp))
                        Slider(Fx.levels.getOrElse(b) { 0 }.toFloat(), { Fx.levels[b] = it.toInt(); Fx.apply() }, valueRange = r[0].toFloat()..r[1].toFloat())
                    }
                }
            }

            Sec("Bass kuchaytirgich")
            Slider(Fx.bass / 1000f, { Fx.bass = (it * 1000).toInt(); Fx.apply() })

            Sec("Uyqu taymeri")
            if (Sleep.endAt > 0) Text("Qoldi: " + fmt(maxOf(0L, Sleep.endAt - now)), color = Gold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60, 90).forEach { m -> AssistChip({ Sleep.start(m) }, { Text("$m daq") }) }
                AssistChip({ Sleep.cancel() }, { Text("O'chirish") })
            }

            Sec("Qisqa fayllarni yashirish")
            Text("Shu davomiylikdan qisqa fayllar (rington, ovozli xabar) ro'yxatda ko'rinmaydi. Qo'ng'iroq yozuvlari esa papka va nom bo'yicha avtomatik chiqarib tashlanadi.", color = Mute, fontSize = 12.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 15, 30, 60).forEach { s ->
                    FilterChip(Store.minSec == s, { Store.minSec = s; scan() }, { Text(if (s == 0) "Yo'q" else "$s soniya") })
                }
            }

            Sec("Tashqi audio fayllar")
            Text("Mediatekada ko'rinmagan MP3, FLAC, WAV, M4A yoki boshqa audio faylni tizim fayl tanlagichi orqali qo'shing.", color = Mute, fontSize = 12.sp)
            Button(
                onClick = { audioPicker.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.AudioFile, null)
                Spacer(Modifier.width(8.dp))
                Text("Audio fayl tanlash")
            }

            Sec("Qo'ng'iroq yozuvlari va istisno papkalar")
            Text("Infinix uchun Music/PhoneRecord boshlang'ich holatda yashiriladi. Qo'ng'iroq yozuvlari qaysi audio formatda bo'lishidan qat'i nazar, papka yoki aniq yozuv nomi orqali ajratiladi. Istalgan boshqa papkani ham tanlashingiz mumkin.", color = Mute, fontSize = 12.sp)
            Button(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.FolderOff, null)
                Spacer(Modifier.width(8.dp))
                Text("Istisno papka tanlash")
            }
            Store.hiddenPaths.sorted().forEach { folder ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(folder, color = Cream, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { Store.removeHiddenPath(folder); scan() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Istisnoni olib tashlash", tint = Mute)
                    }
                }
            }

            Sec("Qo'shiq matni")
            Text("Qo'shiq menyusidagi «Matnni tahrirlash» orqali matnni qo'lda kiritishingiz mumkin. Vaqt belgilari bilan yozilgan LRC matni ijro davomida sinxron ko'rsatiladi.", color = Mute, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
        }
    }

    @Composable
    fun Mini(t: Track, open: () -> Unit) {
        Row(Modifier.fillMaxWidth().background(Ink2).clickable { open() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Art(t, 44.dp, 128)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, color = Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = Mute, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton({ ctl?.seekToPrevious() }) { Icon(Icons.Rounded.SkipPrevious, null, tint = Cream) }
            IconButton({ if (playing) ctl?.pause() else ctl?.play() }) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Gold, modifier = Modifier.size(32.dp)) }
            IconButton({ ctl?.seekToNext() }) { Icon(Icons.Rounded.SkipNext, null, tint = Cream) }
        }
    }

    @Composable
    fun Full(t: Track, close: () -> Unit) {
        BackHandler { close() }
        var pos by remember { mutableLongStateOf(0L) }
        var ly by remember { mutableStateOf(false) }
        LaunchedEffect(t.id) { while (true) { pos = ctl?.currentPosition ?: 0L; delay(250) } }
        val lines = remember(t.id, ly) { Lyrics.load(t) }
        val d = (ctl?.duration ?: 0L).coerceAtLeast(1L)
        val fav = t.id in favs
        val aqua = Store.theme == "aqua"
        val accent = if (aqua) Color(0xFF42E6E0) else Color(0xFFFF5BC8)
        val accent2 = if (aqua) Color(0xFF168DA5) else Color(0xFF8750FF)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            if (aqua) Color(0xFF06232D) else Color(0xFF170D2C), Ink,
            if (aqua) Color(0xFF031218) else Color(0xFF0B0714))))) {
            Canvas(Modifier.fillMaxSize()) {
                val c = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.34f)
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.13f), Color.Transparent), c, size.minDimension * 0.7f),
                    size.minDimension * 0.7f, c)
            }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(close) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Cream, modifier = Modifier.size(32.dp)) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NOW PLAYING", color = Mute, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
                        Text(if (aqua) "A Q U A" else "N E O N   P U L S E", color = accent, fontSize = 10.sp, letterSpacing = 1.4.sp)
                    }
                    IconButton({ ly = !ly }) { Icon(if (ly) Icons.Rounded.Album else Icons.Rounded.MusicNote, null, tint = if (ly) accent else Cream) }
                }
                if (!ly) {
                    Spacer(Modifier.weight(0.12f))
                    AudioVisualizer(t, playing)
                    Spacer(Modifier.weight(0.12f))
                } else LyricsView(lines, pos, Modifier.weight(1f).fillMaxWidth())
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t.title, fontFamily = FontFamily.Serif, fontSize = 25.sp, color = Cream, maxLines = 2,
                        textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text(t.artist, color = accent, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(18.dp))
                    Slider(pos.toFloat().coerceIn(0f, d.toFloat()), { pos = it.toLong(); ctl?.seekTo(it.toLong()) },
                        valueRange = 0f..d.toFloat(), colors = SliderDefaults.colors(thumbColor = accent,
                            activeTrackColor = accent, inactiveTrackColor = Cream.copy(alpha = 0.16f)))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fmt(pos), color = Mute, fontSize = 11.sp); Text(fmt(d), color = Mute, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ ctl?.shuffleModeEnabled = !shuffle }) { Icon(Icons.Rounded.Shuffle, null, tint = if (shuffle) accent else Mute, modifier = Modifier.size(23.dp)) }
                        IconButton({ ctl?.seekToPrevious() }) { Icon(Icons.Rounded.SkipPrevious, null, tint = Cream, modifier = Modifier.size(36.dp)) }
                        Box(Modifier.size(76.dp).background(Brush.linearGradient(listOf(accent, accent2)), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                            .clickable { if (playing) ctl?.pause() else ctl?.play() }, contentAlignment = Alignment.Center) {
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(42.dp))
                        }
                        IconButton({ ctl?.seekToNext() }) { Icon(Icons.Rounded.SkipNext, null, tint = Cream, modifier = Modifier.size(36.dp)) }
                        IconButton({ ctl?.repeatMode = when (rep) { 0 -> 2; 2 -> 1; else -> 0 } }) {
                            Icon(if (rep == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if (rep != 0) accent else Mute, modifier = Modifier.size(23.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ favs = if (fav) favs - t.id else favs + t.id; Store.favs = favs }) {
                            Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (fav) accent else Mute)
                        }
                        Text(if (fav) "SAVED TO FAVOURITES" else "ADD TO FAVOURITES", color = Mute, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                }
            }
        }
    }

    @Composable
    fun LyricsView(l: List<Pair<Long, String>>, pos: Long, m: Modifier) {
        if (l.isEmpty()) {
            Box(m, Alignment.Center) {
                Text("Matn topilmadi.\nTrek menyusidagi «Matnni tahrirlash» orqali qo'shing yoki .lrc fayl qo'ying.", color = Mute, textAlign = TextAlign.Center)
            }
            return
        }
        val st = rememberLazyListState()
        val idx = if (l[0].first >= 0) l.indexOfLast { it.first <= pos }.coerceAtLeast(0) else -1
        LaunchedEffect(idx) { if (idx >= 0) st.animateScrollToItem(maxOf(0, idx - 3)) }
        LazyColumn(m, st, horizontalAlignment = Alignment.CenterHorizontally) {
            itemsIndexed(l) { i, p ->
                Text(p.second, color = if (i == idx) Gold else Cream.copy(alpha = 0.55f), fontSize = if (i == idx) 22.sp else 18.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    @Composable
    fun LyricDialog(t: Track, done: () -> Unit) {
        var s by remember { mutableStateOf(Store.lyric(t.id) ?: "") }
        AlertDialog(done,
            confirmButton = { TextButton({ Store.setLyric(t.id, s); done() }) { Text("Saqlash") } },
            dismissButton = { TextButton(done) { Text("Bekor") } },
            title = { Text(t.title, maxLines = 1) },
            text = { OutlinedTextField(s, { s = it }, Modifier.fillMaxWidth().height(260.dp), placeholder = { Text("Matnni shu yerga qo'ying. Vaqtli matn ham mumkin: [00:12.50] satr") }) })
    }
}
