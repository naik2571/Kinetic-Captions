package com.media.kineticcaptions

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.media.kineticcaptions.databinding.ActivityMainBinding
import com.media.kineticcaptions.databinding.ItemCaptionSegmentBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CaptionAdapter

    private var isPlaying = false
    private var currentTimeMs: Long = 0
    private val totalDurationMs: Long = 8500
    private val handler = Handler(Looper.getMainLooper())

    private val presetHook = listOf(
        CaptionSegment(0, 1200, "STOP SCROLLING"),
        CaptionSegment(1200, 2400, "THIS APP GENERATES"),
        CaptionSegment(2400, 3800, "VIRAL KINETIC CAPTIONS"),
        CaptionSegment(3800, 5200, "COMPLETELY OFFLINE"),
        CaptionSegment(5200, 6800, "100% ON YOUR PHONE"),
        CaptionSegment(6800, 8500, "WITHOUT ANY CLOUD")
    )

    private val presetTech = listOf(
        CaptionSegment(0, 1400, "NEXT GEN ARCHITECTURE"),
        CaptionSegment(1400, 2800, "ZERO CLOUD SERVERS"),
        CaptionSegment(2800, 4400, "128-DIM LOCAL EMBEDDINGS"),
        CaptionSegment(4400, 6000, "HARDWARE ACCELERATED"),
        CaptionSegment(6000, 8500, "ULTRA FAST & PRIVATE")
    )

    private var activeSegments = presetHook

    private val playbackRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                currentTimeMs += 33 // ~30 FPS loop
                if (currentTimeMs >= totalDurationMs) {
                    currentTimeMs = 0
                }
                updatePlaybackUi()
                handler.postDelayed(this, 33)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        loadPreset(presetHook)
        startPlayback()
    }

    private fun setupRecyclerView() {
        adapter = CaptionAdapter { segment ->
            currentTimeMs = segment.startMs
            updatePlaybackUi()
        }
        binding.recyclerCaptions.layoutManager = LinearLayoutManager(this)
        binding.recyclerCaptions.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) pausePlayback() else startPlayback()
        }

        binding.seekBarTimeline.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentTimeMs = progress.toLong()
                    updatePlaybackUi()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Style Chips
        binding.chipPopBounce.setOnClickListener { binding.captionOverlay.activeStyle = CaptionStyle.POP_BOUNCE }
        binding.chipKaraoke.setOnClickListener { binding.captionOverlay.activeStyle = CaptionStyle.KARAOKE_PILL }
        binding.chipTypewriter.setOnClickListener { binding.captionOverlay.activeStyle = CaptionStyle.TYPEWRITER }
        binding.chipNeonPulse.setOnClickListener { binding.captionOverlay.activeStyle = CaptionStyle.NEON_PULSE }

        // Presets
        binding.btnPresetHook.setOnClickListener { loadPreset(presetHook) }
        binding.btnPresetTech.setOnClickListener { loadPreset(presetTech) }

        binding.btnImportVideo.setOnClickListener {
            // Demo notice
            binding.btnImportVideo.text = "🎬 Synthetic MP4 Active"
        }
    }

    private fun loadPreset(segments: List<CaptionSegment>) {
        activeSegments = segments
        binding.captionOverlay.setSegments(segments)
        adapter.submitList(segments)
        currentTimeMs = 0
        updatePlaybackUi()
    }

    private fun startPlayback() {
        isPlaying = true
        binding.btnPlayPause.text = "⏸"
        handler.post(playbackRunnable)
    }

    private fun pausePlayback() {
        isPlaying = false
        binding.btnPlayPause.text = "▶"
        handler.removeCallbacks(playbackRunnable)
    }

    private fun updatePlaybackUi() {
        binding.captionOverlay.currentTimeMs = currentTimeMs
        binding.seekBarTimeline.progress = currentTimeMs.toInt()

        val curSec = currentTimeMs / 1000f
        val totSec = totalDurationMs / 1000f
        binding.tvTimecode.text = String.format("%04.1fs / %04.1fs", curSec, totSec)

        adapter.setActiveTime(currentTimeMs)
    }

    override fun onDestroy() {
        pausePlayback()
        super.onDestroy()
    }

    class CaptionAdapter(private val onSelect: (CaptionSegment) -> Unit) :
        RecyclerView.Adapter<CaptionAdapter.Holder>() {

        private var list = listOf<CaptionSegment>()
        private var activeTimeMs: Long = 0

        fun submitList(newList: List<CaptionSegment>) {
            list = newList
            notifyDataSetChanged()
        }

        fun setActiveTime(timeMs: Long) {
            activeTimeMs = timeMs
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemCaptionSegmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(list[position], activeTimeMs, onSelect)
        }

        override fun getItemCount(): Int = list.size

        class Holder(private val binding: ItemCaptionSegmentBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: CaptionSegment, timeMs: Long, onSelect: (CaptionSegment) -> Unit) {
                val start = item.startMs / 1000f
                val end = item.endMs / 1000f
                binding.tvTiming.text = String.format("%04.1fs - %04.1fs", start, end)
                binding.tvText.text = item.text

                val isActive = item.containsTime(timeMs)
                if (isActive) {
                    binding.tvActiveBadge.visibility = View.VISIBLE
                    binding.root.strokeColor = 0xFFFFE600.toInt()
                } else {
                    binding.tvActiveBadge.visibility = View.GONE
                    binding.root.strokeColor = 0xFF2A2A3C.toInt()
                }

                binding.root.setOnClickListener { onSelect(item) }
            }
        }
    }
}
