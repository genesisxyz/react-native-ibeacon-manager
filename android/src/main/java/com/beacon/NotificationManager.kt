package com.beacon

import androidx.core.app.NotificationCompat
import android.content.Context
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import java.lang.ref.WeakReference

class NotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "Notification"
        private const val NOTIFICATION_ID = 12345678
        private const val NOTIFICATION_CHANNEL_ID = "beacon"
        private const val NOTIFICATION_CONTENT_TITLE = "Beacon"
        private const val NOTIFICATION_CONTENT_TEXT = "Beacon scan enabled"
        private const val NOTIFICATION_SMALL_ICON = "ic_launcher"
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    lateinit var notification: Notification

    private val smallIconId: Int
        get() {
            var id = context.resources.getIdentifier(notificationSmallIcon, "drawable", context.packageName)
            if (id == 0) {
                id = context.resources.getIdentifier(notificationSmallIcon, "mipmap", context.packageName)
            }
            return id
        }

    var notificationId = NOTIFICATION_ID
        private set
    var notificationChannelId = NOTIFICATION_CHANNEL_ID
        private set
    var notificationContentTitle = NOTIFICATION_CONTENT_TITLE
        private set
    var notificationContentText = NOTIFICATION_CONTENT_TEXT
        private set
    var notificationChannelName = NOTIFICATION_CONTENT_TEXT
        private set
    var notificationChannelDescription = NOTIFICATION_CONTENT_TEXT
        private set
    var notificationSmallIcon = NOTIFICATION_SMALL_ICON
        private set

    init {
        initNotification()
    }

    fun initNotification() {
        createNotificationChannel()

        notificationBuilder = NotificationCompat.Builder(context, notificationChannelId)
                .setContentTitle(notificationContentTitle)
                .setContentText(notificationContentText)
                // .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)

        if (smallIconId != 0) {
            notificationBuilder!!.setSmallIcon(smallIconId);
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            // We're defaulting to the behaviour prior API 31 (mutable) even though Android recommends immutability
            val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag)
            notificationBuilder.setContentIntent(contentIntent)
        }

        notification = notificationBuilder!!.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(notificationChannelId, notificationChannelName, importance)
            channel.description = notificationChannelDescription;
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(options: HashMap<String, Any>?) {
        val androidOptions = options?.get("android") as HashMap<String, Any>?;

        val notificationOptions = androidOptions?.get("notification") as HashMap<String, Any>?;
        val newNotificationId = notificationOptions?.get("id") as Double?;
        val newNotificationContentTitle = notificationOptions?.get("contentTitle") as String?;
        val newNotificationContentText = notificationOptions?.get("contentText") as String?;
        val newNotificationSmallIcon = notificationOptions?.get("smallIcon") as String?;

        val notificationChannelOptions = androidOptions?.get("channel") as HashMap<String, Any>?;
        val newNotificationChannelId = notificationChannelOptions?.get("id") as String?;
        val newNotificationChannelName = notificationChannelOptions?.get("name") as String?;
        val newNotificationChannelDescription = notificationChannelOptions?.get("description") as String?;

        if (newNotificationId != null) {
            notificationId = newNotificationId.toInt()
        }

        if (newNotificationChannelId != null) {
            notificationChannelId = newNotificationChannelId
        }

        if (newNotificationContentTitle != null) {
            notificationContentTitle = newNotificationContentTitle
        }

        if (newNotificationContentText != null) {
            notificationContentText = newNotificationContentText
        }

        if (newNotificationChannelName != null) {
            notificationChannelName = newNotificationChannelName
        }

        if (newNotificationChannelDescription != null) {
            notificationChannelDescription = newNotificationChannelDescription
        }

        if (newNotificationSmallIcon != null) {
            notificationSmallIcon = newNotificationSmallIcon
        }
    }

    fun showNotification() {
        notificationBuilder.run {
            setContentTitle(notificationContentTitle)
            setContentText(notificationContentText)

            if (smallIconId != 0) {
                setSmallIcon(smallIconId);
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            this@NotificationManager.notification = build()

            notificationManager.notify(
                    notificationId,
                    this@NotificationManager.notification
            )
        }
    }
}
