package com.tunnellight.airport_terminal.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tunnellight.airport_terminal.R
import com.tunnellight.airport_terminal.model.Concourse
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a stylised schematic map of a terminal: a central spine with each concourse
 * branching off as a pier, and small squares representing gates. This is generated
 * purely from the concourse / gate data so every terminal gets a readable map.
 */
class TerminalMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var concourses: List<Concourse> = emptyList()

    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    /**
     * Colours come from resources so the schematic follows the light / dark theme. The view is
     * rebuilt when the activity is recreated on a theme change, so reading them once here is
     * enough.
     */
    private fun themeColor(id: Int) = ContextCompat.getColor(context, id)

    private val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(R.color.map_spine)
        style = Paint.Style.FILL
    }
    private val pierPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(R.color.map_pier)
        style = Paint.Style.FILL
    }
    private val gatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(R.color.map_gate)
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(R.color.map_label)
        textSize = dp(12f)
        isFakeBoldText = true
    }
    private val gatesLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(R.color.map_gates_range)
        textSize = dp(10f)
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(R.color.map_bg)
        style = Paint.Style.FILL
    }

    /**
     * Scratch rectangle reused for every shape in [onDraw]. drawRoundRect reads it immediately
     * and does not retain it, so one instance is enough and keeps onDraw allocation-free.
     */
    private val scratchRect = RectF()

    fun setConcourses(concourses: List<Concourse>) {
        this.concourses = concourses
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rowHeight = dp(88f)
        val desiredHeight = (dp(16f) + concourses.size * rowHeight + dp(16f)).toInt()
        setMeasuredDimension(width, max(desiredHeight, dp(104f).toInt()))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (concourses.isEmpty()) return

        val padding = dp(16f)
        scratchRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(scratchRect, dp(12f), dp(12f), bgPaint)

        val rowHeight = dp(88f)
        val spineX = padding + dp(8f)
        val spineWidth = dp(10f)

        // Central terminal spine running top-to-bottom.
        scratchRect.set(spineX, padding, spineX + spineWidth, height - padding)
        canvas.drawRoundRect(scratchRect, dp(5f), dp(5f), spinePaint)

        val pierStartX = spineX + spineWidth
        val pierThickness = dp(14f)

        concourses.forEachIndexed { index, concourse ->
            val rowTop = padding + index * rowHeight
            val pierEndX = width - padding

            // Labels sit in their own band at the top of the row, clear of the pier and gates.
            canvas.drawText(concourse.name, pierStartX + dp(12f), rowTop + dp(16f), labelPaint)
            canvas.drawText("Gates ${concourse.gates}", pierStartX + dp(12f), rowTop + dp(31f), gatesLabelPaint)

            // Pier + gates are drawn below the label band.
            val pierCenterY = rowTop + dp(64f)

            // Pier extending from the spine.
            scratchRect.set(
                pierStartX, pierCenterY - pierThickness / 2f, pierEndX, pierCenterY + pierThickness / 2f
            )
            canvas.drawRoundRect(scratchRect, dp(6f), dp(6f), pierPaint)

            // Gates along the pier, alternating above and below it.
            val gateCount = min(concourse.gateCount, 16)
            val gateSize = dp(7f)
            val gateGap = dp(4f)
            val gateAreaStart = pierStartX + dp(10f)
            val gateAreaEnd = pierEndX - dp(8f)
            if (gateCount > 0 && gateAreaEnd > gateAreaStart) {
                val step = (gateAreaEnd - gateAreaStart) / gateCount
                for (g in 0 until gateCount) {
                    val gx = gateAreaStart + step * g + step / 2f - gateSize / 2f
                    val gy = if (g % 2 == 0) {
                        pierCenterY - pierThickness / 2f - gateGap - gateSize
                    } else {
                        pierCenterY + pierThickness / 2f + gateGap
                    }
                    scratchRect.set(gx, gy, gx + gateSize, gy + gateSize)
                    canvas.drawRoundRect(scratchRect, dp(1.5f), dp(1.5f), gatePaint)
                }
            }
        }
    }
}
