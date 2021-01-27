package com.rosberry.imagecropper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Region
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class CropOverlayView : View {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    val frameSize get() = cropRect.width()

    var showGrid: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val frameLeft get() = cropRect.left.toFloat()
    private val frameRight get() = cropRect.right.toFloat()
    private val frameTop get() = cropRect.top.toFloat()
    private val frameBottom get() = cropRect.bottom.toFloat()

    private val frameMargin = 100
    private val gridRowCount = 3
    private val cropRect = Rect()
    private val overlayColor = Color.parseColor("#55000000")
    private val frameWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
    private val gridWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
    private val cropPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = resources.getColor(android.R.color.white, null)
    }

    private var gridLines = FloatArray(gridRowCount * 8)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        cropRect.apply {
            left = 0 + frameMargin
            right = measuredWidth - frameMargin
            top = measuredHeight / 2 - width() / 2
            bottom = top + width()
        }

        calculateGridLines()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            save()
            clipOutRect(cropRect)
            drawColor(overlayColor)
            restore()
            drawFrame(this)
            if (showGrid) {
                drawGrid(this)
            }
        }
    }

    private fun calculateGridLines() {
        for (i in 0 until gridRowCount) {
            gridLines[8 * i] = frameLeft + frameSize / gridRowCount * (i + 1)
            gridLines[8 * i + 1] = frameTop
            gridLines[8 * i + 2] = frameLeft + frameSize / gridRowCount * (i + 1)
            gridLines[8 * i + 3] = frameBottom

            gridLines[8 * i + 4] = frameLeft
            gridLines[8 * i + 5] = frameTop + frameSize / gridRowCount * (i + 1)
            gridLines[8 * i + 6] = frameRight
            gridLines[8 * i + 7] = frameTop + frameSize / gridRowCount * (i + 1)
        }
    }

    private fun drawGrid(canvas: Canvas?) {
        cropPaint.strokeWidth = gridWidth
        canvas?.drawLines(gridLines, cropPaint)
    }

    private fun drawFrame(canvas: Canvas?) {
        cropPaint.strokeWidth = frameWidth
        canvas?.drawRect(cropRect, cropPaint)
    }
}
