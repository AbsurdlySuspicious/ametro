package org.ametro.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.app.Constants
import java.util.*
import org.ametro.utils.ui.*


open class AppCompatActivityEx : AppCompatActivity() {

    val panelOpenNavbarColor by lazy {
        ResourcesCompat.getColor(applicationContext.resources, R.color.navigationColor, null)
    }

    val density by lazy {
        resources.displayMetrics.density
    }

    var transparentNavbarColor = Color.TRANSPARENT
        private set
    var transparentNavbarLight = false
        private set

    private fun setTransparentNavbarLightFlag(flags: Int) =
        if (transparentNavbarLight)
            flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        else
            flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()

    @RequiresApi(Constants.INSETS_MIN_API)
    protected fun setNavbarSolid() {
        window.navigationBarColor = panelOpenNavbarColor
        window.decorView.apply {
            systemUiVisibility =
                systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    @RequiresApi(Constants.INSETS_MIN_API)
    protected fun setNavbarTransparent() {
        window.navigationBarColor = transparentNavbarColor
        window.decorView.apply {
            systemUiVisibility = setTransparentNavbarLightFlag(systemUiVisibility)
        }
    }

    @RequiresApi(Constants.INSETS_MIN_API)
    protected fun setFitSystemWindowsFlags(rootView: ViewGroup, keepNavbar: Boolean = false) {
        rootView.apply {
            systemUiVisibility = systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    (if (keepNavbar) 0 else
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
        window.apply {
            if (!keepNavbar) setNavbarTransparent()
            statusBarColor = Color.TRANSPARENT
        }
    }

    @RequiresApi(Constants.INSETS_MIN_API)
    protected fun applyToolbarInsets(
        toolbar: View,
        additionalActions: (WindowInsets) -> Unit = {}
    ) {
        applyInsets(makeTopInsetsApplier(toolbar)) { insets ->
            val bottomWindow = WindowInsetsCompat
                .toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.navigationBars())
                .bottom
            if (bottomWindow / density > 24f) {
                transparentNavbarLight = true
                transparentNavbarColor =
                    ResourcesCompat.getColor(applicationContext.resources, R.color.navigationColorTransparent, null)
                if (window.navigationBarColor == Color.TRANSPARENT)
                    setNavbarTransparent()
            }
            additionalActions(insets)
        }
    }

    private fun setLocale(localeCode: String) {
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

    private fun setLocaleFromSettings() {
        val app = ApplicationEx.getInstanceActivity(this)
        val lang = app.applicationSettingsProvider.preferredAppLanguage
        if (lang != null)
            setLocale(lang)
        else
            setLocale(Locale.getDefault().language)
    }

    private fun onCreateTasks() {
        setLocaleFromSettings()
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