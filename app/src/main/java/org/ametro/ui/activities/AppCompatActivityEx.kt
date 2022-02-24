package org.ametro.ui.activities

import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import org.ametro.R
import org.ametro.app.ApplicationEx
import java.util.*


open class AppCompatActivityEx : AppCompatActivity() {

    companion object {
        protected const val NAVBAR_LIGHT = 0
        protected const val NAVBAR_DARK = 1
    }

    protected fun setupNavbar(variant: Int) {
        return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return
        when (variant) {
            NAVBAR_DARK -> {
                window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.navigationColorForceDark, null)
                window.decorView.apply {
                    systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
            }
            NAVBAR_LIGHT -> {
                window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.navigationColor, null)
                window.decorView.apply {
                    systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
            }
        }
    }

    protected fun setupNavbar() {
        setupNavbar(NAVBAR_LIGHT)
    }

    protected fun setupNavbarForceDark() {
        setupNavbar(NAVBAR_DARK)
    }

    protected fun setLocale(localeCode: String) {
        val locale = Locale(localeCode.lowercase())

        val res = resources
        val dm = res.displayMetrics
        val conf = res.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            conf.setLocale(locale)
            res.updateConfiguration(conf, dm)
        }

        conf.locale = locale
        res.updateConfiguration(conf, dm)
    }

    protected fun setLocaleFromSettings() {
        val app = ApplicationEx.getInstanceActivity(this)
        val lang = app.applicationSettingsProvider.preferredAppLanguage
        if (lang != null)
            setLocale(lang)
        else
            setLocale(Locale.getDefault().language)
    }

    protected fun onCreateTasks() {
        setLocaleFromSettings()
        setupNavbar()
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        onCreateTasks()
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        onCreateTasks()
        super.onCreate(savedInstanceState)
    }
}