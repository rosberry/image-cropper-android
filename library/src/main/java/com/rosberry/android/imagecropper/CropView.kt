/*
 *
 *  * Copyright (c) 2021 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.imagecropper

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * View provides image preview, customizable overlay with crop frame and tools to crop images.
 */
class CropView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val overlayView: CropOverlayView = context.theme
        .obtainStyledAttributes(attrs, R.styleable.CropView, 0, 0)
        .use { getOverlayView(it) }

    /**
     * Crop frame stroke color represented as packed color int. Default is #FFF.
     */
    var frameColor: Int by overlayView::frameColor

    /**
     * Minimal distance between view bounds and crop frame in pixels. Default is 16dp.
     */
    var frameMargin: Float by overlayView::frameMargin

    /**
     * Crop frame aspect ratio. Default is 1:1.
     */
    var frameRatio: Float
        get() = overlayView.ratio
        set(value) {
            overlayView.ratio = value
            calculateScales()
            update(true)
        }

    /**
     * Crop frame shape. Can be either [FrameShape.RECTANGLE] or [FrameShape.OVAL]. Default is [FrameShape.RECTANGLE].
     */
    var frameShape: FrameShape by overlayView::frameShape

    /**
     * Crop frame line thickness in pixels. Default is 1dp.
     */
    var frameThickness: Float by overlayView::frameThickness

    /**
     * Grid lines color. Default is #FFF.
     * @see gridEnabled
     */
    var gridColor: Int by overlayView::gridColor

    /**
     * Number of grid rows and columns appears on user interaction if grid is enabled. Default is 3.
     * @see gridEnabled
     */
    var gridRows: Int by overlayView::gridRowCount

    /**
     * Grid line thickness in pixels. Default is 0.5dp.
     * @see gridEnabled
     */
    var gridThickness: Float by overlayView::gridThickness

    /**
     * Color fill outside of the crop area. Default is #CC000000.
     */
    var overlayColor: Int by overlayView::overlayColor

    /**
     * Controls whether grid should appear on user interaction. Default is false.
     * @see gridRows
     * @see gridColor
     * @see gridThickness
     */
    var gridEnabled = false

    private val xMin: Float get() = (overlayView.frameWidth / 2 - imageView.width * scale / 2).coerceAtMost(0f)
    private val yMin: Float get() = (overlayView.frameHeight / 2 - imageView.height * scale / 2).coerceAtMost(0f)
    private val xMax: Float get() = (imageView.width * scale / 2 - overlayView.frameWidth / 2).coerceAtLeast(0f)
    private val yMax: Float get() = (imageView.height * scale / 2 - overlayView.frameHeight / 2).coerceAtLeast(0f)

    private val imageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
        isClickable = false
    }
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val touch = PointF()
    private val translation = PointF(0f, 0f)

    private var state = State.IDLE
    private var scale = 1f
    private var scaleFactor = 4f
    private var minScale = 1f
    private var maxScale = minScale * scaleFactor
    private var previewWidth = 0
    private var previewHeight = 0

    private var source: ImageSource<*>? = null
    private var callback: ImageLoadCallback? = null

    init {
        context.theme
            .obtainStyledAttributes(attrs, R.styleable.CropView, 0, 0)
            .use {
                gridEnabled = it.getBoolean(R.styleable.CropView_gridEnabled, false)
                scaleFactor = it.getFloat(R.styleable.CropView_scaleFactor, 4f)
            }
        addView(imageView)
        addView(overlayView)
    }

    /**
     * Registers callback to be invoked when image preview is set. Mainly for cases when
     * update layout after image loaded is necessary (e.g. change view visibility or show controls).
     * Note that callback will be invoked on main thread.
     */
    fun setCallback(callback: ImageLoadCallback?) {
        this.callback = callback
    }

    /**
     * Loads image asset with provided file name.
     * If the file with provided `fileName` could not be opened [IOException] will be thrown.
     * @param fileName asset file name
     */
    fun setImageAsset(fileName: String) {
        source = AssetSource(context, fileName)
        setPreviewBitmap()
    }

    /**
     * Loads image resource with provided id.
     * If the given `resId` does not exist [android.content.res.Resources.NotFoundException] will be thrown.
     * @param resId resource id
     */
    fun setImageResource(resId: Int) {
        source = ResourceSource(context, resId)
        setPreviewBitmap()
    }

    /**
     * Loads image file with provided uri.
     * If the provided `uri` could not be opened [java.io.FileNotFoundException] will be thrown.
     * @param uri file uri
     */
    fun setImageUri(uri: Uri) {
        source = MediaSource(context, uri)
        setPreviewBitmap()
    }

    /**
     * Crops current loaded image applying minimum downsampling to prevent OOM exception.
     * Note that resulting bitmap might be too large to be used without resizing.
     * @return `bitmap` cropped from original image or `null` if selected region decoding fails
     * @throws IllegalStateException if no image source set
     * @see setImageAsset
     * @see setImageResource
     * @see setImageUri
     * @see android.graphics.BitmapRegionDecoder.decodeRegion
     */
    fun crop(): Bitmap? {
        return source.run {
            this ?: throw IllegalStateException("Image source must be set before applying crop.")

            getCroppedBitmap(
                    previewWidth,
                    overlayView.frameWidth,
                    overlayView.frameHeight,
                    translation,
                    scale
            )
        }
    }

    private fun setPreviewBitmap() {
        val bitmap = source?.getPreviewBitmap(measuredWidth, measuredHeight) ?: throw IOException("Failed to load preview bitmap.")

        source?.options?.let { options ->
            val ratio = bitmap.width / bitmap.height.toFloat()

            if (ratio > 0) {
                previewHeight = min(measuredHeight, options.outHeight)
                previewWidth = (previewHeight * ratio).roundToInt()
            } else {
                previewWidth = min(measuredWidth, options.outWidth)
                previewHeight = (previewWidth / ratio).roundToInt()
            }

            calculateScales()
            post {
                imageView.setImageBitmap(bitmap)
                update(true)
                callback?.onImageLoaded()
            }
        }
    }

    private fun getOverlayView(attr: TypedArray): CropOverlayView {
        return CropOverlayView(
                context = context,
                frameColor = attr.getColor(R.styleable.CropView_frameColor, ContextCompat.getColor(context, R.color.cropView_colorGrid)),
                frameMargin = attr.getDimension(R.styleable.CropView_frameMargin, resources.getDimension(R.dimen.cropView_frameMargin)),
                frameShape = FrameShape.values()[attr.getInt(R.styleable.CropView_frameShape, 0)],
                frameRatio = parseRatio(attr.getString(R.styleable.CropView_frameRatio)),
                frameThickness = attr.getDimension(R.styleable.CropView_frameThickness, resources.getDimension(R.dimen.cropView_frameThickness)),
                gridColor = attr.getColor(R.styleable.CropView_gridColor, ContextCompat.getColor(context, R.color.cropView_colorGrid)),
                gridRowCount = attr.getInt(R.styleable.CropView_gridRows, 3),
                gridThickness = attr.getDimension(R.styleable.CropView_gridThickness, resources.getDimension(R.dimen.cropView_gridThickness)),
                overlayColor = attr.getColor(R.styleable.CropView_overlayColor, ContextCompat.getColor(context, R.color.cropView_colorOverlay))
        ).apply { isClickable = false }
    }

    private fun calculateScales() {
        source ?: return

        minScale = max(overlayView.frameWidth / previewWidth, overlayView.frameHeight / previewHeight)
        maxScale = minScale * scaleFactor
        scale = minScale
        (imageView.layoutParams as LayoutParams).let {
            it.gravity = Gravity.CENTER
            it.height = previewHeight
            it.width = previewWidth
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
        if (state == State.IDLE) {
            if (gridEnabled) overlayView.showGrid = true
            state = State.DRAG
            touch.set(x, y)
        }
    }

    private fun onTouchMove(x: Float, y: Float) {
        if (state == State.DRAG) {
            translation.add(x - touch.x, y - touch.y)
            touch.set(x, y)
        }
    }

    private fun onTouchEnd() {
        if (gridEnabled) overlayView.showGrid = false
        state = State.IDLE
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
                ?.let {
                    val width = it[0].toFloat()
                    val height = it.getOrNull(1)
                        ?.toFloat() ?: 1f

                    width / height
                } ?: 1f
        } catch (e: NumberFormatException) {
            1f
        }
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            state = State.SCALE
            return true
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector?.scaleFactor?.let {
                scale = (scale * it).coerceIn(minScale, maxScale)
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            state = State.IDLE
        }
    }
}