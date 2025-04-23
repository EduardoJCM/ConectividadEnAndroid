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

fun getNetworkType(context: Context): String {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return "Sin conexión"

    val network = connectivityManager.activeNetwork ?: return "Sin conexión"
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Sin conexión"

    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> detectWifiType(context)
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> detectMobileNetworkType(context)
        else -> "Red desconocida"
    }
}

private fun detectWifiType(context: Context): String {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val freq = wifiManager?.connectionInfo?.frequency ?: -1
            if (freq in 4900..5900) "Wi-Fi 5GHz" else "Wi-Fi 2.4GHz"
        } else {
            "Wi-Fi"
        }
    } catch (e: Exception) {
        "Wi-Fi (error)"
    }
}

private fun detectMobileNetworkType(context: Context): String {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return "Datos móviles"
    }

    return try {
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