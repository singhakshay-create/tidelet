# Tidelet — Code Review (2026-06-10)

Reviewed: full `app/src/main` tree (~14.5k lines Kotlin), manifest, Gradle config, resources, test layout. Working tree has 38 modified/untracked files on top of the single `Initial import: Tidelet v1` commit.

**Overall:** the codebase is in very good shape. Architecture is clean (single Activity, manual DI, repository over Room + DataStore, pure-Kotlin export codec), comments explain *why*, the Phase 2 spec items I spot-checked (Breathe `Animatable` fix, `imePadding` on all long-form screens, `verifyBackup`, grace language, widget) are implemented. Nothing here is a rewrite — the items below are targeted fixes.

Each item is written to be independently actionable by a coding agent: **Where → Problem → Fix → Acceptance.** Work top-down; P0 items block the Play Store launch.

---

## P0 — Launch blockers

### P0.1 Release build is not minified and has no signing config

**Where:** `app/build.gradle.kts` → `buildTypes.release`

**Problem:** `isMinifyEnabled = false`, no `shrinkResources`, and no `signingConfigs` block. The launch plan (§2.1, §1.3) requires a minified, signed AAB. `proguard-rules.pro` is empty, so enabling minification without keep rules will break Room/Compose reflection paths silently.

**Fix:**
1. Add to `release`: `isMinifyEnabled = true`, `isShrinkResources = true`.
2. Populate `proguard-rules.pro`: keep rules for Room entities/DAOs (`-keep class com.tidelet.app.data.db.** { *; }` or narrower `@Entity`-targeted rules), kotlinx-coroutines debug probes exclusion, and `-keepattributes *Annotation*`.
3. Add a `signingConfigs.release` block that reads `TIDELET_KEYSTORE_PATH` / `TIDELET_KEYSTORE_PASSWORD` / `TIDELET_KEY_ALIAS` / `TIDELET_KEY_PASSWORD` from `local.properties` or env vars, and only applies it when the properties are present (so CI/dev builds without the keystore still work). Never commit the keystore (`.gitignore` already covers `*.jks` indirectly via no rule — **add `*.jks` and `*.keystore` explicitly**).

**Acceptance:** `./gradlew :app:bundleRelease` produces a signed AAB when keystore props are set and an unsigned one otherwise; install the minified release build and walk every screen (onboarding → SOS tools → check-in → journal read screens → settings export/import/verify → widget) with no crashes.

### P0.2 Room schema export is configured but not wired — no `schemas/` directory

**Where:** `app/build.gradle.kts` (KSP config), `TideletDatabase.kt` (`exportSchema = true`)

**Problem:** `exportSchema = true` but there's no `room.schemaLocation` KSP arg and no `app/schemas/` directory. Schema history is how migrations get tested; without it, `MigrationTestHelper` can't work and the build emits a warning. The DB is already at version 10 with one real migration (`MIGRATION_9_10`).

**Fix:**
1. In `app/build.gradle.kts`: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
2. Build once to generate `app/schemas/com.tidelet.app.data.db.TideletDatabase/10.json`; commit it.
3. Add an instrumented `MigrationTest` using `androidx.room:room-testing`'s `MigrationTestHelper` covering 9→10 (insert a `craving_event` row pre-migration, migrate, assert `intensity` is NULL and the row survives).

**Acceptance:** `app/schemas/` exists and is committed; migration test passes in `connectedAndroidTest`.

### P0.3 `fallbackToDestructiveMigration()` will silently wipe user data

**Where:** `TideletDatabase.kt:75`

**Problem:** For this app, local data IS the product — there's no cloud copy. If a future version bump ships without a migration, every user's journal/streak history is destroyed on update. "Safe because early-beta" stops being true at launch. (The no-arg overload is also deprecated in Room 2.7.)

**Fix:** Remove `fallbackToDestructiveMigration()` from the release path. Either delete it outright (preferred — a missed migration then crashes loudly in internal testing instead of wiping quietly in production), or gate it on `BuildConfig.DEBUG`. Requires P0.2 so all historical schemas are known.

