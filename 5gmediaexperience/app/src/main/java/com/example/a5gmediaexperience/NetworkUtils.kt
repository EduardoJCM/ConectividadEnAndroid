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
    val upstreamBandwidthKbps: Int
)

fun getNetworkInfo(context: Context): NetworkInfo {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkInfo("Sin conexión", 0, 0)

    val network = connectivityManager.activeNetwork ?: return NetworkInfo("Sin conexión", 0, 0)
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkInfo("Sin conexión", 0, 0)

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
                                TelephonyManager.NETWORK_TYPE_HSPAP -> "3G HSPA+"
                                TelephonyManager.NETWORK_TYPE_HSDPA -> "3G HSDPA"
                                TelephonyManager.NETWORK_TYPE_HSUPA -> "3G HSUPA"
                                TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS"
                                TelephonyManager.NETWORK_TYPE_EDGE -> "2G EDGE"
                                TelephonyManager.NETWORK_TYPE_GPRS -> "2G GPRS"
                                else -> "Datos móviles"
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val networkType = telephonyManager.networkType
                            @Suppress("DEPRECATION")
                            when (networkType) {
                                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                                TelephonyManager.NETWORK_TYPE_LTE -> "4G/LTE"
                                TelephonyManager.NETWORK_TYPE_HSPAP -> "3G HSPA+"
                                TelephonyManager.NETWORK_TYPE_HSDPA -> "3G HSDPA"
                                TelephonyManager.NETWORK_TYPE_HSUPA -> "3G HSUPA"
                                TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS"
                                TelephonyManager.NETWORK_TYPE_EDGE -> "2G EDGE"
                                TelephonyManager.NETWORK_TYPE_GPRS -> "2G GPRS"
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
        }
    )
}