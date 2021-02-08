package com.rosberry.imagecropper

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CropView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    var frameRatio: Float
        get() = overlay.ratio
        set(value) {
            overlay.ratio = value
            calculateScales()
            update(true)
        }

    var frameShape: FrameShape
        get() = overlay.frameShape
        set(value) {
            overlay.frameShape = value
        }

    private val xMin: Float get() = (overlay.frameWidth / 2 - imageView.width * scale / 2).coerceAtMost(0f)
    private val yMin: Float get() = (overlay.frameHeight / 2 - imageView.height * scale / 2).coerceAtMost(0f)
    private val xMax: Float get() = (imageView.width * scale / 2 - overlay.frameWidth / 2).coerceAtLeast(0f)
    private val yMax: Float get() = (imageView.height * scale / 2 - overlay.frameHeight / 2).coerceAtLeast(0f)

    private val touch = PointF()
    private val translation = PointF(0f, 0f)
    private val cropHelper = CropHelper(context)
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val overlay = CropOverlayView(context).apply {
        isClickable = false
    }
    private val imageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
        isClickable = false
    }

    private var listener: CropListener? = null
    private var bitmap: Bitmap? = null
    private var scale = 1f
    private var minScale = 1f
    private var maxScale = 4f
    private var bitmapScale = 1f
    private var isScaling = false
    private var isDragging = false
    private var imageWidth = 0
    private var imageHeight = 0

    init {
        addView(imageView)
        addView(overlay)
    }

    fun setListener(listener: CropListener) {
        this.listener = listener
    }

    fun crop(): Bitmap? {
        return bitmap?.let { cropHelper.getCroppedImage(it, translation, overlay.frameWidth, overlay.frameHeight, bitmapScale / scale) }
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

        imageView.setImageBitmap(bitmap)
        update(true)

        listener?.onImageLoaded()
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
            overlay.showGrid = true
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
        overlay.showGrid = false
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