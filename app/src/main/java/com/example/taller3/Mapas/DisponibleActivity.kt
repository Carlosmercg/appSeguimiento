package com.example.taller3.Mapas

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.activity.result.ActivityResultCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.taller3.MenuAccountActivity
import com.example.taller3.R
import com.example.taller3.databinding.ActivityDisponibleBinding
import com.example.taller3.databinding.ActivityLocationsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

class DisponibleActivity : AppCompatActivity() {

    lateinit var binding: ActivityDisponibleBinding
    lateinit var map : MapView

    private lateinit var userID : String
    private lateinit var posicion: GeoPoint
    private lateinit var posicion2: GeoPoint
    private var currentLocationMarker: Marker? = null
    private lateinit var geocoder: Geocoder
    private var ultimaPosicion: GeoPoint? = null
    private var marcador: Marker? = null

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var firstLocationUpdate = true


    /**
     * Callback para cuando se activa la ubicación del dispositivo (GPS).
     */
    val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if(it.resultCode == RESULT_OK){
                startLocationUpdates()
            }
        }
    )

    /**
     * Callback para el permiso de ubicación.
     */
    val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            if(it){
                locationSettings()
            }
        }
    )

    override fun onResume() {
        super.onResume()
        locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        map.onResume()

        val uims = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if(uims.nightMode == UiModeManager.MODE_NIGHT_YES){
            map.
            overlayManager.
            tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }
        posicionFirestore()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        map.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisponibleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa servicios de ubicación
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()

        Configuration.getInstance().load(this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))

        geocoder = Geocoder(baseContext)
        map = binding.osmMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        userID = intent.getStringExtra("userID").toString()

    }

    fun locationSettings(){
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here. // ...
            startLocationUpdates()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    val isr : IntentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettings.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.i("error","There is no GPS hardware")
                }
            }
        }
    }

    private fun createLocationRequest(): com.google.android.gms.location.LocationRequest {
        val request = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
        return request
    }

    private fun createLocationCallback(): LocationCallback {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val loc = result.lastLocation
                if (loc != null) {

                    val newPosition = GeoPoint(loc.latitude, loc.longitude)

                    if (firstLocationUpdate) {
                        posicion = newPosition
                        addLocationMarker()
                        map.controller.setZoom(16.0)
                        map.controller.animateTo(posicion)
                        val firestore = FirebaseFirestore.getInstance()
                        val usuario = Firebase.auth.currentUser

                        val nuevaUbicacion = mapOf(
                            "latitud" to newPosition.latitude,
                            "longitud" to newPosition.longitude
                        )

                        if (usuario != null) {
                            firestore.collection("usuarios").document(usuario.uid)
                                .update(nuevaUbicacion)
                        }
                        firstLocationUpdate = false
                    }
                    else {
                        if(!newPosition.equals(posicion)){
                            posicion = newPosition
                            addLocationMarker()
                            map.controller.setZoom(16.0)
                            map.controller.animateTo(posicion)
                            val firestore = FirebaseFirestore.getInstance()
                            val usuario = Firebase.auth.currentUser

                            val nuevaUbicacion = mapOf(
                                "latitud" to newPosition.latitude,
                                "longitud" to newPosition.longitude
                            )

                            if (usuario != null) {
                                firestore.collection("usuarios").document(usuario.uid)
                                    .update(nuevaUbicacion)
                            }
                        }
                    }

                    updateUI(loc)


                }
            }
        }
        return callback
    }

    fun updateUI(location : Location){
        Log.i("GPS_APP", "(lat: ${location.latitude}, long: ${location.longitude})")
        posicion = GeoPoint(location.latitude, location.longitude)
    }

    fun startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun stopLocationUpdates(){
        locationClient.removeLocationUpdates(locationCallback)
    }

    fun addMarker(p: GeoPoint, snippet: String) {

        if (marcador != null) {
            map.overlays.remove(marcador)
        }

        marcador = createMarker(
            p, snippet, "Posicion de Usuario Disponible",
            R.drawable.baseline_arrow_drop_down_24
        )
        if (marcador != null) {
            map.overlays.add(marcador)
            map.invalidate()
        }
    }

    fun addLocationMarker() {
        if (currentLocationMarker != null) {
            map.overlays.remove(currentLocationMarker)
        }

        var direccion = "Dirección desconocida"

        val addresses = geocoder.getFromLocation(posicion.latitude, posicion.longitude, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            direccion = addresses[0].getAddressLine(0) ?: "Dirección desconocida"
        }

        val marker = Marker(map)
        marker.title = direccion
        marker.subDescription = "Latitud: ${posicion.latitude}\nLongitud: ${posicion.longitude}"

        val myIcon = resources.getDrawable(R.drawable.baseline_accessibility_new_24, theme)
        marker.icon = myIcon
        marker.position = posicion
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        map.overlays.add(marker)
        currentLocationMarker = marker
    }

    fun createMarker(p: GeoPoint, title: String, desc: String, iconID : Int) : Marker? {
        var marker : Marker? = null;
        if(map!=null) {
            marker = Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                val myIcon = getResources().getDrawable(iconID, this.getTheme());
                marker.setIcon(myIcon);
            }
            marker.setPosition(p);
            marker.setAnchor(
                Marker.
                ANCHOR_CENTER, Marker.
                ANCHOR_BOTTOM);
        }
        return marker
    }

    fun posicionFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios")
            .whereEqualTo("id", userID)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val lat = document.getDouble("latitud")
                    val lng = document.getDouble("longitud")
                    val nombre = document.getString("nombre") ?: "Desconocido"

                    if (lat != null && lng != null) {

                        posicion2 = GeoPoint(lat, lng)

                        if (ultimaPosicion == null || ultimaPosicion != posicion2){
                            marcador?.let {
                                map.overlays.remove(it)
                            }
                            addMarker(posicion2, "Usuario $nombre")
                            map.controller.setZoom(18.0)
                            map.controller.animateTo(posicion2)
                            map.invalidate()
                            ultimaPosicion=posicion2
                        }
                    } else {
                        Log.w("Firestore", "Coordenadas de Usuario disponible faltantes en documento ${document.id}")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error al leer usuarios", exception)
            }
    }
}