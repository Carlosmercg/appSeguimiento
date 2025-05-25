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

    private val CHANNEL_ID = "UsuarioDisponible"
    private var notid = 0
    private lateinit var bd: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()
        bd = FirebaseFirestore.getInstance()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notForeground = buildNotification(
            "Servicio Activo",
            "Escuchando ...",
            R.drawable.ic_launcher_foreground,
            LoginActivity::class.java
        )
        startForeground(1, notForeground)

        bd.collection("usuarios")
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(
                    snapshots: QuerySnapshot?,
                    error: FirebaseFirestoreException?
                ) {
                    if (error != null) {
                        Log.e("ServicioNotif", "Error al escuchar cambios", error)
                        return
                    }
                    if (snapshots != null) {
                        for (change in snapshots.documentChanges) {
                            if (change.type == DocumentChange.Type.MODIFIED) {
                                val doc = change.document
                                val nombre = doc.getString("nombre") ?: "Usuario"
                                val disp = doc.getBoolean("disponible") ?: false
                                val mensaje: String
                                if (disp) {
                                    mensaje = nombre + " está disponible"
                                } else {
                                    mensaje = nombre + " ya no está disponible"
                                }

                                val destinoActivity = if (
                                    FirebaseAuth.getInstance().currentUser != null
                                ) {
                                    DisponibleActivity::class.java
                                } else {
                                    LoginActivity::class.java
                                }

                                val notif = buildNotification(
                                    "Cambio de disponibilidad",
                                    mensaje,
                                    R.drawable.ic_launcher_foreground,
                                    destinoActivity
                                )
                                notid = notid + 1
                                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                                    == PackageManager.PERMISSION_GRANTED
                                ) {
                                    NotificationManagerCompat.from(this@ServicioNotif)
                                        .notify(notid, notif)
                                }
                            }
                        }
                    }
                }
            })

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Disponibilidad de usuarios",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            canal.description = "Notificaciones cuando cambia la disponibilidad"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    private fun buildNotification(
        title: String,
        message: String,
        iconRes: Int,
        target: Class<*>
    ): Notification {
        val intent = Intent(this, target)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        builder.setSmallIcon(iconRes)
        builder.setContentTitle(title)
        builder.setContentText(message)
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        builder.setContentIntent(pending)
        builder.setAutoCancel(true)

        return builder.build()
    }
}
