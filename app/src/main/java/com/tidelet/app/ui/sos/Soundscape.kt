package com.tidelet.app.ui.sos

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.tidelet.app.R

/**
 * Ambient sounds offered on the Ride the Wave screen.
 *
 * [rawResId] is null for [Silent]; any non-null value must point at an audio
 * file in `res/raw/` that MediaPlayer can loop (we set isLooping = true at the
 * call site). The extension is irrelevant to the resource lookup — the files
 * are currently MP3 (Pixabay, CC0 / Pixabay Content License).
 *
 * Sources (see `res/raw/CREDITS.md` for the full record):
 *   - ocean.mp3 — Pixabay: "Ocean Waves" by SolarMusic (112906)
 *   - river.mp3 — Pixabay: "Birds Singing Calm River Nature Ambient Sound"
 *                 by SoundsForYou (127411)
 *   - rain.mp3  — Pixabay: "Calming Rain Loop" by Dragon Studio (398653)
 */
enum class Soundscape(
    @StringRes val labelRes: Int,
    @RawRes val rawResId: Int?,
) {
    Silent(R.string.soundscape_silent, null),
    Ocean(R.string.soundscape_ocean, R.raw.ocean),
    River(R.string.soundscape_river, R.raw.river),
    Rain(R.string.soundscape_rain, R.raw.rain),
}
