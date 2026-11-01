package com.example.wavemusic

import android.content.Intent
import android.net.Uri\nimport android.webkit.WebView\nimport android.webkit.WebViewClient\nimport android.view.ViewGroup\nimport androidx.compose.ui.viewinterop.AndroidView\nimport androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.text.input.KeyboardOptions\nimport androidx.compose.ui.text.input.KeyboardType\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.withContext\nimport org.json.JSONObject\nimport java.net.HttpURLConnection\nimport java.net.URLEncoder\nimport java.net.URL
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var selectedAudio by mutableStateOf<Uri?>(null)
    private var selectedName by mutableStateOf("Выберите аудиофайл")
    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedAudio = it
            selectedName = it.lastPathSegment?.substringAfterLast('/') ?: "Локальный трек"
            startPlayback(it)
        }
    }

    private fun startPlayback(uri: Uri) {
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PLAY
            data = uri
        }
        startForegroundService(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var dark by remember { mutableStateOf(true) }
            var tab by remember { mutableStateOf("Главная") }\n            var soundCloudUrl by remember { mutableStateOf("") }
            WaveTheme(dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(26.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("WAVE", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                                Text(when(tab) { "Поиск" -> "Найти музыку"; "Медиатека" -> "Твоя музыка"; "Настройки" -> "Настройки"; else -> "Слушай своё" }, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Rounded.GraphicEq, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        when(tab) {
                            "Настройки" -> SettingsScreen(dark) { dark = it }
                            "Поиск" -> SearchScreen(soundCloudUrl, { soundCloudUrl = it })
                            "Медиатека" -> LibraryScreen(selectedName) { audioPicker.launch(arrayOf("audio/*")) }
                            else -> HomeScreen(selectedName) { audioPicker.launch(arrayOf("audio/*")) }
                        }
                        Spacer(Modifier.weight(1f))
                        MiniPlayer(selectedName)
                        Spacer(Modifier.height(12.dp))
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)) {
                            listOf("Главная", "Поиск", "Медиатека", "Настройки").forEachIndexed { i, label ->
                                val icons = listOf(Icons.Rounded.Home, Icons.Rounded.Search, Icons.Rounded.LibraryMusic, Icons.Rounded.Settings)
                                NavigationBarItem(selected = tab == label, onClick = { tab = label }, icon = { Icon(icons[i], null) }, label = { Text(label, fontSize = 10.sp) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFFB7A5FF), background = Color(0xFF0B0B12),
        surface = Color(0xFF171722), surfaceVariant = Color(0xFF242433),
        onBackground = Color(0xFFF5F3FF), onSurface = Color(0xFFF5F3FF)
    ) else lightColorScheme(
        primary = Color(0xFF6650C8), background = Color(0xFFF5F4FA),
        surface = Color.White, surfaceVariant = Color(0xFFE9E6F3)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun HomeScreen(track: String, pick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.fillMaxWidth().height(190.dp).background(
            Brush.linearGradient(listOf(Color(0xFF5E48B8), Color(0xFF20203D), Color(0xFF11111A))),
            RoundedCornerShape(28.dp)).padding(22.dp)) {
            Column(Modifier.align(Alignment.BottomStart)) {
                Text("YOUR SOUND. YOUR FLOW.", color = Color.White.copy(alpha = .7f), fontSize = 11.sp, letterSpacing = 2.sp)
                Text("Музыка без границ", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Добавь свой первый трек", color = Color.White.copy(alpha = .75f))
            }
        }
        Text("Быстрый доступ", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        ActionCard("Добавить аудио", "Выбери файл на устройстве", Icons.Rounded.AudioFile, pick)
        ActionCard("SoundCloud", "Открой поиск и вставь ссылку на трек", Icons.Rounded.Cloud, {})
        Text("Последний трек", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(track, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f))
    }
}

@Composable
private fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Название трека или ссылка SoundCloud") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(18.dp), singleLine = true)
        Text("SoundCloud", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Вставь ссылку на общедоступный трек. Для каталожного поиска SoundCloud потребуется подключить официальный API и ключ приложения.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f))
        if (query.startsWith("https://")) {
            Button(onClick = { /* SoundCloud player integration point */ }, modifier = Modifier.fillMaxWidth()) { Text("Открыть трек") }
        }
    }
}

@Composable
private fun LibraryScreen(track: String, pick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ActionCard("Импортировать аудио", "MP3, M4A, WAV и другие форматы", Icons.Rounded.Add, pick)
        Text("Недавно добавлено", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(track, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
    }
}

@Composable
private fun SettingsScreen(dark: Boolean, onDark: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Тёмная тема", fontWeight = FontWeight.SemiBold)
                    Text(if (dark) "Dark mode включён" else "Light mode включён", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                }
                Switch(checked = dark, onCheckedChange = onDark)
            }
        }
        Text("Оформление iOS-inspired · Wave Music 1.0", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f), fontSize = 12.sp)
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable { action() }, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .14f), shape = RoundedCornerShape(14.dp)) {
                Icon(icon, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
            }
            Icon(Icons.Rounded.ChevronRight, null)
        }
    }
}

@Composable
private fun MiniPlayer(track: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFF6553A8), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.MusicNote, null, Modifier.padding(12.dp), tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track, maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Локальная музыка", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
            }
            Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
