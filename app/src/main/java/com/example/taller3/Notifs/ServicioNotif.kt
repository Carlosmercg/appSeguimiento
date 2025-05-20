package com.example.taller3.Notifs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.example.taller3.R

class ServicioNotif : Service(){

    private var notid = 0
    private lateinit var bd : FirebaseFirestore

    override fun onCreate(){
        super.onCreate()

        bd = FirebaseFirestore.getInstance()
        createNotificationChannel()

    }

    //es necesario hacerle override pq sino la clase debe ser "abstracta" y no queremos eso
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    //------------------------------ acceso firebase ---------------------------------
    // que tenemos que  hacer? -> revisar base de datos para ver si "disponible" es true
    // si lo es se notifica a los usuarios que tal persona esta disponible
    // al darle click a la notif se debe abrir el mapa de seguimiento del usuario que está disponible
    // que implica? estar "escuchando" la base de datos o escuchar cuando se presiona el boton de "disponible"
    // si al presionar el boton de "disponible" el estado del usuario cambia a "true" se notifica, si cambia a false o si ya esta en false no pasa nada


    // ----------------------------- notificaciones metodos ----------------------------------

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel";
            val description = "channel description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("Test", name, importance)
            channel.setDescription(description)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel (channel)
        }
    }

    fun buildNotification(title: String, message: String, icon: Int, target: Class<*>) : Notification {
        val builder = NotificationCompat.Builder(this, "Test")
        builder.setSmallIcon(icon)
        builder.setContentTitle(title);
        builder.setContentText(message);builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        val intent = Intent(this, target)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        return builder.build()
    }

    fun notify (notificacion: Notification){
        notid++
        val notificationManager = NotificationManagerCompat.from(this)

        if(checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
            notificationManager.notify(notid,notificacion)
        }
    }


}