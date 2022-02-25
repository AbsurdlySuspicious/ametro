package org.ametro.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.app.Constants
import org.ametro.utils.misc.ColorUtils
import java.util.*
import org.ametro.utils.ui.*


open class AppCompatActivityEx : AppCompatActivity() {

    val primaryNavbarColor by lazy {
        val resColor = ResourcesCompat.getColor(applicationContext.resources, R.color.primary, null)
        ColorUtils.fromColorInt(resColor).apply { a = 0.1f }.toColorInt()
    }

    val panelOpenNavbarColor by lazy {
        ResourcesCompat.getColor(applicationContext.resources, R.color.navigationColor, null)
    }

    val density by lazy {
        resources.displayMetrics.density
    }

    var transparentNavbarColor = Color.TRANSPARENT
        private set
    var transparentNavbarLight = true
        private set

    private fun setTransparentNavbarLightFlag(flags: Int, light: Boolean) =
        if (light)
            flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        else
            flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()

    @RequiresApi(Constants.INSETS_MIN_API)
    private fun setNavbarColor(@ColorInt color: Int, light: Boolean) {
        window.navigationBarColor = color
        window.decorView.apply {
            systemUiVisibility = setTransparentNavbarLightFlag(systemUiVisibility, light)
        }
    }

    @RequiresApi(Constants.INSETS_MIN_API)
    protected fun setNavbarSolid() {
        setNavbarColor(panelOpenNavbarColor, true)
    }

    @RequiresApi(Constants.INSETS_MIN_API)
    protected fun setNavbarSolidPrimary() {
        setNavbarColor(primaryNavbarColor, false)
    }

    @RequiresApi(Constants.INSETS_MIN_API)
    protected fun setNavbarTransparent() {
        setNavbarColor(transparentNavbarColor, transparentNavbarLight)
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