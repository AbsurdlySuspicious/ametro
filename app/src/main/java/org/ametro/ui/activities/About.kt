package org.ametro.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import org.ametro.R
import org.ametro.app.Constants
import org.ametro.databinding.ActivityAboutViewBinding
import org.ametro.databinding.WidgetAboutComponentItemBinding
import org.ametro.databinding.WidgetIconButtonBinding
import org.ametro.utils.FileUtils
import org.ametro.utils.misc.getAppVersion
import org.ametro.utils.misc.linkIntent
import org.ametro.utils.ui.applyInsets
import org.ametro.utils.ui.makeBottomInsetsApplier

open class About : AppCompatActivityEx() {
    private lateinit var binding: ActivityAboutViewBinding

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val res = this.resources
        val inflater = LayoutInflater.from(this)

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

        binding.apply {
            license.text = htmlContentRaw(R.raw.license)
            desc.text = htmlContentString(R.string.about_desc)
            version.text = versionNumber

            sectionSetup(sectionComponents, sectionComponentsExpand, components)
            sectionSetup(sectionLicense, sectionLicenseExpand, license)

        }

        binding.components.apply {
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

            adapter = object : SimpleBaseAdapter<WidgetAboutComponentItemBinding, Component>(components) {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
                    Holder(WidgetAboutComponentItemBinding.inflate(inflater, parent, false))

                override fun onBindViewHolder(holder: Holder, position: Int) {
                    val item = items[position]
                    holder.bind.apply {
                        title.text = item.title
                        link.text = item.link
                        root.setOnClickListener {
                            context.startActivity(linkIntent(item.link, true))
                        }
                    }
                }
            }
        }

        binding.buttons.apply {
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            layoutManager = FlexboxLayoutManager(context, FlexDirection.ROW, FlexWrap.WRAP).also {
                it.justifyContent = JustifyContent.SPACE_EVENLY
            }

            adapter = object : SimpleBaseAdapter<WidgetIconButtonBinding, Link>(links) {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
                    Holder(WidgetIconButtonBinding.inflate(inflater, parent, false))

                override fun onBindViewHolder(holder: Holder, position: Int) {
                    val item = items[position]
                    holder.bind.apply {
                        text.text = res.getText(item.text)
                        icon.setImageDrawable(ResourcesCompat.getDrawable(res, item.icon, null))
                        button.setOnClickListener {
                            item.intent?.also { context.startActivity(it) }
                        }

                        (root.layoutParams as FlexboxLayoutManager.LayoutParams).apply {
                            flexGrow = 1f
                        }
                        root.requestLayout()
                    }
                }
            }
        }
    }

    private val versionNumber: String
        get() = getAppVersion(this)

    private val links = arrayOf(
        Link(R.drawable.ic_code, R.string.about_button_src, linkIntent("https://github.com/AbsurdlySuspicious/ametro")),
        Link(R.drawable.ic_bug_report, R.string.about_button_issues, linkIntent("https://github.com/AbsurdlySuspicious/ametro/issues")),
    )

    private val components = arrayOf(
        Component("Maps Icons Collection", "mapicons.mapsmarker.com"),
        Component("Flag Icons by Steven Skelton", "github.com/stevenrskelton/flag-icon"),
        Component("Android-SVG", "github.com/japgolly/svg-android"),
        Component("FasterXML Jackson", "github.com/FasterXML"),
        Component("AndroidX and other google components", "github.com/google"),
        Component("Material components", "github.com/material-components"),
        Component("Kotlin stdlib", "github.com/JetBrains/kotlin")
    )

    private fun sectionSetup(click: View, arrow: View, expand: View) {
        click.setOnClickListener {
            val expanded = expand.isVisible
            val arrowScale = if (expanded) 1f else -1f
            expand.isVisible = !expanded
            arrow.apply { scaleX = arrowScale; scaleY = arrowScale }
        }
    }

    private fun htmlContentRaw(@RawRes resId: Int): SpannableString {
        val html = FileUtils.readAllText(resources.openRawResource(resId))
        return htmlContent(html)
    }

    private fun htmlContentString(@StringRes resId: Int): SpannableString {
        val html = resources.getString(resId)
        return htmlContent(html)
    }

    private fun htmlContent(html: String): SpannableString {
        return SpannableString(Html.fromHtml(html).trimEnd('\r', '\n'))
    }

    data class Link(@DrawableRes val icon: Int, @StringRes val text: Int, val intent: Intent?)
    data class Component(val title: String, val link: String)

    abstract class SimpleBaseAdapter<V : ViewBinding, T>(
        protected val items: Array<T>
    ) : RecyclerView.Adapter<SimpleBaseAdapter<V, T>.Holder>() {
        inner class Holder(val bind: V) : RecyclerView.ViewHolder(bind.root)

        override fun getItemCount(): Int = items.size
    }
}