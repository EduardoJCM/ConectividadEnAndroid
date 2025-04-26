package com.example.a5gmediaexperience

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class NetworkInfo(
    val type: String,
    val downstreamBandwidthKbps: Int,
    val upstreamBandwidthKbps: Int,
    val isMetered: Boolean
)

fun getNetworkInfo(context: Context): NetworkInfo {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkInfo("Sin conexión", 0, 0, true)

    val network = connectivityManager.activeNetwork ?: return NetworkInfo("Sin conexión", 0, 0, true)
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkInfo("Sin conexión", 0, 0, true)

    // Verificar si la red es medida
    val isMetered = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        !(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED))
    } else {
        true // Por defecto asumimos que es medida en versiones antiguas
    }

    return NetworkInfo(
        type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED) {
                        val freq = wifiManager?.connectionInfo?.frequency ?: -1
                        if (freq in 4900..5900) "Wi-Fi 5GHz" else "Wi-Fi 2.4GHz"
                    } else {
                        "Wi-Fi"
                    }
                } catch (e: Exception) {
                    "Wi-Fi (error)"
                }
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) != PackageManager.PERMISSION_GRANTED) {
                    "Datos móviles"
                } else {
                    try {
                        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val networkType = telephonyManager.dataNetworkType
                            when (networkType) {
                                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                                TelephonyManager.NETWORK_TYPE_LTE -> "4G/LTE"
                                else -> "Datos móviles"
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            when (telephonyManager.networkType) {
                                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                                TelephonyManager.NETWORK_TYPE_LTE -> "4G/LTE"
                                else -> "Datos móviles"
                            }
                        }
                    } catch (e: Exception) {
                        "Datos móviles"
                    }
                }
            }
            else -> "Red desconocida"
        },
        downstreamBandwidthKbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            capabilities.linkDownstreamBandwidthKbps
        } else {
            0
        },
        upstreamBandwidthKbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            capabilities.linkUpstreamBandwidthKbps
        } else {
            0
        },
        isMetered = isMetered
    )
}