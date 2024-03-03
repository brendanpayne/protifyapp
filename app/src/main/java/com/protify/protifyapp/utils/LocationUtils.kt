package com.protify.protifyapp.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices

class LocationUtils(private val context: Context) {
    //Init location manager
    //var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var userLattidue: Double = 0.0
    var userLongitude: Double = 0.0
    fun getCurrentLocation(Callback: (Double, Double) -> Unit) {
        //Fused location client will use any location provider available (Wifi, GPS, etc.)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        //Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        //If not, request permissions
            ActivityCompat.requestPermissions(
                context.applicationContext as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
                )
            return
        }
        else {
            //If permissions are granted, get the last known location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    //Got last known location. In some rare situations this can be null.
                    if (location != null && (location.latitude != 0.0 && location.longitude != 0.0)) {
                        userLattidue = location.latitude
                        userLongitude = location.longitude
                        Callback(userLattidue, userLongitude)
                    }
                }
        }
    }
}