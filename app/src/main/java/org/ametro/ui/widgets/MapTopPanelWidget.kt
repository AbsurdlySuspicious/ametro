package org.ametro.ui.widgets

import android.animation.Animator
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.ametro.R
import org.ametro.app.Constants

class MapTopPanelWidget(view: ViewGroup) : Animator.AnimatorListener {
    private val view: View
    private val textView: TextView
    private var text: String? = null
    private val hideAnimation: Runnable
    private val showAnimation: Runnable

    private var actionOnEndAnimation: Runnable? = null
    private var visible: Boolean
    private var firstTime: Boolean

    init {
        this.view = view
        textView = view.findViewById(R.id.message)
        visible = false
        firstTime = true

        hideAnimation = Runnable {
            view.animate().setDuration(Constants.ANIMATION_DURATION).setListener(this@MapTopPanelWidget)
                .translationY(-view.height.toFloat())
        }

        showAnimation = Runnable {
            view.visibility = View.VISIBLE
            textView.text = text
            view.animate().setDuration(Constants.ANIMATION_DURATION).setListener(this@MapTopPanelWidget).translationY(0f)
        }
    }

    fun show(newText: String?) {
        if (visible && text != null && newText != null && text == newText) {
            return
        }
        text = newText
        if (!visible && !firstTime) {
            visible = true
            showAnimation.run()
            return
        }
        visible = true
        firstTime = false
        actionOnEndAnimation = showAnimation
        hideAnimation.run()
    }

    fun hide() {
        if (!visible) {
            return
        }
        visible = false
        hideAnimation.run()
    }

    override fun onAnimationStart(animation: Animator) {}
    override fun onAnimationEnd(animation: Animator) {
        if (!visible) view.visibility = View.INVISIBLE
        if (actionOnEndAnimation != null) {
            actionOnEndAnimation!!.run()
            actionOnEndAnimation = null
        }
    }

    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
}