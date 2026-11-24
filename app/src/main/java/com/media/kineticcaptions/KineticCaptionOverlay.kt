package com.media.kineticcaptions

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

class KineticCaptionOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val segments = mutableListOf<CaptionSegment>()
    var currentTimeMs: Long = 0
        set(value) {
            field = value
            invalidate()
        }

    var activeStyle: CaptionStyle = CaptionStyle.POP_BOUNCE
        set(value) {
            field = value
            invalidate()
        }

    // Paint setup
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = 64f
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFE600")
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF0055")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        textAlign = Paint.Align.CENTER
        textSize = 64f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val bgGradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setSegments(newSegments: List<CaptionSegment>) {
        segments.clear()
        segments.addAll(newSegments)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. Draw synthetic dynamic video background gradient loop
        drawDynamicBackdrop(canvas, w, h)

        // 2. Find currently active segment
        val segment = segments.firstOrNull { it.containsTime(currentTimeMs) } ?: return

        when (activeStyle) {
            CaptionStyle.POP_BOUNCE -> drawPopBounce(canvas, segment, w, h)
            CaptionStyle.KARAOKE_PILL -> drawKaraokePill(canvas, segment, w, h)
            CaptionStyle.TYPEWRITER -> drawTypewriter(canvas, segment, w, h)
            CaptionStyle.NEON_PULSE -> drawNeonPulse(canvas, segment, w, h)
        }
    }

    private fun drawDynamicBackdrop(canvas: Canvas, w: Float, h: Float) {
        val cycle = ((currentTimeMs % 6000).toFloat() / 6000f) * 2f * PI.toFloat()
        val shift = sin(cycle) * 0.2f

        val colors = intArrayOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#1E1B4B"),
            Color.parseColor("#090D16")
        )
        val shader = LinearGradient(
            0f, 0f, w, h * (1f + shift),
            colors, null, Shader.TileMode.CLAMP
        )
        bgGradientPaint.shader = shader
        canvas.drawRect(0f, 0f, w, h, bgGradientPaint)
    }

    private fun drawPopBounce(canvas: Canvas, segment: CaptionSegment, w: Float, h: Float) {
        val activeIdx = segment.getActiveWordIndex(currentTimeMs)
        val progress = segment.getWordProgress(currentTimeMs, activeIdx)
        val scale = 1.0f + 0.35f * sin(progress * PI.toFloat())

        val words = segment.words
        val totalWords = words.size
        val centerY = h * 0.55f

        // Render each word
        val fullText = words.joinToString(" ")
        textPaint.textSize = 58f
        val totalWidth = textPaint.measureText(fullText)
        var startX = (w - totalWidth) / 2f

        for (i in 0 until totalWords) {
            val word = words[i]
            val wordWidth = textPaint.measureText(word)
            val wordCenterX = startX + wordWidth / 2f

            canvas.save()
            if (i == activeIdx) {
                canvas.scale(scale, scale, wordCenterX, centerY)
                textPaint.color = Color.parseColor("#FFE600")
                textPaint.setShadowLayer(16f, 0f, 0f, Color.parseColor("#FFE600"))
            } else {
                textPaint.color = Color.argb(190, 255, 255, 255)
                textPaint.clearShadowLayer()
            }

            canvas.drawText(word, wordCenterX, centerY, textPaint)
            canvas.restore()

            val spaceWidth = textPaint.measureText(" ")
            startX += wordWidth + spaceWidth
        }
    }

    private fun drawKaraokePill(canvas: Canvas, segment: CaptionSegment, w: Float, h: Float) {
        val activeIdx = segment.getActiveWordIndex(currentTimeMs)
        val words = segment.words
        val centerY = h * 0.55f

        textPaint.textSize = 54f
        val fullText = words.joinToString(" ")
        val totalWidth = textPaint.measureText(fullText)
        var startX = (w - totalWidth) / 2f

        for (i in words.indices) {
            val word = words[i]
            val wordWidth = textPaint.measureText(word)
            val wordCenterX = startX + wordWidth / 2f

            if (i == activeIdx) {
                // Draw vibrant pill behind active word
                highlightPaint.color = Color.parseColor("#00F0FF")
                val rect = RectF(
                    startX - 10f, centerY - 50f,
                    startX + wordWidth + 10f, centerY + 16f
                )
                canvas.drawRoundRect(rect, 16f, 16f, highlightPaint)

                // Dark text inside pill
                textPaint.color = Color.BLACK
                textPaint.clearShadowLayer()
            } else {
                textPaint.color = Color.WHITE
                textPaint.setShadowLayer(8f, 0f, 0f, Color.BLACK)
            }

            canvas.drawText(word, wordCenterX, centerY, textPaint)

            val spaceWidth = textPaint.measureText(" ")
            startX += wordWidth + spaceWidth
        }
    }

    private fun drawTypewriter(canvas: Canvas, segment: CaptionSegment, w: Float, h: Float) {
        val duration = (segment.endMs - segment.startMs).coerceAtLeast(1)
        val elapsed = (currentTimeMs - segment.startMs).coerceIn(0, duration)
        val ratio = elapsed.toFloat() / duration

        val fullText = segment.text
        val charsToShow = (ratio * fullText.length).toInt().coerceIn(0, fullText.length)
        val sub = fullText.substring(0, charsToShow) + if ((currentTimeMs / 300) % 2L == 0L) "|" else ""

        textPaint.textSize = 54f
        textPaint.color = Color.parseColor("#FFE600")
        textPaint.setShadowLayer(12f, 0f, 0f, Color.parseColor("#FF9900"))

        val centerY = h * 0.55f
        canvas.drawText(sub, w / 2f, centerY, textPaint)
    }

    private fun drawNeonPulse(canvas: Canvas, segment: CaptionSegment, w: Float, h: Float) {
        val activeIdx = segment.getActiveWordIndex(currentTimeMs)
        val progress = segment.getWordProgress(currentTimeMs, activeIdx)
        val pulse = sin(progress * PI.toFloat() * 2f) * 6f

        val centerY = h * 0.55f
        val text = segment.text

        // Outer Neon Glow Stroke
        strokePaint.textSize = 56f
        strokePaint.strokeWidth = 12f + pulse
        strokePaint.setShadowLayer(24f, 0f, 0f, Color.parseColor("#FF0055"))
        canvas.drawText(text, w / 2f, centerY, strokePaint)

        // Inner White Fill
        textPaint.textSize = 56f
        textPaint.color = Color.WHITE
        textPaint.clearShadowLayer()
        canvas.drawText(text, w / 2f, centerY, textPaint)
    }
}
