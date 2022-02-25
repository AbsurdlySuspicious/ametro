package org.ametro.ui.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import org.ametro.R
import android.text.method.LinkMovementMethod
import kotlin.Throws
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Html
import org.ametro.app.Constants
import org.ametro.databinding.ActivityAboutViewBinding
import org.ametro.utils.FileUtils
import org.ametro.utils.ui.*
import org.ametro.utils.misc.getAppVersion
import java.io.IOException
import java.lang.RuntimeException

open class About : AppCompatActivityEx() {
    private lateinit var binding: ActivityAboutViewBinding

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.includeToolbar.toolbar)

        supportActionBar?.run {
            setDefaultDisplayHomeAsUpEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        if (Build.VERSION.SDK_INT >= Constants.INSETS_MIN_API) {
            setFitSystemWindowsFlags(binding.root)
            applyToolbarInsets(binding.includeToolbar.toolbar)
            applyInsets(makeBottomInsetsApplier(binding.scrollView, keepHeight = true))
        }

        try {
            binding.text.let {
                it.text = aboutContent
                it.movementMethod = LinkMovementMethod.getInstance()
            }
            binding.license.text = licenseContent
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @get:Throws(IOException::class)
    protected val aboutContent: SpannableString
        get() {
            val aboutResource = FileUtils.readAllText(resources.openRawResource(R.raw.about))
            val builder = SpannableStringBuilder()
            builder.append(Html.fromHtml("<p>${getString(R.string.app_name)} $versionNumber</p>"))
            builder.append(Html.fromHtml(aboutResource))
            return SpannableString(builder)
        }

    @get:Throws(IOException::class)
    protected val licenseContent: SpannableString
        get() {
            val licenseResource = FileUtils.readAllText(resources.openRawResource(R.raw.license))
            return SpannableString(Html.fromHtml(licenseResource))
        }

    private val versionNumber: String
        get() = getAppVersion(this)
}