package com.example.a5gmediaexperience

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

const val NETWORK_TYPE_LTE_CA = 19
const val NET_CAPABILITY_CA = 12

data class NetworkInfo(
    val type: String,
    val downstreamBandwidthKbps: Int,
    val upstreamBandwidthKbps: Int,
    val isMetered: Boolean,
    val networkGeneration: String,
    val networkTechnology: String
)

// Constante para compatibilidad con versiones anteriores
const val NET_CAPABILITY_MMWAVE = 30 // Valor de NetworkCapabilities.NET_CAPABILITY_MMWAVE en API 30+

@SuppressLint("WrongConstant")
@RequiresApi(Build.VERSION_CODES.R)
fun is5GmmWave(context: Context, telephonyManager: TelephonyManager): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                capabilities.hasCapability(NET_CAPABILITY_MMWAVE)
    } else {
        false
    }
}

@RequiresApi(Build.VERSION_CODES.R)
fun is5GSub6(context: Context, telephonyManager: TelephonyManager): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            !is5GmmWave(context, telephonyManager)
}

fun getWifiType(context: Context): String {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            val freq = wifiManager?.connectionInfo?.frequency ?: -1
            when {
                freq in 2400..2500 -> "Wi-Fi 2.4GHz"
                freq in 4900..5900 -> "Wi-Fi 5GHz"
                freq > 5900 -> "Wi-Fi 6GHz"
                else -> "Wi-Fi"
            }
        } else {
            "Wi-Fi (permiso requerido)"
        }
    } catch (e: Exception) {
        "Wi-Fi (error)"
    }
}

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.N)
fun isLteAdvanced(telephonyManager: TelephonyManager): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Usamos reflexión para acceder al método si falla la referencia directa
            val method = telephonyManager.javaClass.getMethod("isCarrierAggregationSupported")
            method.invoke(telephonyManager) as Boolean
        } else {
            telephonyManager.networkSpecifier?.contains("CA") == true ||
                    telephonyManager.networkSpecifier?.contains("lte-a") == true
        }
    } catch (e: Exception) {
        false
    }
}

@RequiresPermission(Manifest.permission.READ_PHONE_STATE)
fun getNetworkGeneration(telephonyManager: TelephonyManager): Pair<String, String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        when (telephonyManager.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G" to "NR"
            NETWORK_TYPE_LTE_CA -> "4.5G" to "LTE-A"  // Usamos nuestra constante definida
            TelephonyManager.NETWORK_TYPE_LTE -> {
                if (isLteAdvanced(telephonyManager)) {
                    "4.5G" to "LTE-A"
                } else {
                    "4G" to "LTE"
                }
            }
            TelephonyManager.NETWORK_TYPE_LTE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isLteAdvanced(telephonyManager)) {
                    "4.5G" to "LTE-A"
                } else {
                    "4G" to "LTE"
                }
            }
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3.5G" to "HSPA+"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "3G" to "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "3G" to "HSUPA"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G" to "UMTS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "2G" to "EDGE"
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G" to "GPRS"
            else -> "Desconocido" to "Desconocido"
        }
    } else {
        @Suppress("DEPRECATION")
        when (telephonyManager.networkType) {
            NETWORK_TYPE_LTE_CA -> "4.5G" to "LTE-A"  // Usamos nuestra constante definida
            TelephonyManager.NETWORK_TYPE_LTE -> {
                if (isLteAdvanced(telephonyManager)) {
                    "4.5G" to "LTE-A"
                } else {
                    "4G" to "LTE"
                }
            }
            TelephonyManager.NETWORK_TYPE_LTE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isLteAdvanced(telephonyManager)) {
                    "4.5G" to "LTE-A"
                } else {
                    "4G" to "LTE"
                }
            }
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3.5G" to "HSPA+"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "3G" to "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "3G" to "HSUPA"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G" to "UMTS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "2G" to "EDGE"
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G" to "GPRS"
            else -> "Desconocido" to "Desconocido"
        }
    }
}

fun getNetworkInfo(context: Context): NetworkInfo {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkInfo("Sin conexión", 0, 0, true, "Desconocido", "Desconocido")

    val network = connectivityManager.activeNetwork ?: return NetworkInfo("Sin conexión", 0, 0, true, "Desconocido", "Desconocido")
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkInfo("Sin conexión", 0, 0, true, "Desconocido", "Desconocido")

    val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)

    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
            val wifiType = getWifiType(context)
            NetworkInfo(
                type = wifiType,
                downstreamBandwidthKbps = capabilities.linkDownstreamBandwidthKbps,
                upstreamBandwidthKbps = capabilities.linkUpstreamBandwidthKbps,
                isMetered = isMetered,
                networkGeneration = "Wi-Fi",
                networkTechnology = when {
                    wifiType.contains("2.4GHz") -> "802.11n"
                    wifiType.contains("5GHz") -> "802.11ac"
                    wifiType.contains("6GHz") -> "802.11ax"
                    else -> "Wi-Fi"
                }
            )
        }
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED) {
                NetworkInfo(
                    type = "Datos móviles",
                    downstreamBandwidthKbps = 0,
                    upstreamBandwidthKbps = 0,
                    isMetered = true,
                    networkGeneration = "Desconocido",
                    networkTechnology = "Desconocido"
                )
            } else {
                try {
                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val (generation, technology) = getNetworkGeneration(telephonyManager)

                    val (type, techDetail) = if (generation == "5G" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (is5GmmWave(context, telephonyManager)) "5G UW" to "mmWave"
                        else if (is5GSub6(context, telephonyManager)) "5G+" to "Sub-6"
                        else "5G" to "NR"
                    } else {
                        generation to technology
                    }

                    NetworkInfo(
                        type = type,
                        downstreamBandwidthKbps = capabilities.linkDownstreamBandwidthKbps,
                        upstreamBandwidthKbps = capabilities.linkUpstreamBandwidthKbps,
                        isMetered = isMetered,
                        networkGeneration = generation,
                        networkTechnology = techDetail
                    )
                } catch (e: Exception) {
                    NetworkInfo(
                        type = "Datos móviles",
                        downstreamBandwidthKbps = 0,
                        upstreamBandwidthKbps = 0,
                        isMetered = true,
                        networkGeneration = "Desconocido",
                        networkTechnology = "Desconocido"
                    )
                }
            }
        }
        else -> NetworkInfo(
            type = "Red desconocida",
            downstreamBandwidthKbps = 0,
            upstreamBandwidthKbps = 0,
            isMetered = true,
            networkGeneration = "Desconocido",
            networkTechnology = "Desconocido"
        )
    }
}