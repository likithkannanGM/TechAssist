package com.example.techassist.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Emits true when the device has an active network with internet capability, false otherwise.
 * This reports the device's connectivity only. It does not mean the app "needs" the network:
 * the app is designed to work with no connection at all.
 */
fun Context.networkStatusFlow(): Flow<Boolean> = callbackFlow {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun currentlyOnline(): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(currentlyOnline()) }
        override fun onLost(network: Network) { trySend(currentlyOnline()) }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        }
    }

    trySend(currentlyOnline())
    cm.registerDefaultNetworkCallback(callback)
    awaitClose { cm.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()