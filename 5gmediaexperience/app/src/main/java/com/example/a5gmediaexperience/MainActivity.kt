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
import java.text.SimpleDateFormat
import java.util.*

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
            MaterialTheme {
                VideoSpeedTestApp()
            }
        }
    }
}

@Composable
fun VideoSpeedTestApp() {
    val context = LocalContext.current
    var networkInfo by remember { mutableStateOf(NetworkInfo("Detectando red...", 0, 0)) }
    var loadingState by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoOption?>(null) }
    var testHistory by remember { mutableStateOf<List<TestResult>>(emptyList()) }

    // Lista de videos de prueba
    val videoOptions = listOf(
        VideoOption("Prueba Pequeña (20MB)", "https://drive.google.com/uc?export=download&id=1w2umNQk2LaHTwLvg_t6Lsoq-N9zKnjRj"),
        VideoOption("Prueba Mediana (41MB)", "https://drive.google.com/uc?export=download&id=11T_uIHdA_34HzfloD7BbIR7wiledgyhf"),
        VideoOption("Prueba Grande (100MB)", "https://speedtest.newark.linode.com/100MB-newark.bin")
    )

    // Función para actualizar la información de red
    fun updateNetworkInfo() {
        networkInfo = getNetworkInfo(context)
    }

    // Actualizar al iniciar
    LaunchedEffect(Unit) {
        updateNetworkInfo()
        //Monitorear
        NetworkMonitor.startMonitoring(context) {
            networkInfo = it
        }
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
                onClick = { updateNetworkInfo() },
                modifier = Modifier.size(48.dp)
            ) {
                Text("⟳")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tarjeta de información de red
        NetworkInfoCard(networkInfo)

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de videos
        Text(
            text = "Seleccione video de prueba:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // Estado de la prueba
        if (loadingState.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = loadingState,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Historial de pruebas
        Text(
            text = "Historial de Pruebas",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                updateNetworkInfo()

                loadingState = "Iniciando prueba de ${videoToTest.name}..."

                val startTime = System.currentTimeMillis()
                var bytesDownloaded = 0L
                var contentLength = 0L

                withContext(Dispatchers.IO) {
                    val connection = URL(videoToTest.url).openConnection().apply {
                        connectTimeout = 30000
                        readTimeout = 600000 // 10 minutos timeout

                        // Headers para PixelDrain
                        if (videoToTest.url.contains("pixeldrain.com")) {
                            setRequestProperty("Accept", "application/octet-stream")
                        }
                    }

                    connection.connect()
                    contentLength = connection.contentLengthLong

                    connection.getInputStream().use { inputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            bytesDownloaded += bytesRead

                            // Actualizar progreso cada 5MB
                            if (bytesDownloaded % (5 * 1024 * 1024) == 0L) {
                                withContext(Dispatchers.Main) {
                                    val currentSpeed = (bytesDownloaded / (1024.0 * 1024)) /
                                            ((System.currentTimeMillis() - startTime) / 1000.0)

                                    loadingState = buildString {
                                        append("Descargando ${bytesDownloaded / (1024 * 1024)}MB")
                                        if (contentLength > 0) {
                                            append(" de ${contentLength / (1024 * 1024)}MB")
                                        }
                                        append("\nVelocidad actual: %.2f MB/s".format(currentSpeed))
                                    }
                                }
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
                    networkType = networkInfo.type,
                    timeMs = duration,
                    speedMBps = speed,
                    fileSizeMB = bytesDownloaded / (1024 * 1024),
                    timestamp = System.currentTimeMillis(),
                    estimatedBandwidth = networkInfo.downstreamBandwidthKbps / 1000.0
                )

                loadingState = "¡Prueba completada!\n" +
                        "Tamaño: ${bytesDownloaded / (1024 * 1024)} MB\n" +
                        "Tiempo: ${duration}ms\n" +
                        "Velocidad: %.2f MB/s".format(speed)

            } catch (e: Exception) {
                loadingState = "Error en la prueba:\n${e.message ?: "Error desconocido"}"
            } finally {
                isTesting = false
                selectedVideo = null
            }
        }
    }
}

@Composable
fun NetworkInfoCard(networkInfo: NetworkInfo) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tipo de red
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tipo de red:", style = MaterialTheme.typography.labelMedium)
                Text(networkInfo.type, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ancho de banda descendente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bajada estimada:", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = if (networkInfo.downstreamBandwidthKbps > 0) {
                        "%.2f Mbps".format(networkInfo.downstreamBandwidthKbps / 1000.0)
                    } else {
                        "No disponible"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Ancho de banda ascendente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subida estimada:", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = if (networkInfo.upstreamBandwidthKbps > 0) {
                        "%.2f Mbps".format(networkInfo.upstreamBandwidthKbps / 1000.0)
                    } else {
                        "No disponible"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun TestResultItem(result: TestResult) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(result.timestamp) {
        dateFormat.format(Date(result.timestamp))
    }

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
                Text("Hora:", style = MaterialTheme.typography.labelMedium)
                Text(formattedTime, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Video:", style = MaterialTheme.typography.labelMedium)
                Text(result.videoName, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Red:", style = MaterialTheme.typography.labelMedium)
                Text(result.networkType, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tamaño:", style = MaterialTheme.typography.labelMedium)
                Text("${result.fileSizeMB} MB", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tiempo:", style = MaterialTheme.typography.labelMedium)
                Text("${result.timeMs} ms", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Velocidad:", style = MaterialTheme.typography.labelMedium)
                Text("%.2f MB/s".format(result.speedMBps), style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ancho de banda estimado:", style = MaterialTheme.typography.labelMedium)
                Text(
                    "%.2f Mbps".format(result.estimatedBandwidth),
                    style = MaterialTheme.typography.bodyMedium
                )
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
    val timestamp: Long,
    val estimatedBandwidth: Double
)