**Acceptance:** Release builds throw `IllegalStateException` on a missing migration rather than wiping; debug behavior documented in the class KDoc.

### P0.4 Import is not atomic — partial writes possible

**Where:** `ExportImportManager.applyImport` (lines 80–148), `data/repo/TideletRepository.kt`

**Problem:** PHASE_2_SPEC §2 requires corrupt input to "fail cleanly without partial writes." Decode-before-write covers parse failures, but the write loop (`forEach { repo.importX(it) }`) is dozens of separate suspend calls. A crash, process death, or constraint violation mid-loop leaves a half-imported DB, and the DataStore writes (profile, tool counts) can't roll back with the Room writes at all.

**Fix:**
1. Add `suspend fun <T> runInDbTransaction(block: suspend () -> T): T` to `TideletRepository` (Room impl: `db.withTransaction(block)`; fake: just invoke `block`).
2. In `applyImport`, wrap all Room-backed imports (check-ins, craving events, reasons, journal entries, weekly reflections, thought records, functional analyses, refusal phrases, milestone letters, evening reviews, relapse plan) in that transaction.
3. Order the DataStore writes (profile restore, tool counts) **after** the transaction commits, so a failed DB import leaves prefs untouched.

**Acceptance:** New instrumented test: seed an import doc whose Nth row violates a constraint (or inject a repo whose Nth `importX` throws) → assert zero rows from the import landed and the profile is unchanged. Existing round-trip tests still green.

---

## P1 — Should fix before/at launch

### P1.1 Dead check-in reminder preferences — feature never shipped

**Where:** `UserPreferences.kt` (`CHECKIN_REMINDER_*` keys, `setCheckInReminder`), export codec profile fields, `SettingsViewModel` reset paths

**Problem:** Reminder hour/minute/enabled prefs exist, round-trip through export/import, and default to 20:00 — but no UI exposes them, no WorkManager reminder job exists, and the manifest has no `POST_NOTIFICATIONS`. Dead state that misleads future contributors (and the export file shows `checkin_reminder_enabled = false` to users).

**Fix (pick one, decision for Akshay):**
- **(a) Ship reminders in v1:** Settings row (time picker + toggle), `PeriodicWorkRequest`/`AlarmManager` daily notification, `POST_NOTIFICATIONS` runtime permission flow (API 33+), notification copy that is content-free ("Time for your check-in" — no alcohol references visible on a lock screen), deep-link to today's check-in.
- **(b) Cut for v1:** delete the three keys from `UserPreferences`, `UserProfile`, the export codec (keep parse-side tolerance for old keys), and `restoreProfile`'s parameters.

**Acceptance:** Either a working reminder behind the permission flow, or zero references to `checkin_reminder` in `app/src/main` (parse-side `intOrNull` tolerance may remain).

### P1.2 Widget refresh worker runs hourly forever, widget or not

**Where:** `TideletApplication.onCreate` → `StreakWidgetRefreshWorker.schedule(this)`

**Problem:** The periodic worker is enqueued unconditionally on every app start, even for the majority of users who never place the widget. That's 24 wakeups/day of pure waste — and "battery-respectful" is part of this app's quiet posture.

**Fix:**
1. In `StreakWidgetRefreshWorker.schedule`, no-op unless `AppWidgetManager.getInstance(context).getAppWidgetIds(StreakWidgetProvider.componentOf(context)).isNotEmpty()`.
2. Override `onEnabled` in `StreakWidgetProvider` → `schedule(context)`; override `onDisabled` → `WorkManager.cancelUniqueWork(UNIQUE_NAME)` (expose a `cancel(context)` companion fun).
3. Keep the `Application.onCreate` call (it now self-gates) so an existing widget survives app updates/reboots.

**Acceptance:** Fresh install without widget → `adb shell dumpsys jobscheduler` shows no tidelet periodic job. Place widget → job appears; remove widget → job gone. Unit test for the gate via a fake/`shadowOf(AppWidgetManager)` in Robolectric.

