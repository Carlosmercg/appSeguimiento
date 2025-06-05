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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ServicioNotif : Service() {

    private val CHANNEL_ID = "DisponibilidadUsuarios"
    private val CHANNEL_NAME = "Disponibilidad de usuarios"
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificaciones()
        registrarListenerDisponibilidad()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones cuando cambia la disponibilidad"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    private fun registrarListenerDisponibilidad() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        listenerRegistration = db.collection("usuarios")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("ServicioNotif", "Error al escuchar cambios", error)
                    return@addSnapshotListener
                }
                if (snapshots == null) {
                    return@addSnapshotListener
                }

                for (change in snapshots.documentChanges) {
                    if (change.type == DocumentChange.Type.MODIFIED) {
                        val doc = change.document
                        val userId = doc.id
                        val nombre = doc.getString("nombre")
                        val disponible = doc.getBoolean("disponible")
                        if (currentUser != null && userId == currentUser.uid) {
                            continue
                        }

                        val mensaje = when {
                            nombre != null && disponible == true -> "$nombre está disponible"
                            nombre != null && disponible == false -> "$nombre ya no está disponible"
                            else -> "Usuario cambió su estado"
                        }

                        val destinoClass: Class<*> = if (FirebaseAuth.getInstance().currentUser != null) {
                            DisponibleActivity::class.java
                        } else {
                            LoginActivity::class.java
                        }

                        // Construir la notificación
                        val notif = buildNotification(
                            "Cambio de disponibilidad",
                            mensaje,
                            com.example.taller3.R.drawable.hombre,
                            destinoClass,
                            null
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                NotificationManagerCompat.from(this).notify(userId.hashCode(), notif)
                            } else {
                                Log.w(
                                    "ServicioNotif",
                                    "No se envía notificación: permiso POST_NOTIFICATIONS denegado"
                                )
                            }
                        } else {
                            NotificationManagerCompat.from(this).notify(userId.hashCode(), notif)
                        }
                    }
                }
            }
    }

    private fun buildNotification(
        title: String,
        message: String,
        iconRes: Int,
        destino: Class<*>,
        extra: String?
    ): Notification {
        val intent = Intent(this, destino).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            extra?.let { putExtra("extra", it) }
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
    }
}
