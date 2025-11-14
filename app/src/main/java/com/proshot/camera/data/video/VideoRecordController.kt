package com.proshot.camera.data.video

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Professional video recording controller
 * Supports 4K, manual controls, and pro-level features
 */
@Singleton
class VideoRecordController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    private var mediaRecorder: MediaRecorder? = null
    private var currentVideoFile: File? = null

    private val _recordingState = MutableStateFlow(VideoRecordingState())
    val recordingState: StateFlow<VideoRecordingState> = _recordingState.asStateFlow()

    /**
     * Start video recording
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun startRecording(settings: VideoSettings = VideoSettings()) {
        if (_recordingState.value.isRecording) {
            Timber.w("Already recording")
            return
        }

        try {
            // Create output file
            currentVideoFile = createVideoFile(settings.resolution)

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                // Audio source
                setAudioSource(MediaRecorder.AudioSource.MIC)

                // Video source
                setVideoSource(MediaRecorder.VideoSource.SURFACE)

                // Output format
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                // Video encoder
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)

                // Audio encoder
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                // Resolution and framerate
                val (width, height) = getResolution(settings.resolution)
                setVideoSize(width, height)
                setVideoFrameRate(settings.framerate)

                // Bitrate (higher = better quality)
                val bitrate = calculateBitrate(settings.resolution, settings.quality)
                setVideoEncodingBitRate(bitrate)

                // Audio settings
                setAudioEncodingBitRate(128_000)  // 128 kbps
                setAudioSamplingRate(48000)       // 48 kHz

                // Output file
                setOutputFile(currentVideoFile!!.absolutePath)

                // Prepare
                prepare()
            }

            // Start recording
            mediaRecorder?.start()

            val startTime = SystemClock.elapsedRealtime()

            _recordingState.value = VideoRecordingState(
                isRecording = true,
                isPaused = false,
                startTime = startTime,
                elapsedTime = 0L,
                fileSize = 0L,
                resolution = settings.resolution,
                framerate = settings.framerate
            )

            // Start timer
            timerJob = scope.launch {
                while (isActive && _recordingState.value.isRecording) {
                    val elapsed = SystemClock.elapsedRealtime() - startTime
                    val fileSize = currentVideoFile?.length() ?: 0L

                    _recordingState.value = _recordingState.value.copy(
                        elapsedTime = elapsed,
                        fileSize = fileSize
                    )

                    delay(100)  // Update every 100ms
                }
            }

            Timber.d("Video recording started: ${currentVideoFile!!.absolutePath}")
            Timber.d("Settings: ${settings.resolution}, ${settings.framerate}fps, ${settings.quality}")

        } catch (e: Exception) {
            Timber.e(e, "Failed to start video recording")
            stopRecording()
            _recordingState.value = VideoRecordingState()
        }
    }

    /**
     * Stop video recording
     */
    fun stopRecording(): File? {
        timerJob?.cancel()
        timerJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val videoFile = currentVideoFile

            _recordingState.value = VideoRecordingState(
                isRecording = false,
                isPaused = false
            )

            Timber.d("Video recording stopped: ${videoFile?.absolutePath}")

            return videoFile

        } catch (e: Exception) {
            Timber.e(e, "Error stopping video recording")
            return null
        }
    }

    /**
     * Pause recording (API 24+)
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun pauseRecording() {
        if (!_recordingState.value.isRecording || _recordingState.value.isPaused) {
            return
        }

        try {
            mediaRecorder?.pause()
            _recordingState.value = _recordingState.value.copy(isPaused = true)
            Timber.d("Video recording paused")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pause recording")
        }
    }

    /**
     * Resume recording (API 24+)
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun resumeRecording() {
        if (!_recordingState.value.isRecording || !_recordingState.value.isPaused) {
            return
        }

        try {
            mediaRecorder?.resume()
            _recordingState.value = _recordingState.value.copy(isPaused = false)
            Timber.d("Video recording resumed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to resume recording")
        }
    }

    /**
     * Get resolution dimensions
     */
    private fun getResolution(resolution: VideoResolution): Pair<Int, Int> {
        return when (resolution) {
            VideoResolution.UHD_4K -> 3840 to 2160
            VideoResolution.QHD_2K -> 2560 to 1440
            VideoResolution.FHD_1080P -> 1920 to 1080
            VideoResolution.HD_720P -> 1280 to 720
            VideoResolution.SD_480P -> 854 to 480
        }
    }

    /**
     * Calculate bitrate based on resolution and quality
     */
    private fun calculateBitrate(resolution: VideoResolution, quality: VideoQuality): Int {
        val baseBitrate = when (resolution) {
            VideoResolution.UHD_4K -> 60_000_000    // 60 Mbps
            VideoResolution.QHD_2K -> 30_000_000    // 30 Mbps
            VideoResolution.FHD_1080P -> 20_000_000 // 20 Mbps
            VideoResolution.HD_720P -> 12_000_000   // 12 Mbps
            VideoResolution.SD_480P -> 6_000_000    // 6 Mbps
        }

        val qualityMultiplier = when (quality) {
            VideoQuality.HIGH -> 1.5f
            VideoQuality.MEDIUM -> 1.0f
            VideoQuality.LOW -> 0.6f
        }

        return (baseBitrate * qualityMultiplier).toInt()
    }

    /**
     * Create video file
     */
    private fun createVideoFile(resolution: VideoResolution): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "ProShot_Video_${resolution.name}_${timeStamp}.mp4"

        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "ProShot/Videos"
        )

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, fileName)
    }

    /**
     * Release resources
     */
    fun release() {
        stopRecording()
        timerJob?.cancel()
    }
}

/**
 * Video recording state
 */
data class VideoRecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val startTime: Long = 0L,
    val elapsedTime: Long = 0L,
    val fileSize: Long = 0L,
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val framerate: Int = 30
) {
    /**
     * Formatted duration (MM:SS)
     */
    val formattedDuration: String
        get() {
            val totalSeconds = elapsedTime / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    /**
     * Formatted file size (MB)
     */
    val formattedFileSize: String
        get() {
            val mb = fileSize / (1024.0 * 1024.0)
            return String.format("%.1f MB", mb)
        }

    /**
     * Estimated bitrate (Mbps)
     */
    val estimatedBitrate: Float
        get() {
            if (elapsedTime == 0L) return 0f
            val seconds = elapsedTime / 1000.0
            val bits = fileSize * 8.0
            return (bits / seconds / 1_000_000).toFloat()  // Convert to Mbps
        }
}

/**
 * Video resolution options
 */
enum class VideoResolution(val displayName: String) {
    UHD_4K("4K UHD (3840×2160)"),
    QHD_2K("2K QHD (2560×1440)"),
    FHD_1080P("1080p FHD (1920×1080)"),
    HD_720P("720p HD (1280×720)"),
    SD_480P("480p SD (854×480)")
}

/**
 * Video quality settings
 */
enum class VideoQuality {
    HIGH,      // 1.5x bitrate
    MEDIUM,    // 1.0x bitrate
    LOW        // 0.6x bitrate
}

/**
 * Video settings
 */
data class VideoSettings(
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val framerate: Int = 30,  // 24, 30, 60, 120, 240
    val quality: VideoQuality = VideoQuality.HIGH,
    val stabilization: Boolean = true,
    val audioEnabled: Boolean = true
)
