package com.example.taller3.Notifs

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taller3.Auth.LoginActivity
import com.example.taller3.Mapas.DisponibleActivity
import com.example.taller3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot

class ServicioNotif : Service() {

    private var notid = 0
    private lateinit var bd: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()

        bd = FirebaseFirestore.getInstance()
        createNotificationChannel()

        //------------------------------ acceso firebase ---------------------------------
        // que tenemos que  hacer? ->
        // si lo es se notifica a los usuarios que tal persona esta disponible o no
        // que implica? estar "escuchando" la base de datos o escuchar cuando se presiona el boton de "disponible"
        // si al presionar el boton de "disponible" el estado del usuario cambia a "true" se notifica, si esta en "true" igual, si se cambia a false se debe notificar que el usuario ya no esta disponible

        bd.collection("usuarios")
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.e("ServicioNotif", "Error al escuchar cambios", e)
                    return@EventListener
                }
                if (snapshots != null) {
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.MODIFIED) {
                            val doc = change.document
                            val nombre = doc.getString("nombre") ?: "Usuario"
                            val disp = doc.getBoolean("disponible") ?: false
                            val mensaje = if (disp)
                                "$nombre está disponible"
                            else
                                "$nombre ya no está disponible"

                            // elegir destino según estado de autenticación
                            val target = if (FirebaseAuth.getInstance().currentUser != null)
                                DisponibleActivity::class.java
                            else
                                LoginActivity::class.java

                            // construir y enviar notificación
                            val notif = buildNotification(
                                "Cambio de disponibilidad",
                                mensaje,
                                R.drawable.ic_launcher_foreground,
                                target
                            )
                            notify(notif)
                        }
                    }
                }
            })
    }

    //es necesario hacerle override pq sino la clase debe ser "abstracta" y no queremos eso
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    //----------------------------- notificaciones metodos ----------------------------------

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel"
            val description = "channel description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("Test", name, importance)
            channel.setDescription(description)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(title: String, message: String, icon: Int, target: Class<*>): Notification {
        val builder = NotificationCompat.Builder(this, "Usuario Disponible")
        builder.setSmallIcon(icon)
        builder.setContentTitle(title)
        builder.setContentText(message)
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val intent = Intent(this, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        return builder.build()
    }

    fun notify(notificacion: Notification) {
        notid++
        val notificationManager = NotificationManagerCompat.from(this)

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notid, notificacion)
        }
    }

}