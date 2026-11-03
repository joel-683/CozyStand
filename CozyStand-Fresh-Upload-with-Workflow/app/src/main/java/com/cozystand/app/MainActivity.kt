package com.cozystand.app

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class CozyTheme(val name: String, val top: Color, val bottom: Color, val accent: Color, val text: Color)
private val themes = listOf(
    CozyTheme("Oat milk", Color(0xFFDED1B9), Color(0xFF8C7965), Color(0xFF594B3E), Color(0xFF29241F)),
    CozyTheme("Walnut", Color(0xFF46362D), Color(0xFF1E1917), Color(0xFFD6B58C), Color(0xFFF4E8D8)),
    CozyTheme("Moss", Color(0xFF354638), Color(0xFF17241E), Color(0xFFB5C19A), Color(0xFFF0EBDD)),
    CozyTheme("Dusk", Color(0xFF4C4B65), Color(0xFF202333), Color(0xFFD9B8A8), Color(0xFFF3E8E1)),
    CozyTheme("Terracotta", Color(0xFFB96F50), Color(0xFF542F2A), Color(0xFFFFD5B8), Color(0xFFFFF0E5)),
    CozyTheme("Midnight", Color(0xFF252A35), Color(0xFF101319), Color(0xFFB9C8D9), Color(0xFFEAF0F6))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        setContent { CozyStandApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CozyStandApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cozystand", Context.MODE_PRIVATE) }
    var themeIndex by remember { mutableIntStateOf(prefs.getInt("theme", 0).coerceIn(themes.indices)) }
    var clockStyle by remember { mutableStateOf(prefs.getString("clock", "Digital") ?: "Digital") }
    var use24h by remember { mutableStateOf(prefs.getBoolean("24h", false)) }
    var showSeconds by remember { mutableStateOf(prefs.getBoolean("seconds", false)) }
    var mediaSeparate by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var wallpaperUri by remember { mutableStateOf(prefs.getString("wallpaper", null)?.let(Uri::parse)) }
    var wallpaper by remember { mutableStateOf<Bitmap?>(null) }
    var extractedAccent by remember { mutableStateOf<Color?>(null) }
    var extractedText by remember { mutableStateOf<Color?>(null) }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var mediaTitle by remember { mutableStateOf("Nothing playing") }
    var mediaArtist by remember { mutableStateOf("Start music in another app") }
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }
    var playing by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val theme = themes[themeIndex]
    val accent = extractedAccent ?: theme.accent
    val clockColor = extractedText ?: theme.text

    LaunchedEffect(wallpaperUri) {
        wallpaper = null; extractedAccent = null; extractedText = null
        val uri = wallpaperUri ?: return@LaunchedEffect
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bmp = ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = false }
            wallpaper = bmp
            Palette.from(bmp).generate()?.let { p ->
                val swatch = p.vibrantSwatch ?: p.dominantSwatch ?: p.mutedSwatch
                swatch?.rgb?.let { extractedAccent = Color(it) }; swatch?.titleTextColor?.let { extractedText = Color(it) }
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) { now = LocalDateTime.now(); delay(1000) }
    }
    fun refreshMedia() {
        runCatching {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = manager.getActiveSessions(ComponentName(context, MediaSessionNotificationListener::class.java))
            val controller = sessions.firstOrNull()
            mediaController = controller
            val metadata = controller?.metadata
            mediaTitle = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Nothing playing"
            mediaArtist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: controller?.packageName ?: "Start music in another app"
            albumArt = (metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART))
            playing = controller?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        }
    }
    LaunchedEffect(Unit) { while (true) { refreshMedia(); delay(2500) } }
    val pickWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; wallpaperUri = uri; prefs.edit().putString("wallpaper", uri.toString()).apply() }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = theme.bottom, surface = theme.bottom, primary = accent, onPrimary = theme.text, onSurface = theme.text)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.top, theme.bottom)))) {
            if (wallpaper != null) {
                Image(wallpaper!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.10f), Color.Black.copy(.48f)))))
            }
            val wide = maxWidth > 650.dp
            AnimatedContent(targetState = mediaSeparate, label = "main-screen") { musicScreen ->
                if (musicScreen) {
                    MusicScreen(mediaTitle, mediaArtist, albumArt, playing, accent, clockColor,
                        onBack = { mediaSeparate = false }, onPlay = { mediaController?.transportControls?.let { if (playing) it.pause() else it.play() }; refreshMedia() },
                        onNext = { mediaController?.transportControls?.skipToNext(); refreshMedia() }, onPrev = { mediaController?.transportControls?.skipToPrevious(); refreshMedia() },
                        onRefresh = { refreshMedia() })
                } else {
                    if (wide) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 42.dp, vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                ClockFace(now, clockStyle, use24h, showSeconds, clockColor)
                                Spacer(Modifier.height(14.dp))
                                Text(now.format(DateTimeFormatter.ofPattern("EEEE  •  MMMM d")), color = clockColor.copy(.82f), fontSize = 19.sp, letterSpacing = 2.sp)
                            }
                            Column(Modifier.weight(.8f), verticalArrangement = Arrangement.Center) {
                                MediaCard(mediaTitle, mediaArtist, albumArt, playing, accent, clockColor,
                                    onPlay = { mediaController?.transportControls?.let { if (playing) it.pause() else it.play() }; refreshMedia() },
                                    onNext = { mediaController?.transportControls?.skipToNext(); refreshMedia() },
                                    onPrev = { mediaController?.transportControls?.skipToPrevious(); refreshMedia() },
                                    onExpand = { mediaSeparate = true })
                                Spacer(Modifier.height(20.dp))
                                BottomControls(accent, clockColor, onThemes = { showSettings = true }, onMusic = { mediaSeparate = true }, onWallpaper = { pickWallpaper.launch(arrayOf("image/*")) })
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Spacer(Modifier.weight(.4f))
                            ClockFace(now, clockStyle, use24h, showSeconds, clockColor)
                            Spacer(Modifier.height(12.dp))
                            Text(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), color = clockColor.copy(.85f), fontSize = 16.sp, letterSpacing = 1.sp)
                            Spacer(Modifier.weight(.35f))
                            MediaCard(mediaTitle, mediaArtist, albumArt, playing, accent, clockColor,
                                onPlay = { mediaController?.transportControls?.let { if (playing) it.pause() else it.play() }; refreshMedia() },
                                onNext = { mediaController?.transportControls?.skipToNext(); refreshMedia() },
                                onPrev = { mediaController?.transportControls?.skipToPrevious(); refreshMedia() }, onExpand = { mediaSeparate = true })
                            Spacer(Modifier.height(18.dp))
                            BottomControls(accent, clockColor, onThemes = { showSettings = true }, onMusic = { mediaSeparate = true }, onWallpaper = { pickWallpaper.launch(arrayOf("image/*")) })
                        }
                    }
                }
            }
            if (showSettings) {
                ModalBottomSheet(onDismissRequest = { showSettings = false }, containerColor = theme.bottom, contentColor = clockColor) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("Make it yours", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                        Text("COLOUR MOODS", fontSize = 11.sp, letterSpacing = 2.sp, color = clockColor.copy(.65f))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(themes) { t ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { themeIndex = themes.indexOf(t); prefs.edit().putInt("theme", themeIndex).apply() }) {
                                    Box(Modifier.size(58.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(t.top, t.bottom))), contentAlignment = Alignment.Center) { if (t == theme) Icon(Icons.Default.Check, null, tint = t.text) }
                                    Text(t.name, fontSize = 11.sp, color = clockColor)
                                }
                            }
                        }
                        Text("CLOCK FACE", fontSize = 11.sp, letterSpacing = 2.sp, color = clockColor.copy(.65f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Digital", "Flip", "Minimal", "Analog").forEach { s -> FilterChip(selected = clockStyle == s, onClick = { clockStyle = s; prefs.edit().putString("clock", s).apply() }, label = { Text(s) }) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("24-hour clock", Modifier.weight(1f)); Switch(use24h, { use24h = it; prefs.edit().putBoolean("24h", it).apply() }) }
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("Show seconds", Modifier.weight(1f)); Switch(showSeconds, { showSeconds = it; prefs.edit().putBoolean("seconds", it).apply() }) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("Automatic wallpaper colours"); Text("Use a colour sampled from your photo", fontSize = 12.sp, color = clockColor.copy(.65f)) }
                            Box(Modifier.size(28.dp).clip(CircleShape).background(accent))
                        }
                        Button(onClick = { pickWallpaper.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = theme.bottom)) { Icon(Icons.Default.Wallpaper, null); Spacer(Modifier.width(8.dp)); Text("Choose background photo") }
                        OutlinedButton(onClick = { wallpaperUri = null; wallpaper = null; extractedAccent = null; extractedText = null; prefs.edit().remove("wallpaper").apply() }, modifier = Modifier.fillMaxWidth()) { Text("Remove custom background") }
                        Text("Media access", fontWeight = FontWeight.SemiBold)
                        Text("To control YouTube Music, Spotify and other players, enable CozyStand under Android Notification access.", fontSize = 13.sp, color = clockColor.copy(.72f))
                        OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Tune, null); Spacer(Modifier.width(8.dp)); Text("Open media access settings") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockFace(now: LocalDateTime, style: String, use24h: Boolean, seconds: Boolean, color: Color) {
    val pattern = when { use24h && seconds -> "HH:mm:ss"; use24h -> "HH:mm"; seconds -> "h:mm:ss"; else -> "h:mm" }
    val time = now.format(DateTimeFormatter.ofPattern(pattern))
    when (style) {
        "Flip" -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            time.forEach { ch -> if (ch == ':') Text(":", fontSize = 66.sp, color = color, fontWeight = FontWeight.Light) else Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(.24f)).padding(horizontal = 9.dp, vertical = 8.dp)) { Text(ch.toString(), fontSize = 58.sp, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium) } }
        }
        "Minimal" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(now.format(DateTimeFormatter.ofPattern("h:mm")), fontSize = 100.sp, fontWeight = FontWeight.ExtraLight, color = color, letterSpacing = (-5).sp)
            Text(now.format(DateTimeFormatter.ofPattern("a")), fontSize = 17.sp, color = color.copy(.75f), letterSpacing = 5.sp)
        }
        "Analog" -> AnalogClock(now, color)
        else -> Text(time, fontSize = if (seconds) 72.sp else 100.sp, lineHeight = 106.sp, fontWeight = FontWeight.Light, color = color, letterSpacing = (-4).sp, fontFamily = FontFamily.SansSerif, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun AnalogClock(now: LocalDateTime, color: Color) {
    Box(Modifier.size(230.dp).clip(CircleShape).background(Color.Black.copy(.12f)), contentAlignment = Alignment.Center) {
        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0)
            Box(Modifier.offset(x = (88 * kotlin.math.sin(angle)).dp, y = (-88 * kotlin.math.cos(angle)).dp).size(if (i % 3 == 0) 7.dp else 4.dp).clip(CircleShape).background(color))
        }
        Text("", color = color)
        Box(Modifier.width(5.dp).height(62.dp).offset(y = (-20).dp).clip(RoundedCornerShape(5.dp)).background(color))
        Box(Modifier.width(3.dp).height(85.dp).offset(y = (-25).dp).clip(RoundedCornerShape(5.dp)).background(color.copy(.8f)))
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(now.format(DateTimeFormatter.ofPattern("h:mm")), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), color = color, fontSize = 12.sp)
    }
}

