package com.protify.protifyapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NetworkManager(private val context:Context) {

        private val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        private var isConnected = false
        private var connectivityChangeCallback: ((Boolean) -> Unit)? = null
    fun setNetworkChangeListener(listener: (Boolean) -> Unit) {
        connectivityChangeCallback = listener
    }

        private val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                Log.d("NetworkManager", "Network is available")
                isConnected = true
                connectivityChangeCallback?.invoke(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("NetworkManager", "Network is lost")
                isConnected = false
                connectivityChangeCallback?.invoke(false)
            }
        }
    fun startListening() {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    fun stopListening() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    fun isConnected(): Boolean {
        return isConnected
    }
}