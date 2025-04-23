package com.example.a5gmediaexperience

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity<Bundle> : ComponentActivity() {
    private val permissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.INTERNET
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* No necesitamos manejar la respuesta aquí */ }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        if (permissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            requestPermissionLauncher.launch(permissions)
        }

        setContent {
            VideoSpeedTestApp()
        }
    }
}

@Composable
fun VideoSpeedTestApp() {
    val context = LocalContext.current
    var networkType by remember { mutableStateOf("Detectando red...") }
    var loadingState by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoOption?>(null) }
    var testHistory by remember { mutableStateOf<List<TestResult>>(emptyList()) }

    // Lista fija de videos de prueba
    val videoOptions = listOf(
        VideoOption("Video Pequeño (20MB)", "https://drive.google.com/uc?export=download&id=1w2umNQk2LaHTwLvg_t6Lsoq-N9zKnjRj"),
        VideoOption("Video Mediano (41MB)", "https://drive.google.com/uc?export=download&id=11T_uIHdA_34HzfloD7BbIR7wiledgyhf"),
        VideoOption("Video Grande (127MB)", "https://drive.google.com/uc?export=download&id=16fvYlIMGvx0czauDZsrZ6g2uURSF3EXf")
    )

    // Función para actualizar el tipo de red
    fun updateNetworkType() {
        networkType = getNetworkType(context)
    }

    // Actualizar el tipo de red al iniciar
    LaunchedEffect(Unit) {
        updateNetworkType()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Prueba de Velocidad 5G",
                style = MaterialTheme.typography.headlineSmall
            )

            IconButton(
                onClick = { updateNetworkType() },
                modifier = Modifier.size(48.dp)
            ) {
                Text("⟳")
            }
        }

        // Estado de la red
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Red detectada:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = networkType,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Selector de videos
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Seleccione video de prueba:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            videoOptions.forEach { video ->
                Button(
                    onClick = {
                        if (!isTesting) {
                            selectedVideo = video
                            isTesting = true
                            loadingState = "Preparando prueba de ${video.name}..."
                        }
                    },
                    enabled = !isTesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(video.name)
                }
            }
        }

        // Estado de la prueba
        if (loadingState.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = loadingState,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Historial de pruebas
        Text(
            text = "Historial de Pruebas",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.Start)
        )

        if (testHistory.isEmpty()) {
            Text(
                text = "Realice una prueba para ver resultados",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(testHistory.sortedByDescending { it.timestamp }) { result ->
                    TestResultItem(result)
                    Divider()
                }
            }
        }
    }

    // Lógica de prueba de velocidad
    if (isTesting && selectedVideo != null) {
        LaunchedEffect(isTesting) {
            try {
                val videoToTest = selectedVideo!!
                updateNetworkType()

                loadingState = "Iniciando prueba de ${videoToTest.name}..."

                val startTime = System.currentTimeMillis()
                var bytesDownloaded = 0L

                withContext(Dispatchers.IO) {
                    val connection = URL(videoToTest.url).openConnection().apply {
                        connectTimeout = 30000
                        readTimeout = 300000
                    }

                    connection.connect()
                    val contentLength = connection.contentLengthLong

                    connection.getInputStream().use { inputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            bytesDownloaded += bytesRead
                            withContext(Dispatchers.Main) {
                                loadingState = "Descargando ${bytesDownloaded / (1024 * 1024)}MB de ${contentLength / (1024 * 1024)}MB"
                            }
                        }
                    }
                }

                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                val speed = (bytesDownloaded / (1024.0 * 1024)) / (duration / 1000.0)

                // Agregar al historial
                testHistory = testHistory + TestResult(
                    videoName = videoToTest.name,
                    networkType = networkType,
                    timeMs = duration,
                    speedMBps = speed,
                    fileSizeMB = bytesDownloaded / (1024 * 1024),
                    timestamp = System.currentTimeMillis()
                )

                loadingState = "¡Prueba completada en ${duration}ms!"
            } catch (e: Exception) {
                loadingState = "Error: ${e.message ?: "Error en la prueba"}"
            } finally {
                isTesting = false
                selectedVideo = null
            }
        }
    }
}

@Composable
fun TestResultItem(result: TestResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Video:", style = MaterialTheme.typography.labelMedium)
                Text(result.videoName, style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Red:", style = MaterialTheme.typography.labelMedium)
                Text(result.networkType, style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tamaño:", style = MaterialTheme.typography.labelMedium)
                Text("${result.fileSizeMB} MB", style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tiempo:", style = MaterialTheme.typography.labelMedium)
                Text("${result.timeMs} ms", style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Velocidad:", style = MaterialTheme.typography.labelMedium)
                Text("%.2f MB/s".format(result.speedMBps), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

data class VideoOption(val name: String, val url: String)
data class TestResult(
    val videoName: String,
    val networkType: String,
    val timeMs: Long,
    val speedMBps: Double,
    val fileSizeMB: Long,
    val timestamp: Long
)