package com.tunnellight.airport_terminal.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Adds the system-bar and display-cutout insets to this view's existing padding.
 *
 * The screens used to clear the status bar with a hardcoded `paddingTop="72dp"`, which is a guess:
 * it is wrong on devices with a taller status bar, wrong under a display cutout, and wrong in
 * landscape. With targetSdk 35+ the system draws content edge to edge, so the real inset has to be
 * asked for rather than assumed.
 *
 * The base padding is captured once, at the time this is called, and the inset is added to it.
 * Insets can be dispatched repeatedly (rotation, keyboard, multi-window), so adding to the view's
 * *current* padding instead would make it grow every time.
 */
fun View.applySystemBarInsets(
    top: Boolean = false,
    bottom: Boolean = false,
    horizontal: Boolean = false
) {
    val baseTop = paddingTop
    val baseBottom = paddingBottom
    val baseLeft = paddingLeft
    val baseRight = paddingRight

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(
            top = if (top) baseTop + insets.top else baseTop,
            bottom = if (bottom) baseBottom + insets.bottom else baseBottom,
            left = if (horizontal) baseLeft + insets.left else baseLeft,
            right = if (horizontal) baseRight + insets.right else baseRight
        )
        // Returned unconsumed so sibling views can apply the insets they care about.
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}