### P1.3 No `windowSoftInputMode` on MainActivity

**Where:** `AndroidManifest.xml` → `<activity android:name=".MainActivity">`

**Problem:** PHASE_2_SPEC §10.1 calls for confirming `adjustResize`. Screens use `Modifier.imePadding()`, which needs the window to propagate IME insets; on API 26–29 the default `adjustPan` can pan the window instead, leaving the keyboard-overlap bug alive on exactly the older devices in the test matrix.

**Fix:** Add `android:windowSoftInputMode="adjustResize"` to the activity element.

**Acceptance:** On an API 26–28 emulator, a long Journal entry keeps the caret visible above the keyboard.

### P1.4 Crisis-line disclaimer — verify it's actually reachable in-app

**Where:** `ui/resources/ResourcesScreen.kt` / `ResourceCatalog.kt` (untracked new files), onboarding flow

**Problem:** Play's health-content policy self-audit (launch plan §4.1) requires the crisis disclaimer to be *visible in-app*, not only in the store listing. A Resources screen exists but is reachable only via Settings. Someone in crisis shouldn't need to find Settings.

**Fix:** Add a quiet but persistent "Need more help? Crisis resources" entry point on the SOS hub screen (`SosScreen.kt`) — the screen a struggling user is most likely looking at — navigating to `Routes.RESOURCES`. Confirm `ResourceCatalog` numbers: use `tel:` URIs via `ACTION_DIAL` (no permission needed, matches the manifest comment), and include at least one India helpline and SAMHSA for US locales, keyed on `Locale.getDefault().country` with a generic international fallback.

**Acceptance:** From a craving SOS flow, crisis resources are ≤2 taps away; numbers verified current; manual TalkBack pass over the SOS → Resources path.

### P1.5 Bottom-bar tab never shows selected on child routes

**Where:** `MainActivity.kt:395` (`selected = currentRoute == item.route`)

**Problem:** Journal child routes (`JOURNAL_THOUGHT_CHECKS`, detail screens, etc.) deliberately keep the bottom bar visible, but exact-match comparison means no tab renders as selected there — the user appears to be "nowhere."

**Fix:** Compute selection from the back-stack destination hierarchy or a route-prefix map: e.g. give `BottomNavItem` a `fun owns(route: String?): Boolean` where Journal owns every `Routes.JOURNAL*` route, Log owns `LOG*`, etc. Use `selected = item.owns(currentRoute)`.

**Acceptance:** Compose UI test: navigate Home → Journal → Thought checks → detail; assert the Journal `NavigationBarItem` is selected on all three.

### P1.6 Two `!!` assertions in detail screens

**Where:** `ui/journal/JournalDetailScreen.kt:47`, `ui/journal/ThoughtCheckDetailScreen.kt:56`

**Problem:** `state.entry!!` / `state.record!!` in the `else` branch of the state `when`. If the ViewModel ever emits a state combination not covered by earlier branches (loading=false, error=null, entry=null — e.g. row deleted while the screen is open), this crashes in the one place the user is rereading something personal.

**Fix:** Restructure each `when` to bind non-null explicitly: `val entry = state.entry` then `entry != null -> DetailBody(entry, onBack)` with a final fallback branch rendering the existing empty/error state.

**Acceptance:** No `!!` in `app/src/main` (grep clean); unit test that an emitted state with null entry shows the fallback rather than crashing.

### P1.7 Working tree hygiene: 38 uncommitted files, stale spec, `tools:targetApi` drift

**Where:** repo root; `AndroidManifest.xml` (`tools:targetApi="34"`)

**Problem:** Everything since the initial commit — including all of Phase 2 — is uncommitted. A coding agent (or a disk failure) can't tell intended state from work-in-progress. `tools:targetApi="34"` is stale against `targetSdk = 35` (lint-only, but misleading).

