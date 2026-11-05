package uz.pleyer.premium

import android.media.audiofx.Visualizer
import androidx.compose.runtime.mutableStateListOf

object AudioViz {
    val levels = mutableStateListOf<Float>().apply { repeat(48) { add(0.04f) } }
    private var effect: Visualizer? = null

    @Synchronized fun attach(sessionId: Int) {
        release()
        if (sessionId <= 0) return
        try {
            val v = Visualizer(sessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {
                    val count = levels.size
                    for (i in 0 until count) {
                        val start = i * waveform.size / count
                        val end = ((i + 1) * waveform.size / count).coerceAtLeast(start + 1)
                        var peak = 0
                        for (j in start until end.coerceAtMost(waveform.size)) {
                            peak = maxOf(peak, kotlin.math.abs(waveform[j].toInt()))
                        }
                        levels[i] = (peak / 128f).coerceIn(0.04f, 1f)
                    }
                }
                override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) = Unit
            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            v.enabled = true
            effect = v
        } catch (_: Exception) {
            release()
        }
    }

    @Synchronized fun release() {
        try { effect?.enabled = false; effect?.release() } catch (_: Exception) { }
        effect = null
        for (i in levels.indices) levels[i] = 0.04f
    }
}
