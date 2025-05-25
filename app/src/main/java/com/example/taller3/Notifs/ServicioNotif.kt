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

    companion object {
        private const val CHANNEL_ID = "UsuarioDisponible"
        private const val FOREGROUND_ID = 1
        private const val NOTIF_ID_DISPONIBILIDAD = 42
        private const val EXTRA_USUARIO_ID = "usuarioID"
    }

    private lateinit var bd: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()
        bd = FirebaseFirestore.getInstance()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        val notForeground = buildNotification(
            "Servicio Activo",
            "Escuchando cambios de disponibilidad",
            R.drawable.ic_launcher_foreground,
            LoginActivity::class.java,
            null
        )
        startForeground(FOREGROUND_ID, notForeground)

        bd.collection("usuarios").addSnapshotListener(object : EventListener<QuerySnapshot> {
            override fun onEvent(snapshots: QuerySnapshot?, error: FirebaseFirestoreException?) {
                if (error != null) {
                    Log.e("ServicioNotif", "Error al escuchar cambios", error)
                    return
                }
                if (snapshots == null) {
                    return
                }

                for (change in snapshots.documentChanges) {
                    if (change.type == DocumentChange.Type.MODIFIED) {
                        val doc = change.document
                        val userId = doc.id
                        val nombre = doc.getString("nombre")
                        val disponible = doc.getBoolean("disponible")

                        var mensaje = "Usuario cambió su estado"
                        if (nombre != null && disponible != null) {
                            if (disponible) {
                                mensaje = nombre + " está disponible"
                            } else {
                                mensaje = nombre + " ya no está disponible"
                            }
                        }

                        val destinoClass: Class<*>
                        if (FirebaseAuth.getInstance().currentUser != null) {
                            destinoClass = DisponibleActivity::class.java
                        } else {
                            destinoClass = LoginActivity::class.java
                        }

                        val notif = buildNotification(
                            "Cambio de disponibilidad",
                            mensaje,
                            R.drawable.ic_launcher_foreground,
                            destinoClass,
                            userId
                        )

                        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            val nm = NotificationManagerCompat.from(applicationContext)
                            nm.notify(NOTIF_ID_DISPONIBILIDAD, notif)
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

    private fun buildNotification(
        title: String,
        message: String,
        iconRes: Int,
        targetClass: Class<*>,
        usuarioId: String?
    ): Notification {
        val intent = Intent(this, targetClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        if (usuarioId != null) {
            intent.putExtra(EXTRA_USUARIO_ID, usuarioId)
        }

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
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.setContentIntent(pending)
        builder.setAutoCancel(true)

        return builder.build()
    }
}
