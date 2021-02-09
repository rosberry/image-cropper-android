package com.rosberry.imagecropper

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.res.use
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CropView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val overlay: CropOverlayView = context.theme
        .obtainStyledAttributes(attrs, R.styleable.CropView, 0, 0)
        .use { getOverlay(it) }

    var frameColor: Int by overlay::frameColor

    var frameMargin: Float by overlay::frameMargin

    var frameRatio: Float
        get() = overlay.ratio
        set(value) {
            overlay.ratio = value
            calculateScales()
            update(true)
        }

    var frameShape: FrameShape by overlay::frameShape

    var frameWidth: Float by overlay::frameStrokeWidth

    var gridColor: Int by overlay::gridColor

    var gridRows: Int by overlay::gridRowCount

    var gridWidth: Float by overlay::gridStrokeWidth

    var overlayColor: Int by overlay::overlayColor

    private val xMin: Float get() = (overlay.frameWidth / 2 - imageView.width * scale / 2).coerceAtMost(0f)
    private val yMin: Float get() = (overlay.frameHeight / 2 - imageView.height * scale / 2).coerceAtMost(0f)
    private val xMax: Float get() = (imageView.width * scale / 2 - overlay.frameWidth / 2).coerceAtLeast(0f)
    private val yMax: Float get() = (imageView.height * scale / 2 - overlay.frameHeight / 2).coerceAtLeast(0f)

    private val touch = PointF()
    private val translation = PointF(0f, 0f)
    private val imageLoadHelper = ImageLoadHelper(context)
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val imageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
        isClickable = false
    }

    private var bitmap: Bitmap? = null
    private var scale = 1f
    private var minScale = 1f
    private var maxScale = 4f
    private var bitmapScale = 1f
    private var isScaling = false
    private var isDragging = false
    private var imageWidth = 0
    private var imageHeight = 0
    private var gridEnabled = false

    init {
        context.theme
            .obtainStyledAttributes(attrs, R.styleable.CropView, 0, 0)
            .use { gridEnabled = it.getBoolean(R.styleable.CropView_gridEnabled, false) }

        addView(imageView)
        addView(overlay)
    }

    fun crop(): Bitmap? {
        return bitmap?.let { CropHelper.getCroppedImage(it, translation, overlay.frameWidth, overlay.frameHeight, bitmapScale / scale) }
    }

    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap

        val bitmapRatio = bitmap.width / bitmap.height.toFloat()

        if (bitmapRatio > 0) {
            imageHeight = min(measuredHeight, bitmap.height)
            imageWidth = (imageHeight * bitmapRatio).roundToInt()
        } else {
            imageWidth = min(measuredWidth, bitmap.width)
            imageHeight = (imageWidth / bitmapRatio).roundToInt()
        }

        calculateScales()

        imageView.setImageBitmap(imageLoadHelper.resizeBitmap(bitmap))
        update(true)
    }

    private fun getOverlay(attrs: TypedArray): CropOverlayView {
        return CropOverlayView(
                context,
                attrs.getColor(R.styleable.CropView_frameColor, Color.WHITE),
                attrs.getDimension(R.styleable.CropView_frameMargin, resources.getDimension(R.dimen.cropView_frameMargin)),
                FrameShape.values()[attrs.getInt(R.styleable.CropView_frameShape, 0)],
                attrs.getString(R.styleable.CropView_frameRatio)
                    .parseRatio(),
                attrs.getDimension(R.styleable.CropView_frameWidth, resources.getDimension(R.dimen.cropView_frameWidth)),
                attrs.getColor(R.styleable.CropView_gridColor, Color.WHITE),
                attrs.getInt(R.styleable.CropView_gridRows, 3),
                attrs.getDimension(R.styleable.CropView_gridWidth, resources.getDimension(R.dimen.cropView_gridWidth)),
                attrs.getColor(R.styleable.CropView_overlayColor, Color.argb(128, 0, 0, 0))
        ).apply { isClickable = false }
    }

    private fun calculateScales() {
        bitmap?.let { bitmap ->
            minScale = max(overlay.frameWidth / imageWidth, overlay.frameHeight / imageHeight)
            maxScale = minScale * 4
            scale = minScale
            bitmapScale = bitmap.width / imageWidth.toFloat()
            (imageView.layoutParams as LayoutParams).let {
                it.gravity = Gravity.CENTER
                it.height = imageHeight
                it.width = imageWidth
            }
        }
    }

    private fun update(checkTranslate: Boolean = false) {
        imageView.apply {
            scaleX = scale
            scaleY = scale
            translationX = translation.x
            translationY = translation.y
        }
        if (checkTranslate) {
            fixImagePosition()
        }
    }

    private fun fixImagePosition() {
        val closestX = translation.x.closestInRange(xMin, xMax)
        val closestY = translation.y.closestInRange(yMin, yMax)
        imageView.clearAnimation()
        if (translation.x != closestX || translation.y != closestY) {
            AnimatorSet()
                .apply {
                    duration = 200
                    playTogether(
                            ValueAnimator
                                .ofFloat(translation.x, closestX)
                                .apply {
                                    addUpdateListener {
                                        translation.x = it.animatedValue as Float
                                        update()
                                    }
                                },
                            ValueAnimator
                                .ofFloat(translation.y, closestY)
                                .apply {
                                    addUpdateListener {
                                        translation.y = it.animatedValue as Float
                                        update()
                                    }
                                }
                    )
                }
                .start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event?.action) {
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> onTouchEnd()
            MotionEvent.ACTION_DOWN -> onTouchStart(event.x, event.y)
            MotionEvent.ACTION_MOVE -> onTouchMove(event.x, event.y)
        }
        update()
        return true
    }

    private fun onTouchStart(x: Float, y: Float) {
        if (!isScaling) {
            if (gridEnabled) overlay.showGrid = true
            isDragging = true
            touch.set(x, y)
        }
    }

    private fun onTouchMove(x: Float, y: Float) {
        if (isDragging) {
            if (!isScaling) {
                translation.add(x - touch.x, y - touch.y)
            }
            touch.set(x, y)
        }
    }

    private fun onTouchEnd() {
        if (gridEnabled) overlay.showGrid = false
        isDragging = false
        update(true)
    }

    private fun PointF.add(x: Float, y: Float) {
        this.x = (this.x + x).coerceIn(xMin, xMax)
        this.y = (this.y + y).coerceIn(yMin, yMax)
    }

    private fun Float.closestInRange(min: Float, max: Float): Float {
        return when {
            this > max -> max
            this < min -> min
            else -> this
        }
    }

    private fun String?.parseRatio(): Float {
        return try {
            this?.split(':')
                ?.let { it[0].toFloat() / it[1].toFloat() }
                ?: 1f
        } catch (e: Exception) {
            1f
        }
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            isDragging = false
            isScaling = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector?.scaleFactor?.let {
                scale = (scale * it).coerceIn(minScale, maxScale)
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            isScaling = false
        }
    }
}