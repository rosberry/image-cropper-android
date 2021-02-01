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
    val frameHeight get() = cropRect.height()

    var showGrid: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var ratio: Float = 1f
        set(value) {
            field = value
            updateClipPath()
            calculateGridLines()
            invalidate()
        }

    var frameShape: FrameShape = FrameShape.RECTANGLE
        set(value) {
            field = value
            updateClipPath()
            invalidate()
        }

    private val clipPath = Path()
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
    private val gridLines = FloatArray(8 * gridRowCount - 1)
    private val overlayColor = Color.parseColor("#99000000")

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
                left = measuredWidth / 2 - height() * ratio / 2
                right = left + height() * ratio
            } else {
                left = 0 + frameMargin
                right = measuredWidth - frameMargin
                top = measuredHeight / 2 - width() / ratio / 2
                bottom = top + width() / ratio
            }
        }

        clipPath.apply {
            reset()
            when (frameShape) {
                FrameShape.RECTANGLE -> addRect(cropRect, Path.Direction.CW)
                FrameShape.CIRCLE -> addOval(cropRect, Path.Direction.CW)
            }
        }
    }

    private fun calculateGridLines() {
        for (i in 0 until gridRowCount - 1) {
            gridLines[8 * i] = cropRect.left + frameWidth / gridRowCount * (i + 1)
            gridLines[8 * i + 1] = cropRect.top
            gridLines[8 * i + 2] = cropRect.left + frameWidth / gridRowCount * (i + 1)
            gridLines[8 * i + 3] = cropRect.bottom

            gridLines[8 * i + 4] = cropRect.left
            gridLines[8 * i + 5] = cropRect.top + frameHeight / gridRowCount * (i + 1)
            gridLines[8 * i + 6] = cropRect.right
            gridLines[8 * i + 7] = cropRect.top + frameHeight / gridRowCount * (i + 1)
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
        when (frameShape) {
            FrameShape.RECTANGLE -> drawRect(cropRect, framePaint)
            FrameShape.CIRCLE -> drawOval(cropRect, framePaint)
        }
    }
}
