package org.ametro.providers

import android.content.Context
import android.graphics.drawable.Drawable
import android.content.res.AssetManager
import java.io.IOException
import java.util.*

class IconProvider(context: Context, private val defaultIcon: Drawable?, private val assetPath: String) {
    private val assetManager: AssetManager
    private val icons = HashMap<String, Drawable>()
    private val assets = HashSet<String>()

    init {
        assetManager = context.assets
        try {
            for (assetName in assetManager.list(assetPath)!!) {
                assets.add(assetName.lowercase(Locale.getDefault()))
            }
        } catch (ex: IOException) {
            // no icons available
        }
    }

    fun getIcon(iso: String): Drawable? {
        return icons[iso] ?: run {
            val assetName = iso.lowercase(Locale.getDefault()) + ".png"
            if (assets.contains(assetName)) try {
                Drawable.createFromStream(
                    assetManager.open("$assetPath/$assetName"), null
                )
            } catch (e: IOException) {
                defaultIcon
            }
            else defaultIcon
        }.also { it?.let { icons[iso] = it } }
    }
}