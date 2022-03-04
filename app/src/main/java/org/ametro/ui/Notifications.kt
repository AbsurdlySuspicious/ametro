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
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.res.ResourcesCompat
import org.ametro.R
import org.ametro.ui.activities.MapList
import org.ametro.ui.activities.Map
import org.ametro.utils.misc.uniqueRequestCode

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

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun mapUpdateIntent(context: Context, update: Boolean): PendingIntent {
        val i = Intent(context, MapList::class.java).apply {
            putExtra(EXTRA_UPDATE_NOW, update)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(context, uniqueRequestCode(Map.OPEN_MAPS_ACTION), i, 0)
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