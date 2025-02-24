/*
 *
 *  * Copyright (c) 2021 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.imagecropper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.view.View

@SuppressLint("ViewConstructor")
internal class CropOverlayView(
        context: Context,
        frameColor: Int,
        frameMargin: Float,
        frameShape: FrameShape,
        frameRatio: Float,
        frameThickness: Float,
        gridColor: Int,
        gridRowCount: Int,
        gridThickness: Float,
        overlayColor: Int
) : View(context) {

    val frameWidth get() = cropRect.width()

    val frameHeight get() = cropRect.height()

    var showGrid: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var ratio: Float = frameRatio
        set(value) {
            field = value
            updateFrame()
            invalidate()
        }

    var frameColor = frameColor
        set(value) {
            field = value
            invalidate()
        }

    var frameMargin = frameMargin
        set(value) {
            field = value
            updateFrame()
            invalidate()
        }

    var frameShape = frameShape
        set(value) {
            field = value
            updateClipPath()
            invalidate()
        }

    var frameThickness = frameThickness
        set(value) {
            field = value
            invalidate()
        }

    var gridColor = gridColor
        set(value) {
            field = value
            invalidate()
        }

    var gridThickness = gridThickness
        set(value) {
            field = value
            invalidate()
        }

    var gridRowCount = gridRowCount
        set(value) {
            field = value
            gridLines = FloatArray(8 * value - 1)
            calculateGridLines()
            invalidate()
        }

    var overlayColor = overlayColor
        set(value) {
            field = value
            invalidate()
        }

    private val clipPath = Path()
    private val cropRect = RectF()
    private var gridLines = FloatArray(8 * gridRowCount - 1)
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        updateFrame()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawOverlay()
            drawGrid()
            drawFrame()
        }
    }

    private fun updateFrame() {
        updateClipPath()
        calculateGridLines()
    }

    private fun updateClipPath() {
        cropRect.apply {
            if (measuredWidth / measuredHeight.toFloat() > ratio) {
                alignHorizontally()
                if (width() > measuredWidth - 2 * frameMargin) {
                    alignVertically()
                }
            } else {
                alignVertically()
                if (height() > measuredHeight - 2 * frameMargin) {
                    alignHorizontally()
                }
            }
        }

        clipPath.apply {
            reset()
            when (frameShape) {
                FrameShape.RECTANGLE -> addRect(cropRect, Path.Direction.CW)
                FrameShape.OVAL -> addOval(cropRect, Path.Direction.CW)
            }
        }
    }

    private fun RectF.alignVertically() {
        left = 0 + frameMargin
        right = measuredWidth - frameMargin
        top = measuredHeight / 2 - width() / ratio / 2
        bottom = top + width() / ratio
    }

    private fun RectF.alignHorizontally() {
        top = 0 + frameMargin
        bottom = measuredHeight - frameMargin
        left = measuredWidth / 2 - height() * ratio / 2
        right = left + height() * ratio
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
            paint.apply {
                color = gridColor
                strokeWidth = gridThickness
            }
            drawLines(gridLines, paint)
            restore()
        }
    }

    private fun Canvas.drawFrame() {
        paint.apply {
            color = frameColor
            strokeWidth = frameThickness
        }
        when (frameShape) {
            FrameShape.RECTANGLE -> drawRect(cropRect, paint)
            FrameShape.OVAL -> drawOval(cropRect, paint)
        }
    }
}
