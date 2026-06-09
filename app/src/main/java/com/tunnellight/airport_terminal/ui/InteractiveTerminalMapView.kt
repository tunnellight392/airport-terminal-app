package com.tunnellight.airport_terminal.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.tunnellight.airport_terminal.model.Concourse
import kotlin.math.ceil

/**
 * A full-screen, interactive schematic of a terminal. Supports drag-to-pan and pinch /
 * double-tap to zoom. Each concourse is drawn as a pier with individually labelled gates,
 * and (when present) the terminal's transit line is drawn linking the concourses. Tapping a
 * gate reports it back through [onGateSelected].
 */
class InteractiveTerminalMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class GateBox(val rect: RectF, val label: String, val concourse: String)
    private data class Pier(
        val rect: RectF,
        val name: String,
        val gatesText: String,
        val labelX: Float,
        val labelY: Float,
        val stationY: Float
    )

    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private var concourses: List<Concourse> = emptyList()
    private var transit: String? = null

    private val gateBoxes = mutableListOf<GateBox>()
    private val piers = mutableListOf<Pier>()
    private var worldWidth = 0f
    private var worldHeight = 0f
    private var transitX = 0f

    private var selected: GateBox? = null
    var onGateSelected: ((label: String, concourse: String) -> Unit)? = null

    // View transform.
    private var scale = 1f
    private var minScale = 0.4f
    private var maxScale = 6f
    private var translateX = 0f
    private var translateY = 0f
    private var fitScale = 1f

    // ---- Paints ----
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EAF1F8") }
    private val pierPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B7D4F0") }
    private val pierEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0"); style = Paint.Style.STROKE; strokeWidth = dp(1.5f)
    }
    private val gatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val gateStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1976D2"); style = Paint.Style.STROKE; strokeWidth = dp(1.5f)
    }
    private val gateSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFCA28") }
    private val gateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D47A1"); textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val concourseLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D47A1"); isFakeBoldText = true; textSize = dp(16f)
    }
    private val gatesRangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#546E7A"); textSize = dp(12f)
    }
    private val transitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00897B"); style = Paint.Style.STROKE
        strokeWidth = dp(6f); strokeCap = Paint.Cap.ROUND
    }
    private val transitConnectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4DB6AC"); style = Paint.Style.STROKE; strokeWidth = dp(3f)
    }
    private val stationFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val stationStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00897B"); style = Paint.Style.STROKE; strokeWidth = dp(3f)
    }
    private val stationTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00695C"); textAlign = Paint.Align.CENTER
        isFakeBoldText = true; textSize = dp(10f)
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            translateX -= distanceX
            translateY -= distanceY
            clampTranslation()
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val target = if (scale < fitScale * 1.6f) fitScale * 2.4f else fitScale
            zoomTo(target, e.x, e.y)
            return true
        }
    })

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = (scale * detector.scaleFactor).coerceIn(minScale, maxScale)
            val f = newScale / scale
            translateX = detector.focusX - (detector.focusX - translateX) * f
            translateY = detector.focusY - (detector.focusY - translateY) * f
            scale = newScale
            clampTranslation()
            invalidate()
            return true
        }
    })

    fun setTerminal(concourses: List<Concourse>, transit: String?) {
        this.concourses = concourses
        this.transit = transit
        selected = null
        if (width > 0) {
            buildLayout()
            resetView()
        }
        invalidate()
    }

    fun resetView() {
        if (worldWidth <= 0 || width == 0) return
        fitScale = ((width - dp(32f)) / worldWidth).coerceIn(0.3f, 2.5f)
        minScale = fitScale * 0.7f
        maxScale = fitScale * 5f
        scale = fitScale
        translateX = (width - worldWidth * scale) / 2f
        translateY = dp(16f)
        clampTranslation()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildLayout()
        resetView()
    }

    private fun buildLayout() {
        gateBoxes.clear()
        piers.clear()
        if (concourses.isEmpty()) return

        val pad = dp(20f)
        val gateW = dp(54f)
        val gateH = dp(34f)
        val gateGapX = dp(10f)
        val pierThickness = dp(26f)
        val gateGapY = dp(10f)
        val rowLabelSpace = dp(34f)
        val rowBottomPad = dp(20f)
        val rowHeight = rowLabelSpace + gateH + gateGapY + pierThickness + gateGapY + gateH + rowBottomPad

        val hasTransit = transit != null
        transitX = pad + dp(16f)
        val pierStartX = if (hasTransit) transitX + dp(44f) else pad

        gateTextPaint.textSize = dp(13f)

        var maxPierEnd = 0f
        concourses.forEachIndexed { index, concourse ->
            val rowTop = pad + index * rowHeight
            val pierCenterY = rowTop + rowLabelSpace + gateH + gateGapY + pierThickness / 2f
            val labels = concourse.gateLabels()
            val columns = ceil(labels.size / 2f).toInt().coerceAtLeast(1)
            val gateAreaStart = pierStartX + dp(14f)
            val pierEndX = gateAreaStart + columns * (gateW + gateGapX) + dp(6f)
            maxPierEnd = maxOf(maxPierEnd, pierEndX)

            piers.add(
                Pier(
                    rect = RectF(pierStartX, pierCenterY - pierThickness / 2f, pierEndX, pierCenterY + pierThickness / 2f),
                    name = concourse.name,
                    gatesText = "Gates ${concourse.gates}",
                    labelX = pierStartX + dp(2f),
                    labelY = rowTop + dp(18f),
                    stationY = pierCenterY
                )
            )

            val topY = pierCenterY - pierThickness / 2f - gateGapY - gateH
            val bottomY = pierCenterY + pierThickness / 2f + gateGapY
            labels.forEachIndexed { k, label ->
                val column = k / 2
                val onTop = k % 2 == 0
                val gx = gateAreaStart + column * (gateW + gateGapX)
                val gy = if (onTop) topY else bottomY
                gateBoxes.add(GateBox(RectF(gx, gy, gx + gateW, gy + gateH), label, concourse.name))
            }
        }

        worldWidth = maxPierEnd + pad
        worldHeight = pad + concourses.size * rowHeight + pad
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bgPaint.color)
        if (piers.isEmpty()) return

        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scale, scale)

        // Transit line behind the piers.
        if (transit != null && piers.size > 1) {
            val firstY = piers.first().stationY
            val lastY = piers.last().stationY
            canvas.drawLine(transitX, firstY, transitX, lastY, transitLinePaint)
            for (pier in piers) {
                canvas.drawLine(transitX, pier.stationY, pier.rect.left, pier.stationY, transitConnectorPaint)
            }
        }

        // Piers + concourse labels.
        for (pier in piers) {
            canvas.drawRoundRect(pier.rect, dp(7f), dp(7f), pierPaint)
            canvas.drawRoundRect(pier.rect, dp(7f), dp(7f), pierEdgePaint)
            canvas.drawText(pier.name, pier.labelX, pier.labelY, concourseLabelPaint)
            canvas.drawText(pier.gatesText, pier.labelX, pier.labelY + dp(15f), gatesRangePaint)
        }

        // Transit stations on top of the line.
        if (transit != null && piers.size > 1) {
            for (pier in piers) {
                canvas.drawCircle(transitX, pier.stationY, dp(9f), stationFill)
                canvas.drawCircle(transitX, pier.stationY, dp(9f), stationStroke)
                canvas.drawText("T", transitX, pier.stationY + dp(3.5f), stationTextPaint)
            }
        }

        // Gates with labels.
        val textOffset = (gateTextPaint.descent() + gateTextPaint.ascent()) / 2f
        for (gate in gateBoxes) {
            val fill = if (gate == selected) gateSelectedPaint else gatePaint
            canvas.drawRoundRect(gate.rect, dp(4f), dp(4f), fill)
            canvas.drawRoundRect(gate.rect, dp(4f), dp(4f), gateStroke)
            canvas.drawText(gate.label, gate.rect.centerX(), gate.rect.centerY() - textOffset, gateTextPaint)
        }

        canvas.restore()
    }

    private fun handleTap(screenX: Float, screenY: Float) {
        val worldX = (screenX - translateX) / scale
        val worldY = (screenY - translateY) / scale
        val hit = gateBoxes.firstOrNull { it.rect.contains(worldX, worldY) }
        selected = hit
        invalidate()
        if (hit != null) onGateSelected?.invoke(hit.label, hit.concourse)
    }

    private fun zoomTo(target: Float, focusX: Float, focusY: Float) {
        val newScale = target.coerceIn(minScale, maxScale)
        val f = newScale / scale
        translateX = focusX - (focusX - translateX) * f
        translateY = focusY - (focusY - translateY) * f
        scale = newScale
        clampTranslation()
        invalidate()
    }

    private fun clampTranslation() {
        val contentW = worldWidth * scale
        val contentH = worldHeight * scale
        val marginH = dp(80f)
        val marginV = dp(80f)
        translateX = if (contentW <= width) {
            (width - contentW) / 2f
        } else {
            translateX.coerceIn(width - contentW - marginH, marginH)
        }
        translateY = if (contentH <= height) {
            (height - contentH) / 2f
        } else {
            translateY.coerceIn(height - contentH - marginV, marginV)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }
}
