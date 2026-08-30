package com.tekphreak.darkbook.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Best-effort, one-shot location fetch for stamping a new entry. Returns null
 * (never throws, never blocks the save indefinitely) if permission isn't
 * granted, location services are off, or no fix arrives within the timeout.
 */
object LocationHelper {
    private const val TIMEOUT_MS = 8_000L

    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val client = LocationServices.getFusedLocationProviderClient(context)
                val cancellationTokenSource = CancellationTokenSource()
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .build()

                client.getCurrentLocation(request, cancellationTokenSource.token)
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location?.let { it.latitude to it.longitude })
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }

                cont.invokeOnCancellation { cancellationTokenSource.cancel() }
            }
        }
    }
}