@Composable
private fun MediaCard(title: String, artist: String, art: Bitmap?, playing: Boolean, accent: Color, text: Color, onPlay: () -> Unit, onNext: () -> Unit, onPrev: () -> Unit, onExpand: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(.22f)).clickable { onExpand() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (art != null) Image(art.asImageBitmap(), null, Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        else Box(Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(.28f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = text, modifier = Modifier.size(27.dp)) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = text, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(artist, color = text.copy(.7f), fontSize = 12.sp, maxLines = 1)
        }
        IconButton(onClick = onPrev) { Icon(Icons.Default.SkipPrevious, null, tint = text) }
        IconButton(onClick = onPlay) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = text, modifier = Modifier.size(30.dp)) }
        IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, null, tint = text) }
    }
}

@Composable
private fun MusicScreen(title: String, artist: String, art: Bitmap?, playing: Boolean, accent: Color, text: Color, onBack: () -> Unit, onPlay: () -> Unit, onNext: () -> Unit, onPrev: () -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = text) }; IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null, tint = text) } }
        Spacer(Modifier.height(18.dp))
        if (art != null) Image(art.asImageBitmap(), null, Modifier.fillMaxWidth(.68f).aspectRatio(1f).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxWidth(.68f).aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(accent.copy(.22f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = text, modifier = Modifier.size(90.dp)) }
        Spacer(Modifier.height(28.dp))
        Text(title, color = text, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
        Text(artist, color = text.copy(.7f), fontSize = 16.sp)
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev, modifier = Modifier.size(64.dp)) { Icon(Icons.Default.SkipPrevious, null, tint = text, modifier = Modifier.size(42.dp)) }
            FilledIconButton(onClick = onPlay, modifier = Modifier.size(76.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = Color(0xFF211C18))) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(42.dp)) }
            IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) { Icon(Icons.Default.SkipNext, null, tint = text, modifier = Modifier.size(42.dp)) }
        }
    }
}

@Composable
private fun BottomControls(accent: Color, text: Color, onThemes: () -> Unit, onMusic: () -> Unit, onWallpaper: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color.Black.copy(.20f)).padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onThemes) { Icon(Icons.Default.Palette, null, tint = text); Spacer(Modifier.width(6.dp)); Text("Themes", color = text) }
        TextButton(onClick = onWallpaper) { Icon(Icons.Default.Image, null, tint = text); Spacer(Modifier.width(6.dp)); Text("Photo", color = text) }
        TextButton(onClick = onMusic) { Icon(Icons.Default.MusicNote, null, tint = text); Spacer(Modifier.width(6.dp)); Text("Music", color = text) }
    }
}
