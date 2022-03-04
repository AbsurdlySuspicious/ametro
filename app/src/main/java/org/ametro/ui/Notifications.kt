package org.ametro.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.ametro.R
import org.ametro.ui.activities.Map

object Notifications {
    private data class ChannelInfo(
        val id: String,
        @StringRes val name: Int,
        @StringRes val desc: Int,
        val priority: Int
    )

    const val ID_MAP_UPDATE = 100

    private val CHANNEL_MAP_UPDATE =
        ChannelInfo(
            "map_update",
            R.string.notif_map_update_chn_name,
            R.string.notif_map_update_chn_desc,
            NotificationCompat.PRIORITY_HIGH,
        )

    private fun createNotificationChannel(context: Context, mgr: NotificationManagerCompat, info: ChannelInfo): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            mgr.getNotificationChannel(info.id) == null
        ) {
            val importance = when (info.priority) {
                NotificationCompat.PRIORITY_HIGH -> NotificationManagerCompat.IMPORTANCE_HIGH
                NotificationCompat.PRIORITY_MAX -> NotificationManagerCompat.IMPORTANCE_MAX
                NotificationCompat.PRIORITY_LOW -> NotificationManagerCompat.IMPORTANCE_LOW
                NotificationCompat.PRIORITY_MIN -> NotificationManagerCompat.IMPORTANCE_MIN
                else -> NotificationManagerCompat.IMPORTANCE_DEFAULT
            }
            val name = context.getString(info.name)
            val channel = NotificationChannel(info.id, name, importance).apply {
                description = context.getString(info.desc)
            }
            mgr.createNotificationChannel(channel)
        }

        return info.id
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun mapUpdateIntent(context: Context, update: Boolean): PendingIntent {
        val request =
            if (update) Map.REQUEST_MAPS_UPDATE
            else Map.REQUEST_MAPS
        val i = Intent(context, Map::class.java).apply {
            putExtra(Map.EXTRA_REQUEST, request)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(context, request, i, 0)
    }

    fun mapUpdate(context: Context, show: Boolean, cities: List<String>): Notification {
        val manager = NotificationManagerCompat.from(context)
        val channelId = createNotificationChannel(context, manager, CHANNEL_MAP_UPDATE)

        val updateAction = NotificationCompat.Action(
            null,
            context.getString(R.string.notif_map_update_action_now),
            mapUpdateIntent(context, true)
        )

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_map_update_title))
            .setContentText(cities.joinToString(", "))
            .setPriority(CHANNEL_MAP_UPDATE.priority)
            .setContentIntent(mapUpdateIntent(context, false))
            .setAutoCancel(false)
            .addAction(updateAction)
            .build()

        if (show) {
            manager.cancel(ID_MAP_UPDATE)
            manager.notify(ID_MAP_UPDATE, notif)
        }

        return notif
    }
}