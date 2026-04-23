# Tidelet

A quiet Android companion for the moments between drinks. Phase 1 ships a craving SOS toolkit and a sobriety day counter — all on-device, no account, no network.

## Status

Phase 1 scaffold. Everything compiles and runs end-to-end: onboarding → home with day counter → SOS flow with four tools:

- **Ride the wave** — 15-min urge-surfing timer with an animated wave and a choice of ambient soundscapes (Ocean / River / Rain / Silent).
- **Breathe** — 4-4-4-4 box breathing circle with cycle count.
- **My reasons** — the user's own "why I'm doing this" list, read in the hard moments, written in the calm ones. Backed by Room.
- **Do something else** — a curated list of 15 quick alternatives, plus a "Pick one for me" shuffle button.

Log / Stats / Settings tabs are deliberate placeholders for Phase 2.

## Requirements

- **Android Studio** Ladybug (2024.2) or newer — earlier versions won't understand Kotlin 2.1 / AGP 8.7.
- **JDK 17** (Android Studio bundles it; just point File → Project Structure → SDK Location → Gradle JDK at the bundled one).
- **Android SDK 35** (the IDE will offer to install it on first sync).
- Emulator or device running **Android 8.0 (API 26) or higher**.

## First-time setup

1. **Open the project.** `File → Open…` and pick the `Tidelet/` folder (the one with `settings.gradle.kts`). Let Android Studio do its initial indexing.

2. **Generate the Gradle wrapper jar.** The repo ships `gradle/wrapper/gradle-wrapper.properties` but not the `gradle-wrapper.jar` binary. Before the first Gradle sync, open the Android Studio terminal and run:

   ```
   gradle wrapper --gradle-version 8.11.1
   ```

   If you don't have a system `gradle` on your PATH, the easiest alternative is to let Android Studio do it: close the project, run `File → New → Import Project…` and point it at the folder — the IDE will scaffold the wrapper. (If the IDE instead uses its bundled Gradle, that's fine too; the wrapper is a convenience, not a hard requirement for IDE builds.)

3. **Sync Gradle.** Accept the prompt when it appears, or `File → Sync Project with Gradle Files`. First sync will download AGP, Kotlin, Compose BOM, Room, and friends — give it a few minutes.

4. **Run.** Pick an emulator (or plug in a device with USB debugging on), hit the green ▶. The app launches straight into onboarding on first run.

## Code map

```
Tidelet/
├─ settings.gradle.kts              # Version catalog + :app module
├─ build.gradle.kts                 # Root build (plugin versions only)
├─ gradle/libs.versions.toml        # Single source of truth for all library versions
└─ app/
   ├─ build.gradle.kts              # applicationId com.tidelet.app
   └─ src/main/
      ├─ AndroidManifest.xml        # Single activity, no runtime permissions
      ├─ res/
      │  ├─ values/                 # strings, arrays, colors, themes
      │  ├─ raw/                    # ocean.mp3, river.mp3, rain.mp3 (looping ambience, Pixabay CC0)
      │  └─ …                       # launcher icons, backup rules
      └─ java/com/tidelet/app/
         ├─ TideletApplication.kt   # Manual DI — exposes .repository
         ├─ MainActivity.kt         # Onboarded? → TideletApp() (Scaffold + NavHost)
         ├─ data/
         │  ├─ db/                  # Room: CheckIn, CravingEvent, Reason, DAOs, database
         │  ├─ prefs/               # DataStore Preferences — UserProfile
         │  └─ repo/                # TideletRepository — the one thing UI talks to
         ├─ util/Time.kt            # streakFromStartDate + milestone helpers
         └─ ui/
            ├─ theme/               # Material 3 + TideletExtendedColors (SOS warm tones)
            ├─ nav/                 # Routes object — all nav destinations
            ├─ onboarding/          # 3-step flow, writes to DataStore
            ├─ home/                # Day counter + next-milestone progress + SOS button
            ├─ sos/                 # SosScreen grid + RideTheWave + Breathe + Reasons + Distractions
            ├─ log/                 # Phase 2 stub
            ├─ stats/                # Phase 2 stub
            └─ settings/             # Phase 2 stub
```

## Design decisions worth knowing

- **No Hilt.** Manual DI through `TideletApplication.repository` — fewer moving parts while you're learning. If complexity grows we can add Hilt later without rewriting screens.
- **Room with `fallbackToDestructiveMigration()`.** Schema churn is fine in Phase 1; we'll add real migrations once we have users. Don't ship this flag. DB version is currently 2 (bumped when we added the `Reason` entity).
- **No network permission.** `AndroidManifest.xml` requests nothing. Data is on-device by design, and `allowBackup=false` keeps it out of Google cloud backups.
- **Compassionate reset.** If the user slips, "I drank" just logs a `CravingEvent` with outcome `DRANK` — it doesn't punish or zero out their history. Reset is a deliberate action in Settings (Phase 2).
- **Extended color tokens.** `TideletTheme.extended.sosSurface` / `sosAccent` give the SOS flow a warmer palette than the rest of the app. Access via `CompositionLocal` — see `ui/theme/Theme.kt`.
- **Ambient audio is real field recordings, not synthesis.** Ocean / River / Rain loops in `app/src/main/res/raw/` are MP3s sourced from Pixabay under the [Pixabay Content License](https://pixabay.com/service/license-summary/) (CC0-equivalent: royalty-free commercial use, no attribution required). Attributions are recorded anyway in `app/src/main/assets/soundscape_credits.md`. An earlier version of this app shipped procedurally-generated noise loops; they sounded synthetic enough to break the "calm" framing of Ride the Wave, so we swapped them for real recordings. `MediaPlayer.setLooping(true)` handles loop continuity — the tracks aren't hand-crossfaded, but they're long enough (30s–3min) that the seam is inaudible during a 15-minute wave.
- **No crisis/help screen.** The earlier version shipped a "Call for help" tile with 988 / SAMHSA / 911. It has been removed by choice for this build.

## Running the happy path

1. Launch → onboarding → pick "Today" → finish.
2. Home shows "0 days" with hours/minutes ticking up.
3. Tap **I'm having a craving** → SOS grid (four tiles).
4. **Ride the wave** → 15-min timer with animated wave, soundscape picker (Ocean / River / Rain / Silent), rotating calm lines → after timer, "how did that go?" outcome question logs a `CravingEvent`.
5. **Breathe** → 4-4-4-4 box breathing circle with cycle count.
6. **My reasons** → add a reason via the composer; existing reasons render as cards; on dispose we log a `CravingEvent` if any were read.
7. **Do something else** → curated list of 15 alternatives, shuffle button picks one and scrolls it into view, "I did it" / "Nothing worked" logs an outcome.

## Phase 2 backlog (not yet built)

- Log tab: daily check-in form, retroactive edits to start date.
- Stats tab: milestones, money saved, hours reclaimed, craving-pattern insights.
- Settings tab: compassionate reset, check-in reminder time, My Reasons + Distractions editors, data export/wipe.
- Notifications via WorkManager (dep is already in the catalog).

## Troubleshooting

- **"SDK 35 not installed"** — SDK Manager → Android SDK → install "Android 15.0 (API 35)".
- **"JDK 17 required"** — Project Structure → SDK Location → Gradle JDK → pick the JBR 17 bundled with Android Studio.
- **Gradle sync fails on wrapper jar** — see step 2 above; run `gradle wrapper --gradle-version 8.11.1` once.
- **KSP complains about Room** — stop the Gradle daemon (`./gradlew --stop`) and re-sync; stale cache issue.
