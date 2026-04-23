package com.tidelet.app.ui.sos

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

/**
 * Thin wrapper around [MediaPlayer] for looping ambient audio.
 *
 * Usage:
 *   - one instance per screen-lifetime (created lazily in the ViewModel)
 *   - call [play] to switch to a new soundscape (stops the old one first)
 *   - call [play] with [Soundscape.Silent] to stop all audio
 *   - call [release] in the ViewModel's onCleared to free native resources
 *
 * Uses [AudioAttributes.USAGE_MEDIA] with [AudioAttributes.CONTENT_TYPE_MUSIC] —
 * ambient sound that shouldn't duck other media, respects the user's media
 * volume, and plays through the normal media stream rather than the call stream.
 */
class SoundscapePlayer(private val appContext: Context) {

    private var player: MediaPlayer? = null
    private var current: Soundscape = Soundscape.Silent

    fun play(soundscape: Soundscape) {
        if (soundscape == current && player != null) return
        stopInternal()
        current = soundscape

        val rawId = soundscape.rawResId ?: return

        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                // Gentle default — loud enough to hear, quiet enough to layer with
                // the user's own music if they have something playing.
                setVolume(0.6f, 0.6f)

                appContext.resources.openRawResourceFd(rawId).use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                setOnPreparedListener { it.start() }
                setOnErrorListener { mp, what, extra ->
                    Log.w(TAG, "MediaPlayer error what=$what extra=$extra")
                    stopInternal()
                    true
                }
                prepareAsync()
            }
        } catch (t: Throwable) {
            // Missing raw resource, format unsupported, etc. Swallow — the screen
            // remains usable in silent mode.
            Log.w(TAG, "Failed to start soundscape $soundscape", t)
            stopInternal()
        }
    }

    fun release() {
        stopInternal()
        current = Soundscape.Silent
    }

    private fun stopInternal() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: IllegalStateException) {
                // already stopped / never prepared — fine
            }
            it.release()
        }
        player = null
    }

    companion object {
        private const val TAG = "SoundscapePlayer"
    }
}
