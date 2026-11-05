package uz.pleyer.premium

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Captures FFT data from the active playback session (not the microphone). */
object AudioViz {
    private val _levels = MutableStateFlow(List(48) { 0.035f })
    val levels: StateFlow<List<Float>> = _levels.asStateFlow()
    private var effect: Visualizer? = null

    @Synchronized fun attach(sessionId: Int) {
        release()
        if (sessionId <= 0) return
        try {
            val v = Visualizer(sessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) = Unit
                override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
                    val bins = (fft.size / 2).coerceAtLeast(2)
                    val next = ArrayList<Float>(48)
                    for (i in 0 until 48) {
                        val low = (2.0 * Math.pow((bins - 1).toDouble(), i.toDouble() / 48)).toInt().coerceIn(1, bins - 1)
                        val high = (2.0 * Math.pow((bins - 1).toDouble(), (i + 1).toDouble() / 48)).toInt().coerceIn(low + 1, bins)
                        var peak = 0.0
                        for (bin in low until high) {
                            val re = fft[2 * bin].toInt()
                            val im = fft[2 * bin + 1].toInt()
                            peak = maxOf(peak, kotlin.math.sqrt((re * re + im * im).toDouble()))
                        }
                        next.add((0.035f + (peak / 48.0).toFloat().coerceIn(0f, 1f) * 0.965f).coerceIn(0.035f, 1f))
                    }
                    _levels.value = next
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true)
            v.enabled = true
            effect = v
        } catch (_: Exception) { release() }
    }

    @Synchronized fun release() {
        try { effect?.enabled = false; effect?.release() } catch (_: Exception) { }
        effect = null
        _levels.value = List(48) { 0.035f }
    }
}