**Fix:**
1. Commit the current tree as logical slices mirroring PHASE_2_SPEC ordering (bug fixes; export tests + verify; settings baseline; antidotes; counters; journal read side; §10 items; milestone letters; widget; §9 features). One commit per slice with spec-section references in messages.
2. Update `tools:targetApi` to `35`.
3. Add `*.jks` / `*.keystore` to `.gitignore` (see P0.1).

**Acceptance:** `git status` clean; `git log --oneline` shows reviewable slices.

---

## P2 — Quality improvements (post-launch acceptable)

### P2.1 Soundscapes don't request audio focus

**Where:** `ui/sos/SoundscapePlayer.kt`

**Problem:** Playback starts without `AudioFocusRequest`. The class KDoc says ambient sound "shouldn't duck other media," which is a defensible product choice — but without *requesting* focus, other apps also can't duck/stop Tidelet correctly (e.g., a navigation prompt or incoming call may overlap the soundscape), and the player ignores `AUDIOFOCUS_LOSS` entirely.

**Fix:** Request focus with `AudioManager.AUDIOFOCUS_GAIN` + `setWillPauseWhenDucked(false)`; on `AUDIOFOCUS_LOSS` stop playback, on `AUDIOFOCUS_LOSS_TRANSIENT` pause and resume on regain. Abandon focus in `release()`.

**Acceptance:** Manual: start Ocean soundscape → play music in another app → soundscape yields; return → resumes (or stays stopped, per chosen UX — document the choice in the KDoc).

### P2.2 `milestoneLabel` and widget strings bypass localization

**Where:** `util/Time.kt` (`milestoneLabel` returns hardcoded English), `strings.xml:617–618` (`widget_streak_singular`/`_plural` as two strings)

**Problem:** PHASE_2_SPEC lists localisation as a feedback theme. `milestoneLabel` can't be translated (it's pure-Kotlin by design, which is good for tests but bad for i18n), and the singular/plural pair should be a `<plurals>` resource so other languages' plural rules work.

**Fix:** Have `milestoneLabel` return a `@StringRes`-friendly key or move the string mapping to a UI-layer `@Composable`/`Context` helper backed by `strings.xml`; replace the widget pair with `<plurals name="widget_streak_days">`. Keep a pure-Kotlin `milestoneDays → semantic bucket` function for tests.

**Acceptance:** `lintDebug` shows no hardcoded-text warnings for these paths; pseudo-locale (`en-XA`) renders translated milestone labels.

### P2.3 Splash gate mutates a captured `var` during composition

**Where:** `MainActivity.kt:86–94`

**Problem:** `var profileLoaded = false` is captured by `setKeepOnScreenCondition` and flipped inside composition (`if (profile != null) profileLoaded = true`). It works because the condition is polled, but a side-effect write during composition violates Compose rules (recomposition can be speculative/discarded) and will trip future lint.

**Fix:** Hoist to `val profileLoaded = mutableStateOf(false)` outside `setContent`, flip it inside a `LaunchedEffect(profile != null)`, and read `!profileLoaded.value` in the keep-on-screen condition.

**Acceptance:** Behavior unchanged (splash dismisses after first profile emission); no composition-side mutation.

### P2.4 Export "byte count" is actually a char count

**Where:** `SettingsViewModel.exportTo` (`markdown.length` → `exportedByteCount`)

**Fix:** Use the actual written byte array: `val bytes = markdown.toByteArray(Charsets.UTF_8)` once, write `bytes`, report `bytes.size`. (Differs whenever entries contain non-ASCII — ₹, emoji, Devanagari.)

**Acceptance:** Unit test with a multi-byte-character journal entry asserts reported size equals `bytes.size`.

### P2.5 Dependency currency + deprecated Gradle DSL

**Where:** `gradle/libs.versions.toml`, `app/build.gradle.kts`

**Problem:** Room is pinned to a beta (`2.7.0-beta01`) — don't ship a beta DB to production. Compose BOM `2024.12.01`, lifecycle `2.8.7`, navigation `2.8.5`, datastore `1.1.1` are all roughly 18 months old against AGP 9.1.1/Kotlin 2.2.10 (verify current stable versions before bumping — do NOT guess them). `kotlinOptions { jvmTarget }` is the deprecated DSL under recent AGP/KGP.

