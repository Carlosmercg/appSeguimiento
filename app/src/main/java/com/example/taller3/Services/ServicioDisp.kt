
package com.example.taller3.Services

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taller3.Auth.LoginActivity
import com.example.taller3.Models.Usuario
import com.example.taller3.mapa.MapaLocations
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.example.taller3.R

class ServicioDisp : Service() {

    // lo de cantidad++, lo usamos como un tipo de "id" para cada notif
    private var contNotif: Int = 0

    // instanciar firestore
    private lateinit var bd: FirebaseFirestore

    // registro de la escucha para poder apagarla o prenderla luego en el codigo
    private var registroEscucha: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()

        // iniciar firestore
        bd = FirebaseFirestore.getInstance()
        crearCanalNotificaciones()

        // nos suscribimos a la coleccion usuarios de nuestra bd
        registroEscucha = bd.collection("usuarios")
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(
                    snapshots: QuerySnapshot?,
                    error: FirebaseFirestoreException?
                ) {
                    // si hay error o no hay datos no hacemos nadita
                    if (error != null || snapshots == null) {

                    } else {
                        for (cambio in snapshots.getDocumentChanges()) {
                            // hay cambios?
                            if (cambio.getType() == DocumentChange.Type.MODIFIED) {
                                // leemos el campo "disponible"
                                val disponibleObj = cambio.document.getBoolean("disponible")
                                if (disponibleObj != null && disponibleObj) {
                                    // convertimos el usuario a objeto dentro de models
                                    val usuario = cambio.document.toObject(Usuario::class.java)

                                    // a qué pantalla te enviamos?
                                    val claseDestino: Class<*> =
                                        if (FirebaseAuth.getInstance().currentUser == null) {
                                            LoginActivity::class.java
                                        } else {
                                            MapaLocations::class.java
                                        }

                                   // crear la notif como tal
                                    val notificacion: Notification = crearNotificacion("Usuario disponible", usuario.nombre + " acaba de estar disponible", icono = R.drawable.luz_notif, claseDestino)
                                    enviarNotificacion(notificacion)
                                }
                            }
                        }
                    }
                }
            })
    }

    // servicio en modo "sticky" para que android lo intente reiniciar si lo mata por x o y motivo
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    // en el ondestrpy ya no mandamos notifs para dejalo simple
    override fun onDestroy() {
        registroEscucha?.remove()
        super.onDestroy()
    }

    // No usamos binding, así que devolvemos null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // diapositivas metodos
    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nombreCanal = "canal_disponibilidad"
            val descripcionCanal = "Notificaciones de usuarios disponibles"
            val importancia = NotificationManager.IMPORTANCE_DEFAULT
            val canal = NotificationChannel("ID_CANAL_DISPONIBILIDAD", nombreCanal, importancia)
            canal.description = descripcionCanal
            val gestor = getSystemService(NotificationManager::class.java)
            gestor.createNotificationChannel(canal)
        }
    }

 // -------Crear notif ---- diapositiva
    private fun crearNotificacion(
        titulo: String,
        texto: String,
        icono: Int,
        destino: Class<*>
    ): Notification {
        val builder = NotificationCompat.Builder(this, "ID_CANAL_DISPONIBILIDAD")
            .setSmallIcon(icono)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val intent = Intent(this, destino)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendiente = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendiente)
        return builder.build()
    }

    // metodo diapositiva para enviar la notif
    private fun enviarNotificacion(notificacion: Notification) {
        contNotif++
        val gestorCompat = NotificationManagerCompat.from(this)
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            gestorCompat.notify(contNotif, notificacion)
        }
    }
}
