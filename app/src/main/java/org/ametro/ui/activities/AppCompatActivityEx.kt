package org.ametro.ui.activities

import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import org.ametro.R
import org.ametro.app.ApplicationEx
import java.util.*


open class AppCompatActivityEx: AppCompatActivity() {

    protected fun setupNavbar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.navigationColor, null)
            window.decorView.apply {
                systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }

    protected fun setupNavbarForceDark() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.navigationColorForceDark, null)
            window.decorView.apply {
                systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
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