package com.media.kineticcaptions

enum class CaptionStyle {
    POP_BOUNCE,
    KARAOKE_PILL,
    TYPEWRITER,
    NEON_PULSE
}

data class CaptionSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
) {
    val words: List<String> = text.split(Regex("\\s+")).filter { it.isNotBlank() }

    fun containsTime(timeMs: Long): Boolean = timeMs in startMs..endMs

    fun getActiveWordIndex(timeMs: Long): Int {
        if (words.isEmpty() || !containsTime(timeMs)) return -1
        val duration = (endMs - startMs).coerceAtLeast(1)
        val elapsed = (timeMs - startMs).coerceIn(0, duration)
        val progress = elapsed.toFloat() / duration
        val idx = (progress * words.size).toInt()
        return idx.coerceIn(0, words.size - 1)
    }

    fun getWordProgress(timeMs: Long, wordIdx: Int): Float {
        if (words.isEmpty()) return 0f
        val duration = (endMs - startMs).coerceAtLeast(1)
        val wordDuration = duration.toFloat() / words.size
        val wordStart = startMs + (wordIdx * wordDuration)
        val elapsed = (timeMs - wordStart).coerceIn(0f, wordDuration)
        return (elapsed / wordDuration).coerceIn(0f, 1f)
    }
}
