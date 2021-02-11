package com.rosberry.imagecropper

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
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

    private val helper = Helper(context)
    private val imageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
        isClickable = false
    }
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val touch = PointF()
    private val translation = PointF(0f, 0f)

    private var bitmapOptions: BitmapFactory.Options? = null
    private var gridEnabled = false
    private var isScaling = false
    private var isDragging = false
    private var scale = 1f
    private var scaleFactor = 4f
    private var minScale = 1f
    private var maxScale = minScale * scaleFactor
    private var previewScale = 1f
    private var previewWidth = 0
    private var previewHeight = 0

    private var imageAssetName: String? = null
    private var imageUri: Uri? = null
    private var imageResId: Int? = null

    init {
        context.theme
            .obtainStyledAttributes(attrs, R.styleable.CropView, 0, 0)
            .use {
                gridEnabled = it.getBoolean(R.styleable.CropView_gridEnabled, false)
                scaleFactor = it.getFloat(R.styleable.CropView_scaleFactor, 4f)
            }
        addView(imageView)
        addView(overlay)
    }

    fun setImageAsset(fileName: String) {
        imageUri = null
        imageResId = null
        imageAssetName = fileName
        bitmapOptions = helper.getDecodeOptions(fileName)

        val bitmap = bitmapOptions?.let { helper.getPreviewBitmap(fileName, measuredWidth, measuredHeight, it) }
        setPreviewBitmap(bitmap)
    }

    fun setImageResource(resId: Int) {
        imageUri = null
        imageAssetName = null
        imageResId = resId
        bitmapOptions = helper.getDecodeOptions(resId)

        val bitmap = bitmapOptions?.let { helper.getPreviewBitmap(resId, measuredWidth, measuredHeight, it) }
        setPreviewBitmap(bitmap)
    }

    fun setImageUri(uri: Uri) {
        imageResId = null
        imageAssetName = null
        imageUri = uri
        bitmapOptions = helper.getDecodeOptions(uri)

        val bitmap = bitmapOptions?.let { helper.getPreviewBitmap(uri, measuredWidth, measuredHeight, it) }
        setPreviewBitmap(bitmap)
    }

    fun crop(): Bitmap? {
        val rect = getCropRect() ?: return null

        return imageUri?.let { helper.getCroppedBitmap(it, rect) }
            ?: imageResId?.let { helper.getCroppedBitmap(it, rect) }
            ?: imageAssetName?.let { helper.getCroppedBitmap(it, rect) }
    }

    private fun setPreviewBitmap(bitmap: Bitmap?) {
        bitmap ?: throw IllegalStateException()

        bitmapOptions?.let { options ->
            val ratio = options.outWidth / options.outHeight.toFloat()

            if (ratio > 0) {
                previewHeight = min(measuredHeight, options.outHeight)
                previewWidth = (previewHeight * ratio).roundToInt()
            } else {
                previewWidth = min(measuredWidth, options.outWidth)
                previewHeight = (previewWidth / ratio).roundToInt()
            }

            calculateScales()

            imageView.setImageBitmap(bitmap)
            update(true)
        } ?: throw IllegalStateException()
    }

    private fun getCropRect(): Rect? {
        return bitmapOptions?.let { options ->
            val relativeScale = previewScale / scale
            val width = (overlay.frameWidth * relativeScale).toInt()
            val height = (overlay.frameHeight * relativeScale).toInt()
            val relativeLeft = (options.outWidth - width) / 2f
            val relativeTop = (options.outHeight - height) / 2f
            val left = (relativeLeft - translation.x * relativeScale).toInt()
            val top = (relativeTop - translation.y * relativeScale).toInt()

            Rect(left, top, left + width, top + height)
        }
    }

    private fun getOverlay(attr: TypedArray): CropOverlayView {
        return CropOverlayView(
                context,
                attr.getColor(R.styleable.CropView_frameColor, Color.WHITE),
                attr.getDimension(R.styleable.CropView_frameMargin, resources.getDimension(R.dimen.cropView_frameMargin)),
                FrameShape.values()[attr.getInt(R.styleable.CropView_frameShape, 0)],
                parseRatio(attr.getString(R.styleable.CropView_frameRatio)),
                attr.getDimension(R.styleable.CropView_frameWidth, resources.getDimension(R.dimen.cropView_frameWidth)),
                attr.getColor(R.styleable.CropView_gridColor, Color.WHITE),
                attr.getInt(R.styleable.CropView_gridRows, 3),
                attr.getDimension(R.styleable.CropView_gridWidth, resources.getDimension(R.dimen.cropView_gridWidth)),
                attr.getColor(R.styleable.CropView_overlayColor, Color.argb(128, 0, 0, 0))
        ).apply { isClickable = false }
    }

    private fun calculateScales() {
        bitmapOptions?.let { options ->
            previewScale = options.outWidth / previewWidth.toFloat()
            minScale = max(overlay.frameWidth / previewWidth, overlay.frameHeight / previewHeight)
            maxScale = minScale * scaleFactor
            scale = minScale
            (imageView.layoutParams as LayoutParams).let {
                it.gravity = Gravity.CENTER
                it.height = previewHeight
                it.width = previewWidth
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

    private fun parseRatio(ratio: String?): Float {
        return try {
            ratio?.split(':')
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