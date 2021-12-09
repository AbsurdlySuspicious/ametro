package org.ametro.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.os.Bundle
import org.ametro.R
import android.widget.TextView
import android.text.method.LinkMovementMethod
import kotlin.Throws
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Html
import android.text.util.Linkify
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import org.ametro.databinding.ActivityAboutViewBinding
import org.ametro.utils.FileUtils
import java.io.IOException
import java.lang.RuntimeException

open class About : AppCompatActivity() {
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

        try {
            binding.text.let { aboutTextView ->
                aboutTextView.text = aboutContent
                aboutTextView.movementMethod = LinkMovementMethod.getInstance()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @get:Throws(IOException::class)
    protected val aboutContent: SpannableString
        get() {
            val builder = SpannableStringBuilder()
            builder.append(
                Html.fromHtml("<p>${getString(R.string.app_name)} $versionNumber</p>")
            )
            builder.append(
                Html.fromHtml(
                    FileUtils.readAllText(resources.openRawResource(R.raw.about))
                )
            )
            Linkify.addLinks(builder, Linkify.ALL)
            return SpannableString(builder)
        }
    private val versionNumber: String
        get() {
            return try {
                packageManager.getPackageInfo(packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                "0.0.0.0"
            }
        }
}