package org.ametro.ui.bottom_panel

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.res.ResourcesCompat
import org.ametro.R
import org.ametro.utils.misc.AnimUtils
import java.util.ArrayList
import kotlin.math.max

class RouteTransfersLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val ACTION_RESIZE = 0
        private const val ACTION_HIDE = 1
        private const val ACTION_SHOW = 2

        private val txfInterpolator = AccelerateDecelerateInterpolator()
    }

    private val viewStash: MutableList<ImageView> = arrayListOf() // todo remove views from stash?
    private var transfers: MutableList<RoutePagerTransfer> = arrayListOf()
    private var txfWidths: TxfWidths? = null
    private var touchAnimProgram: NextPageTxf? = null

    private val lineHeight: Int = context.resources
        .getDimensionPixelSize(R.dimen.panel_bottom_route_line_long_height)
    private val lineMargin: Int = context.resources
        .getDimensionPixelSize(R.dimen.panel_bottom_route_line_long_margin)
    private val lineDrawable: Drawable
        get() = ResourcesCompat
            .getDrawable(context.resources, R.drawable.line_long, null)!!.mutate()

    init {
        this.orientation = HORIZONTAL
    }

    private fun resetView(v: View) {
        (v.layoutParams as LayoutParams).also {
            it.width = 0
            it.leftMargin = 0
            it.rightMargin = 0
        }
    }

    private fun createView() = ImageView(context).also {
        it.layoutParams = LayoutParams(0, lineHeight)
        it.setImageDrawable(lineDrawable.mutate())
        viewStash.add(it)
        this.addView(it)
    }

    private fun makeWidths(newTxf: MutableList<RoutePagerTransfer>): TxfWidths {
        val txfLengthSum = newTxf.fold(0) { acc, i -> acc + i.length }
        val txfCount = newTxf.size
        val txfPartLength =
            if (txfLengthSum == 0 || txfCount == 0) 0
            else this.width / txfLengthSum
        return TxfWidths(txfLengthSum, txfCount, txfPartLength)
    }

    private fun calcWidth(w: TxfWidths, i: Int, t: RoutePagerTransfer): Int {
        var width = t.length * w.txfPartLength
        if (i == w.txfCount - 1)
            width += this.width % w.txfLengthSum
        else
            width -= lineMargin
        return width
    }

    data class NextPageTxf(
        val widths: TxfWidths,
        val program: ArrayList<AnimatedTxf>
    )

    data class TxfWidths(
        val txfLengthSum: Int,
        val txfCount: Int,
        val txfPartLength: Int,
    )

    data class AnimatedTxf(
        val srcColor: Int,
        val dstColor: Int,
        val widthPrev: Int,
        val widthDelta: Int,
        val action: Int
    )

    private fun replaceItemsNoAnim(w: TxfWidths, newTxf: MutableList<RoutePagerTransfer>) {
        for (i in 0 until max(w.txfCount, viewStash.size)) {
            var v = viewStash.getOrNull(i)
            val t = newTxf.getOrNull(i)

            if (t != null) {
                if (v == null)
                    v = createView()

                (v.drawable as GradientDrawable).setColor(t.txf.lineColor)
                (v.layoutParams as LayoutParams).also {
                    it.width = calcWidth(w, i, t)
                    it.rightMargin = lineMargin
                }
                v.requestLayout()
            } else if (v != null) {
                resetView(v)
            }
        }
    }

    private fun replaceItemsSetNext(thisWidths: TxfWidths, nextPage: MutableList<RoutePagerTransfer>?) {
        if (nextPage != null) {
            val nextWidths = makeWidths(nextPage)
            val nextProgram = animProgram(thisWidths, transfers, nextWidths, nextPage)
            this.touchAnimProgram = NextPageTxf(nextWidths, nextProgram)
        } else {
            this.touchAnimProgram = null
        }
    }

    fun replaceItems(
        thisPage: MutableList<RoutePagerTransfer>,
        nextPage: MutableList<RoutePagerTransfer>?,
        animate: Boolean,
        page: Int
    ) {
        this.post {
            val oldTransfers = this.transfers
            val oldWidths = this.txfWidths
            val w = makeWidths(thisPage)

            this.transfers = thisPage
            this.txfWidths = w

            if (!animate
                || page != 0
                || viewStash.isEmpty()
                || thisPage.isEmpty()
                || oldWidths == null
            ) {
                replaceItemsNoAnim(w, thisPage)
                replaceItemsSetNext(w, nextPage)
            } else {
                val animTxf = animProgram(oldWidths, oldTransfers, w, thisPage)

                AnimUtils.getValueAnimator(true, 300, txfInterpolator) { p ->
                    animateViews(animTxf, p)
                }.also {
                    it.doOnEnd { replaceItemsSetNext(w, nextPage) }
                    it.start()
                }
            }

        }
    }

    private fun silentReset() {
        val currentWidths = makeWidths(this.transfers)
        replaceItemsNoAnim(currentWidths, this.transfers)
    }

    fun touchAnimate(progress: Float) {
        if (progress == 0f)
            silentReset()
        else
            touchAnimProgram?.let {
                animateViews(it.program, txfInterpolator.getInterpolation(progress))
            }
    }

    private fun animateViews(animTxf: ArrayList<AnimatedTxf>, p: Float) {
        for ((i, t) in animTxf.withIndex()) {
            val v = viewStash[i]
            val lp = v.layoutParams as LayoutParams

            when (t.action) {
                ACTION_SHOW -> lp.rightMargin = (lineMargin * p).toInt()
                ACTION_HIDE -> lp.rightMargin = (lineMargin - lineMargin * p).toInt()
            }

            lp.width = (t.widthPrev + t.widthDelta * p).toInt()
            v.requestLayout()

            val color = AnimUtils.argbEvaluate(p, t.srcColor, t.dstColor)
            (v.drawable as GradientDrawable).setColor(color)
        }
    }

    private fun animProgram(
        oldWidths: TxfWidths,
        oldTxf: MutableList<RoutePagerTransfer>,
        newWidths: TxfWidths,
        newTxf: MutableList<RoutePagerTransfer>
    ): ArrayList<AnimatedTxf> {
        val animTxf = ArrayList<AnimatedTxf>()
        for (i in 0 until max(newTxf.size, oldTxf.size)) {
            val o = oldTxf.getOrNull(i)
            val t = newTxf.getOrNull(i)
            var v = viewStash.getOrNull(i)

            if (o != null && t != null) {
                val widthPrev = calcWidth(oldWidths, i, o)
                val widthNext = calcWidth(newWidths, i, t) - widthPrev
                val at = AnimatedTxf(o.txf.lineColor, t.txf.lineColor, widthPrev, widthNext, ACTION_RESIZE)
                animTxf.add(at)
            } else if (o != null) {
                val color = o.txf.lineColor
                val widthPrev = calcWidth(oldWidths, i, o)
                val at = AnimatedTxf(color, color, widthPrev, -widthPrev, ACTION_HIDE)
                animTxf.add(at)
            } else if (t != null) {
                if (v == null)
                    v = createView()
                resetView(v)

                val color = t.txf.lineColor
                val widthNext = calcWidth(newWidths, i, t)
                val at = AnimatedTxf(color, color, 0, widthNext, ACTION_SHOW)
                animTxf.add(at)
            }
        }
        return animTxf
    }

}