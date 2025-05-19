
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

/**
 * Servicio que, mientras esté activo, se suscribe a los cambios
 * en la colección "usuarios" de Firestore y dispara notificaciones
 * locales cuando alguien se marca como disponible.
 */
class ServicioDisp : Service() {

    // Contador para dar ID único a cada notificación
    private var contNotif: Int = 0

    // Instancia de Firestore
    private lateinit var bd: FirebaseFirestore

    // Registro de la escucha para poder cancelarla luego
    private var registroEscucha: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()

        // 1) Inicializa Firestore
        bd = FirebaseFirestore.getInstance()

        // 2) Crea el canal de notificaciones (código copiado de la PPT) :contentReference[oaicite:2]{index=2}:contentReference[oaicite:3]{index=3}
        crearCanalNotificaciones()

        // 3) Se suscribe a la colección "usuarios"
        registroEscucha = bd.collection("usuarios")
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(
                    snapshots: QuerySnapshot?,
                    error: FirebaseFirestoreException?
                ) {
                    // Si hay error o no hay datos, no hace nada
                    if (error != null || snapshots == null) {
                        // aquí podrías loggear el error si quisieras
                    } else {
                        // Recorre cada cambio en los documentos
                        for (cambio in snapshots.getDocumentChanges()) {
                            // Solo nos interesan las modificaciones
                            if (cambio.getType() == DocumentChange.Type.MODIFIED) {
                                // Lee el campo "disponible"
                                val disponibleObj = cambio.document.getBoolean("disponible")
                                if (disponibleObj != null && disponibleObj) {
                                    // Convierte a nuestra clase Usuario
                                    val usuario = cambio.document.toObject(Usuario::class.java)

                                    // Decide a qué pantalla mandar al tocar la notificación
                                    val claseDestino: Class<*> =
                                        if (FirebaseAuth.getInstance().currentUser == null) {
                                            LoginActivity::class.java
                                        } else {
                                            MapaLocations::class.java
                                        }

                                    // Construye la notificación (PPT) :contentReference[oaicite:4]{index=4}:contentReference[oaicite:5]{index=5}
                                    val notificacion: Notification = crearNotificacion(
                                        "Usuario disponible",
                                        usuario.nombre + " acaba de estar disponible",
                                        R.drawable.baseline_notifications_none_24,
                                        claseDestino
                                    )
                                    // La envía (PPT) :contentReference[oaicite:6]{index=6}:contentReference[oaicite:7]{index=7}
                                    enviarNotificacion(notificacion)
                                }
                            }
                        }
                    }
                }
            })
    }

    // Servicio en modo "sticky" para que Android lo intente reiniciar si lo mata
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    // Cuando el servicio se destruye, cancelamos la escucha
    override fun onDestroy() {
        registroEscucha?.remove()
        super.onDestroy()
    }

    // No usamos binding, así que devolvemos null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // === Método copiado tal cual de la PPT === :contentReference[oaicite:8]{index=8}:contentReference[oaicite:9]{index=9}
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

    // === Método copiado tal cual de la PPT === :contentReference[oaicite:10]{index=10}:contentReference[oaicite:11]{index=11}
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

    // === Método copiado tal cual de la PPT === :contentReference[oaicite:12]{index=12}:contentReference[oaicite:13]{index=13}
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
