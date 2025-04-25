package com.example.a5gmediaexperience

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

object NetworkMonitor {
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun startMonitoring(context: Context, onNetworkChanged: (NetworkInfo) -> Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


        if (callback != null) return

        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onNetworkChanged(getNetworkInfo(context))
            }

            override fun onLost(network: Network) {
                onNetworkChanged(getNetworkInfo(context))
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                onNetworkChanged(getNetworkInfo(context))
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback!!)
    }
}