**Fix:** Bump Room to current stable first (rerun all DAO/migration tests), then the AndroidX set one group at a time; replace `kotlinOptions` with `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`.

**Acceptance:** Full unit + instrumented suite green after each bump; no deprecation warnings from the Kotlin/AGP DSL in `./gradlew help`.

### P2.6 Soundscape license verification (launch plan §4.5)

**Where:** `app/src/main/assets/soundscape_credits.md`, `res/raw/{ocean,river,rain}.mp3`

**Problem:** Credits file exists but the per-file license verification (CC0/CC-BY/royalty-free, redistribution allowed) is a launch-plan checkbox that hasn't been evidenced anywhere. Also: the credits live in `assets/` but no screen renders them — CC-BY would *require* visible attribution.

**Fix:** Verify each file's source + license; record source URL + license + date checked in `soundscape_credits.md`; if any file is CC-BY, surface credits in Settings → About (small static screen reading the file from assets).

**Acceptance:** Credits doc lists license per file; if attribution is required, it's visible in-app.

### P2.7 Accessibility audit pass (launch plan §2.4)

**Where:** all screens; priority on the SOS flow

**Problem:** Not yet evidenced. Specific checks: every `IconButton` has a `contentDescription` (bottom-nav icons correctly use `null` + text labels); touch targets ≥48dp; TalkBack order through Breathe/RideTheWave (timers/announcements — consider `liveRegion` for phase changes); layouts at 200% font scale; `companion` visuals honor reduce-motion (spec §9B1 says snap, verify implemented).

**Fix:** Run Accessibility Scanner + manual TalkBack on: onboarding, Home, SOS hub, Breathe, Ride the Wave, check-in form, Settings. Fix flagged items; add `Modifier.semantics` where timers announce phase changes.

**Acceptance:** Accessibility Scanner reports no errors on those screens; TalkBack can complete a full SOS session unaided.

---

## Explicitly fine (reviewed, no action)

- **Privacy posture:** no `INTERNET` permission, no analytics/3P SDKs, `allowBackup=false`, logs limited to two `Log.w` in `SoundscapePlayer` (no user content). Matches the Data Safety story exactly.
- **`runBlocking` in `StreakWidgetProvider`:** correct for `AppWidgetProvider`'s synchronous lifetime; well-documented.
- **Export codec:** pure Kotlin, versioned banner, escape handling, unknown-kind tolerance — good design, well-tested (`TideletMarkdownTest`, round-trip + corrupt-input instrumented tests).
- **Breathe first-cycle bug (spec §1):** fixed with `Animatable(0.4f)` as specified.
- **`verifyBackup` (spec §2):** present in `SettingsViewModel` with in-memory round-trip.
- **Two same-name `JournalHubScreen` composables** (`ui/sos` write-side, `ui/journal` read-side): legal Kotlin (different signatures) and per-spec namespacing; slightly confusing but acceptable. Optional: rename the SOS one `JournalWriteHubScreen`.
- **Test architecture:** sharedTest source set, fake repository, fixed clocks, layered unit/instrumented split — better than most production apps.

## Suggested agent execution order

1. P1.7 (commit slices first — everything else becomes reviewable)
2. P0.2 → P0.3 (schema export, then remove destructive fallback)
3. P0.4 (transactional import)
4. P0.1 (minify + signing; needs a device smoke test afterward)
5. P1.3, P1.5, P1.6, P2.3, P2.4 (small, independent)
6. P1.2 (widget worker gating)
7. P1.1 (needs a product decision: ship or cut reminders)
8. P1.4, P2.6, P2.7 (content/policy items, partly manual)
9. P2.1, P2.2, P2.5 (post-launch quality)

---

*Reviewed by Claude against `PHASE_2_SPEC.md`, `Tidelet_Play_Store_Launch_Plan.md`, and Play policy requirements. Line numbers refer to the working tree as of 2026-06-10.*
