package org.ametro.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.res.ResourcesCompat
import org.ametro.R
import org.ametro.ui.activities.MapList
import org.ametro.ui.activities.Map

object Notifications {
    private data class ChannelInfo(
        val id: String,
        @StringRes val name: Int,
        @StringRes val desc: Int,
    )

    const val EXTRA_UPDATE_NOW = "update_now"
    const val ID_MAP_UPDATE = 100

    private val CHANNEL_MAP_UPDATE =
        ChannelInfo("map_update", R.string.notif_map_update_chn_name, R.string.notif_map_update_chn_desc)

    private fun createNotificationChannel(context: Context, mgr: NotificationManagerCompat, info: ChannelInfo): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("HEH", "channel: ${mgr.getNotificationChannel(info.id)}")
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val name = context.getString(info.name)
            val channel = NotificationChannel(info.id, name, importance).apply {
                description = context.getString(info.desc)
            }
            mgr.createNotificationChannel(channel)
        }
        return info.id
    }

    private fun pendingIntent(flags: Int): Int =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) flags
        else flags or PendingIntent.FLAG_IMMUTABLE

    private fun mapUpdateIntent(context: Context, update: Boolean): PendingIntent {
        val i = Intent(context, MapList::class.java).apply {
            putExtra(EXTRA_UPDATE_NOW, update)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val flags = pendingIntent(0)
        return PendingIntent.getActivity(context, Map.OPEN_MAPS_ACTION, i, flags)
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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