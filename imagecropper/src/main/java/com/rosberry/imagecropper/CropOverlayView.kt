package com.rosberry.imagecropper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class CropOverlayView : View {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    val frameWidth get() = cropRect.width()

    var showGrid: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val frameLeft get() = cropRect.left
    private val frameRight get() = cropRect.right
    private val frameTop get() = cropRect.top
    private val frameBottom get() = cropRect.bottom

    private val clipPath = Path()
    private val clipShape = FrameShape.CIRCLE
    private val cropRect = RectF()
    private val frameMargin = 100f
    private val framePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        isAntiAlias = true
    }
    private val frameStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
    private val gridRowCount = 3
    private val gridStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.5f, resources.displayMetrics)
    private val overlayColor = Color.parseColor("#99000000")

    private var gridLines = FloatArray(8 * gridRowCount - 1)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        updateClipPath()
        calculateGridLines()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawOverlay()
            drawGrid()
            drawFrame()
        }
    }

    private fun updateClipPath() {
        cropRect.apply {
            if (measuredWidth > measuredHeight) {
                top = 0 + frameMargin
                bottom = measuredHeight - frameMargin
                left = measuredWidth / 2 - height() / 2
                right = left + height()
            } else {
                left = 0 + frameMargin
                right = measuredWidth - frameMargin
                top = measuredHeight / 2 - width() / 2
                bottom = top + width()
            }
        }

        clipPath.apply {
            reset()
            when (clipShape) {
                FrameShape.RECTANGLE -> addRect(cropRect, Path.Direction.CW)
                FrameShape.CIRCLE -> addOval(cropRect, Path.Direction.CW)
            }
        }
    }

    private fun calculateGridLines() {
        for (i in 0 until gridRowCount - 1) {
            gridLines[8 * i] = frameLeft + frameWidth / gridRowCount * (i + 1)
            gridLines[8 * i + 1] = frameTop
            gridLines[8 * i + 2] = frameLeft + frameWidth / gridRowCount * (i + 1)
            gridLines[8 * i + 3] = frameBottom

            gridLines[8 * i + 4] = frameLeft
            gridLines[8 * i + 5] = frameTop + frameWidth / gridRowCount * (i + 1)
            gridLines[8 * i + 6] = frameRight
            gridLines[8 * i + 7] = frameTop + frameWidth / gridRowCount * (i + 1)
        }
    }

    private fun Canvas.drawOverlay() {
        save()
        clipPath(clipPath, Region.Op.DIFFERENCE)
        drawColor(overlayColor)
        restore()
    }

    private fun Canvas.drawGrid() {
        if (showGrid) {
            save()
            clipPath(clipPath)
            framePaint.strokeWidth = gridStrokeWidth
            drawLines(gridLines, framePaint)
            restore()
        }
    }

    private fun Canvas.drawFrame() {
        framePaint.strokeWidth = frameStrokeWidth
        when (clipShape) {
            FrameShape.RECTANGLE -> drawRect(cropRect, framePaint)
            FrameShape.CIRCLE -> drawOval(cropRect, framePaint)
        }
    }
}
