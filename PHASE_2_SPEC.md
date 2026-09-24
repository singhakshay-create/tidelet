# Tidelet — Phase 2 Implementation Spec

Follows from [README.md](README.md) (Phase 1 shipped) and the testing scaffold in `app/src/{test,sharedTest,androidTest}`.

## Context

Phase 1 shipped onboarding, streak counter, the SOS toolkit (Ride the Wave, Breathe, My Reasons, Do Something Else), the daily check-in log, and a full layered test suite. Early user feedback surfaces a handful of concrete improvements around data trust (backup, log review, test coverage), localisation, CBT depth, retention (streak visibility), and one user-reported bug.

This spec orders the work by a mix of **quality first** (bug fix, missing tests), then **easy high-value wins** (Settings customisation, antidotes), then **higher-effort features** (log review, milestone letters, widget). Everything preserves the Phase 1 constraints: no login, no network permissions, on-device only, fully anonymous.

## Priority order

1. **Bug**: Breathe circle stays static through the first half-cycle
2. **Tests**: Export/import round-trip coverage + "Verify my backup" action
3. **Settings**: user-editable drinks/day, price, and currency locale
4. **Thought Patterns**: add antidotes per distortion; keep Habit out of the list
5. **Log review**: read-only browsing of Thought Checks + Journal entries, grouped by distortion + "on this day"
6. **Local-only usage counters**: anonymous in-app stats keyed on what the user does
7. **Milestone letter-to-self**
8. **Home-screen widget**: current streak

Sections below contain for each: **Why → What → Where → How → Acceptance.**

---

## 1. Bug fix — Breathe circle doesn't animate in the first half-cycle

### Why

The breathing metaphor depends on the circle growing *during* the "In" phase and shrinking *during* "Out". Today the first full cycle (0–8 s) renders a static big circle — no visible "breathing in" at all. The user notices.

### Root cause

In [BreatheScreen.kt](app/src/main/java/com/tidelet/app/ui/sos/BreatheScreen.kt) (lines ~67–106):

```kotlin
var phase by remember { mutableStateOf(BreathPhase.In) }         // starts at In
val targetScale = when (phase) {
    BreathPhase.In -> 1f; BreathPhase.Hold1 -> 1f
    BreathPhase.Out -> 0.4f; BreathPhase.Hold2 -> 0.4f
}
val scale by animateFloatAsState(targetValue = targetScale, ...)
```

On first composition, `animateFloatAsState` snaps to the initial target (1f) without animating. Then In → Hold1 keeps the target at 1f (no animation either). Scale first changes at t=8s when phase flips to Out. The intent — "In = grow from empty to full lungs" — is never rendered.

### Fix

Swap `animateFloatAsState` for an `Animatable` initialised at the "empty lungs" value (0.4f), and drive it from a `LaunchedEffect(phase)`. Initial state becomes: circle small, phase=In → animates 0.4f → 1f over 4 s. The rest of the cycle behaves as before.

```kotlin
val scaleAnim = remember { Animatable(0.4f) }
LaunchedEffect(phase) {
    val target = if (phase == BreathPhase.In || phase == BreathPhase.Hold1) 1f else 0.4f
    val duration = when (phase) {
        BreathPhase.In, BreathPhase.Out -> phase.durationMillis
        BreathPhase.Hold1, BreathPhase.Hold2 -> 0
    }
    scaleAnim.animateTo(target, tween(duration, easing = LinearEasing))
}
val scale = scaleAnim.value
```

### Where

- [app/src/main/java/com/tidelet/app/ui/sos/BreatheScreen.kt](app/src/main/java/com/tidelet/app/ui/sos/BreatheScreen.kt) — replace the `animateFloatAsState` block (~lines 88–106) with the `Animatable` pattern above. Delete the now-unused `animDuration` local.
- Imports: add `androidx.compose.animation.core.Animatable`.

### Acceptance

- Manual: open Breathe → the circle is small at t=0, grows during "In" (t=0→4s), holds large during "Hold" (t=4→8s), shrinks during "Out" (t=8→12s), holds small during "Hold" (t=12→16s). Repeats.
- Update [BreatheScreenTest.kt](app/src/androidTest/java/com/tidelet/app/ui/sos/BreatheScreenTest.kt) with a test that samples the circle's visual size at t=0 and t=3.5 s (via `mainClock.advanceTimeBy`) and asserts the second measurement is strictly larger.

---

## 2. Tests — Export/import round-trip + "Verify my backup" action

### Why

Export/import is the user's only path to data portability and the only protection against a dead phone. Phase 1 testing explicitly scoped this out; there are currently zero tests against [ExportImportManager.kt](app/src/main/java/com/tidelet/app/data/export/ExportImportManager.kt) or [TideletMarkdown.kt](app/src/main/java/com/tidelet/app/data/export/TideletMarkdown.kt). One user already exports daily out of fear of data loss — the feature must be provably correct.

### What

1. **Round-trip test coverage** — seeded data → export → wipe → import → assert every record round-trips with original timestamps.
2. **Corrupt-input tests** — malformed Markdown must fail cleanly without partial writes.
3. **"Verify my backup" in-app action** — one-tap self-test in Settings that builds an export in memory, re-imports it into a scratch buffer, compares row counts + hashes, reports "✓ 147 records round-trip cleanly" or shows what diverged.

### Where

**New test files:**
- `app/src/test/java/com/tidelet/app/data/export/TideletMarkdownTest.kt` — pure-JVM codec tests (encode→decode a seeded `ExportSnapshot`; malformed input; empty collections; Unicode in user text).
- `app/src/androidTest/java/com/tidelet/app/data/export/ExportImportRoundTripTest.kt` — full integration: seed `RoomTideletRepository` → `buildExport` → wipe + re-create DB → `applyImport` → assert every collection matches seed by content hash.
- `app/src/androidTest/java/com/tidelet/app/data/export/ExportImportCorruptInputTest.kt` — feed truncated markdown, mixed-up headers, invalid dates → assert `applyImport` either returns a `failed=N` report without writing, or throws a typed exception.

**Modify:**
- [app/src/main/java/com/tidelet/app/ui/settings/SettingsViewModel.kt](app/src/main/java/com/tidelet/app/ui/settings/SettingsViewModel.kt) — add `fun verifyBackup()` that runs the round-trip in memory (no file I/O) and surfaces a `VerifyReport` on the existing `SettingsUiState`.
- [app/src/main/java/com/tidelet/app/ui/settings/SettingsScreen.kt](app/src/main/java/com/tidelet/app/ui/settings/SettingsScreen.kt) — add a "Verify my backup" button between Export and Delete-all. Show the report inline (row counts matched / any discrepancies).

**Update:**
- [PHASE_1_SPEC]/[testing plan] — mark export/import as in-scope going forward.

### How

- `VerifyReport` is small: `data class VerifyReport(val matched: Int, val mismatched: Int, val perCollection: Map<String, Pair<Int, Int>>)`.
- Verification runs entirely in process: `val md = manager.buildExport(); manager.applyImportToScratch(md)` — the "scratch" variant takes a target `TideletRepository` to write to instead of the real one, so we can diff without touching live data.
- To enable this cleanly, split `ExportImportManager.applyImport` so it accepts an injectable target repo; default = `this.repo`.

### Acceptance

- `./gradlew test` — `TideletMarkdownTest` passes with ≥10 assertions (round-trip per entity type + edge cases).
- `./gradlew connectedAndroidTest` — `ExportImportRoundTripTest` and `ExportImportCorruptInputTest` pass.
- Manual: Settings → "Verify my backup" → after seeding 10+ records, button reports "✓ all records match" within 1 s. Corrupt the in-memory export (truncate last line) via a debug hook and confirm the button reports mismatches without modifying live data.

---

## 3. Settings — customisable drinks/day, price, and currency locale

### Why

Current money-saved math hard-codes USD ($8 × 3/day ≈ $24/day) because the Stats card reads `UserProfile.priceCentsPerDrink` and `typicalDrinksPerDay` but never surfaces them for editing. An IN user needs ₹250 × 4/day ≈ ₹1000/day. More broadly: the app's "money saved" narrative is only motivating when it reflects *the user's* actual drinking baseline and currency.

### Design constraint (from user)

> Make these inputs customisable through Settings, but don't ask for this information in the onboarding flow since it'll add friction.

So: Settings-only. No onboarding changes. Sensible defaults stay for users who never open Settings.

### What

- Two number-input rows in Settings: "Typical drinks per day" (1–30 stepper) and "Price per drink" (integer currency input, with a live-updating symbol).
- Currency symbol and formatting come from `NumberFormat.getCurrencyInstance(Locale.getDefault())` — no per-user currency picker yet. The user's system locale drives the symbol.
- Sensible locale-aware defaults applied once at first launch (or migration): US → 3/day, $8; IN → 4/day, ₹250; fall-back to the existing (3, $8).

### Where

- [app/src/main/java/com/tidelet/app/data/prefs/UserPreferences.kt](app/src/main/java/com/tidelet/app/data/prefs/UserPreferences.kt) — already has `setDrinkingBaseline(drinksPerDay, priceCentsPerDrink)`. Reuse.
- [app/src/main/java/com/tidelet/app/ui/settings/SettingsScreen.kt](app/src/main/java/com/tidelet/app/ui/settings/SettingsScreen.kt) — add "Drinking baseline" section with two inputs and a "Save" action.
- [app/src/main/java/com/tidelet/app/ui/settings/SettingsViewModel.kt](app/src/main/java/com/tidelet/app/ui/settings/SettingsViewModel.kt) — expose current profile as part of `SettingsUiState`; add `updateBaseline(drinksPerDay: Int, priceCents: Int)` that calls `repo.restoreProfile(typicalDrinksPerDay = ..., priceCentsPerDrink = ..., ...)` (existing method already does this).
- [app/src/main/java/com/tidelet/app/ui/stats/StatsScreen.kt](app/src/main/java/com/tidelet/app/ui/stats/StatsScreen.kt) and its ViewModel — render money saved via locale-aware currency formatter rather than a hardcoded `$` prefix. (If it's already formatted, just verify.)
- [TideletApplication.kt](app/src/main/java/com/tidelet/app/TideletApplication.kt) or a small one-shot `SeedLocaleDefaults` runner invoked on first launch (check: if `priceCentsPerDrink == 800` and the user is in IN locale and has no other edits, seed INR defaults). Gate on a DataStore boolean `baselineSeededForLocale` so we only do this once.

### How

- Currency formatting: `NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }.format(priceCents / 100.0)` — produces "$8" in en-US, "₹250" in en-IN, "€8" in de-DE, etc.
- Input UX: accept integer currency values only (we already store cents). Show the system symbol as a prefix adornment on the OutlinedTextField.
- Validation: drinks/day in `1..30`; priceCents in `0..1_000_000`. Reject outside-range silently (clamp).
- No new Room schema changes — everything sits in DataStore.

### Acceptance

- Manual: set system locale to IN → first launch → Settings shows ₹ symbol and 4/₹250 defaults → Stats money saved reads "₹X,XXX". Set to US → defaults to 3/$8. Edit to "6 drinks/day, $12" in US locale → Stats reflects $72/day × streak days.
- Unit test: `SettingsViewModelTest::updateBaseline clamps drinksPerDay to 30`, `updateBaseline clamps priceCents to 0`, `state.baseline mirrors repo.profile`.
- Pure-JVM util test: `CurrencyFormatTest` verifying `formatCents(25000, Locale.US) == "$250"` (or the platform's actual output — adjust assertion to match).

---

## 4. Thought Patterns — add antidotes; keep Habit out

### Why

A distortion name on its own is trivia. Paired with a technique and a challenging question, it becomes a tool the user can actually use mid-craving. The user also asked whether "Habit" belongs here; it doesn't — habits are behavioural, not cognitive, and mixing them confuses the intervention path.

### What

Per-distortion, add two fields:

- **Technique** — a short rule-of-thumb reframe (e.g. all-or-nothing → "Find the gray area").
- **Challenging question** — one question the user can ask themselves (e.g. all-or-nothing → "Would I judge a friend this harshly for the same slip?").

Plus one small UX addition at the bottom of the list: a soft "Not a thought — more of a habit?" callout linking to a one-screen explainer that habit drinking is tracked via the Check-in trigger list, not here.

### Where

- [app/src/main/java/com/tidelet/app/ui/cbt/CognitiveDistortions.kt](app/src/main/java/com/tidelet/app/ui/cbt/CognitiveDistortions.kt) — extend the `Distortion` data class with two `@StringRes` fields: `techniqueRes` and `challengeRes`. Update all 10 list entries.
- `app/src/main/res/values/strings.xml` — add 20 new strings: `distortion_<key>_technique` and `distortion_<key>_question` for each of the 10 distortions.
- [app/src/main/java/com/tidelet/app/ui/cbt/DistortionsLibraryScreen.kt](app/src/main/java/com/tidelet/app/ui/cbt/DistortionsLibraryScreen.kt) — render the two new fields under each entry (bold "Try:" before the technique, italic for the question).
- Same screen — append a small card at the bottom: "Not a thought? Habit drinking is logged as a trigger in your daily check-in" with a nav to Check-in.

### Copy (starter)

| Key | Technique | Challenging question |
|---|---|---|
| `all_or_nothing` | Find the middle ground. | Would I judge a friend this harshly for the same slip? |
| `permission_giving` | Name the thought out loud. It loses power when said. | What does "just this once" actually cost me this time? |
| `emotional_reasoning` | Feelings aren't evidence. Treat the feeling as weather, not fact. | If I weren't feeling this, would I still believe it? |
| `fortune_telling` | Name it: "I'm predicting, not knowing." | What's one time this prediction was wrong? |
| `minimizing` | Put the actual number on it. | If a friend did this, would I call it small? |
| `magnifying` | Say it smaller. One slip, not a collapse. | On a scale of 1–10, how bad is this *actually*? |
| `should_ing` | Swap "should" for "would prefer to." | Who decided this was a rule? |
| `mental_filter` | Add one good thing you're ignoring. | What am I not counting right now? |
| `personalization` | Separate your part from what wasn't yours. | What did I actually control here? |
| `labeling` | Describe the behaviour, not the person. | What would I say about a friend who did this once? |

(Copy refinement welcome — starter text is deliberately tight so the coding agent has something to ship and the PM can edit in-place.)

### Acceptance

- Manual: open Distortions library → every entry shows name, definition, example, technique, challenging question → bottom card visible and nav works.
- Unit test: `CognitiveDistortionsTest::every distortion has non-zero techniqueRes and challengeRes`.
- Confirm: no new entry with key `"habit"` is added. The "Not a thought?" callout is a UI element only, not a `Distortion` list entry.

---

## 5. Log review — read-only browsing of Thought Checks + Journal entries

### Why

Users write Thought Checks and Journal entries but have no way to revisit them. In CBT, re-reading past records is where the therapeutic value compounds — you notice the same distortion showing up, you see old thoughts reframed by your later self. Today the app writes and never reads. This is the highest-leverage content feature on the list.

### What

Two new screens under a new top-level nav section, "Journal":

**Thought Checks list**
- Entries grouped by distortion tag (e.g. "All-or-nothing (7)", "Permission-giving (3)") with an "All" pseudo-group at the top.
- Each entry card shows: date, the raw thought (truncated), and the distortion tag as a coloured chip.
- Tap an entry → read-only detail with all four fields (situation, thought, challenge, friend reframe).

**Journal (Write Freely) list**
- Reverse-chron list.
- Full-text search box at the top (client-side substring match against `text`).
- Each row: date + first line + read-only detail on tap.

**"On this day" card** on Home (below the streak, above the SOS button):
- If any Thought Check or Journal entry exists from exactly N months ago today (N ≥ 1), surface the earliest one as a quiet card.
- Dismissible; re-appears the next day with a different anniversary.

### Where

**New screens:**
- `app/src/main/java/com/tidelet/app/ui/journal/JournalHubScreen.kt` (note: `ui/sos/JournalScreen.kt` + `ui/sos/JournalHubScreen.kt` already exist for *writing* — this is the read-side counterpart; namespace under `ui/journal/` to avoid confusion).
- `app/src/main/java/com/tidelet/app/ui/journal/ThoughtChecksListScreen.kt`
- `app/src/main/java/com/tidelet/app/ui/journal/ThoughtCheckDetailScreen.kt`
- `app/src/main/java/com/tidelet/app/ui/journal/JournalListScreen.kt`
- `app/src/main/java/com/tidelet/app/ui/journal/JournalDetailScreen.kt`
- Corresponding ViewModels in the same package.

**Modify:**
- [app/src/main/java/com/tidelet/app/ui/nav/TideletNav.kt](app/src/main/java/com/tidelet/app/ui/nav/TideletNav.kt) — add a "Journal" tab or surface the new screens from the existing bottom nav.
- [app/src/main/java/com/tidelet/app/ui/home/HomeScreen.kt](app/src/main/java/com/tidelet/app/ui/home/HomeScreen.kt) and [HomeViewModel.kt](app/src/main/java/com/tidelet/app/ui/home/HomeViewModel.kt) — add an "on this day" slot to `HomeUiState` + the corresponding card.

**Repository:**
- No schema changes. Reuse `repo.thoughtRecords` and `repo.journalEntries`.
- Add a convenience query: `fun thoughtRecordsByDistortion(): Flow<Map<String?, List<ThoughtRecord>>>` in the repo interface (ViewModel-side derivation is fine too — pick the cleaner split).

### Acceptance

- Manual: seed 5 Thought Checks across 3 distortion tags + 10 Journal entries. Thought-check list groups correctly with counts. Search in Journal returns substring matches. Detail screens render every field. Back nav works.
- Unit: `ThoughtCheckListViewModelTest::groupsByDistortionTag`, `JournalListViewModelTest::searchFiltersOnSubstring`, `HomeViewModelTest::onThisDayShowsEntryFrom12MonthsAgo` (with a fake clock).
- Compose UI: one happy-path test per list screen asserting the empty state, the grouped list, and detail navigation.

---

## 6. Local-only usage counters

### Why

"What features get used most?" is the question that should be driving prioritisation, but today the app doesn't know. A purely local counter — stored in DataStore, surfaced on the Stats screen, never leaves the device — answers it for the user (self-reflection) and for the PM (when the user exports a diagnostic). Respects the no-network constraint fully.

### What

- Per-tool open counters: every time the user enters Ride the Wave / Breathe / Reasons / Distractions / Journal / Thought Check, increment a counter keyed on the tool.
- Surface on Stats: "You've used Breathe 23 times this month — it's your most-used tool."
- Include in the export (so a user who shares their backup with a therapist or future self has this signal).

### Where

- [app/src/main/java/com/tidelet/app/data/prefs/UserPreferences.kt](app/src/main/java/com/tidelet/app/data/prefs/UserPreferences.kt) — add `toolOpenCounts: Flow<Map<String, Int>>`, `suspend fun incrementToolCount(toolKey: String)`. Use `stringSetPreferencesKey` + encoded string, or individual keys per tool (simpler).
- Repository — forward as `val toolOpenCounts: Flow<Map<String, Int>>` and `suspend fun recordToolOpen(tool: String)` on the interface. Add to `FakeTideletRepository` too.
- Each SOS ViewModel `init` block — call `repo.recordToolOpen(SosToolKey.X)` once on entry.

Alternative (lighter): derive counts from existing `CravingEvent` rows by counting per `tool`. But that only counts *completed* sessions (ones that logged an outcome). Explicit tool-open counters catch abandoned sessions too. Pick per-tool counters for truthfulness.

- [app/src/main/java/com/tidelet/app/ui/stats/StatsScreen.kt](app/src/main/java/com/tidelet/app/ui/stats/StatsScreen.kt) and ViewModel — add a "Tools used" card.
- [ExportImportManager.kt](app/src/main/java/com/tidelet/app/data/export/ExportImportManager.kt) — add `toolOpenCounts` to the `ExportSnapshot` and codec.

### Acceptance

- Manual: open Breathe twice, Ride the Wave once → Stats shows "Breathe: 2, Ride the Wave: 1".
- Unit: `BreatheViewModelTest::entering the screen records a tool-open count` using a fresh fake.
- Integration: `UserPreferencesTest::incrementToolCount persists and accumulates`.
- Export round-trip (ties into §2): the new `toolOpenCounts` field round-trips through Markdown.

---

## 7. Milestone letter-to-self

### Why

At meaningful streak milestones (7, 30, 90 days), invite the user to write a short letter to their future self. Resurface it at the next milestone. Creates emotional compound interest — a 30-day user reading their own 7-day letter is a powerful, private moment. No accountability partner needed.

### What

- New entity: `MilestoneLetter(milestoneDays: Int, writtenAtEpochMillis, text, revealedAtEpochMillis: Long?)`.
- **Write flow**: when the Home streak hits a canonical milestone (reuse `MILESTONES_DAYS`: 1, 3, 7, 14, 30, 60, 90, 180, 365), show a one-time card on Home: "You've reached {milestoneLabel}. Write a note to your future self?" → tap → simple textarea → save. Dismissible once.
- **Reveal flow**: when the user reaches the *next* milestone after a written letter, surface the previous letter as a read-only card on Home for that day, then mark it as revealed. Persist revealed state so it doesn't nag.

### Where

**New files:**
- `app/src/main/java/com/tidelet/app/data/db/MilestoneLetter.kt` — `@Entity`.
- Extend `app/src/main/java/com/tidelet/app/data/db/Daos.kt` — `MilestoneLetterDao` with `insert`, `findByMilestone(days: Int)`, `observeAll(): Flow<List<MilestoneLetter>>`, `markRevealed(milestoneDays: Int)`, `clear()`.
- Bump `TideletDatabase.version` and register the entity + DAO.
- `app/src/main/java/com/tidelet/app/ui/milestones/MilestoneLetterScreen.kt` (write) and `MilestoneLetterRevealCard.kt` (read, embedded on Home).
- Corresponding `MilestoneLetterViewModel.kt`.

**Modify:**
- `TideletRepository.kt` (interface + Room impl + fake) — add letter CRUD.
- `HomeViewModel.kt` / `HomeUiState` — add `pendingLetterPrompt: Int?` (milestone days for today's prompt) and `letterToReveal: MilestoneLetter?`.
- `HomeScreen.kt` — surface the prompt card and the reveal card.
- `ExportImportManager.kt` + `TideletMarkdown.kt` — include `MilestoneLetter` rows in the export snapshot.

### How

- Deciding when to prompt: on `HomeUiState` emission, compute `currentMilestone = MILESTONES_DAYS.lastOrNull { it <= streak.days }`. If non-null and no letter exists for that milestone yet → prompt.
- Deciding when to reveal: compute `previousMilestone = MILESTONES_DAYS.lastOrNull { it < streak.days }`. If a letter exists for it and has `revealedAtEpochMillis == null` → reveal today, then mark it.
- Destructive-migration is fine in Phase 2 per existing policy (see [README.md](README.md)).

### Acceptance

- Manual: set a start date 30 days ago via Settings → Home surfaces the prompt for day 30. Write a letter → confirm it's saved. Set start date 60 days ago → letter from 30 days shows up as a reveal card.
- Unit: `HomeViewModelTest::prompts for 30-day letter when current streak is 30 and no letter exists`, `::reveals 7-day letter when current streak hits 30`.
- Integration: `MilestoneLetterDaoTest` (standard DAO test following the pattern in `CheckInDaoTest`).
- Export round-trip: letters survive a full export → wipe → import cycle.

---

## 8. Home-screen widget — current streak

### Why

A widget on the home screen is the most durable behaviour cue we can build: the user sees their streak count *every time they unlock their phone*, with no app-open required. Android widgets are local-only by design (no network), so they fit the privacy model perfectly. This is the feature most likely to improve retention per line of code.

### What

A single 2×1 widget that shows: the current streak day count, the "days" suffix, and the app name. Tap-through opens the app at Home. Updates at least every hour (Android limits how aggressive you can be) and whenever the app writes to `UserPreferences`.

### Where

**New files:**
- `app/src/main/java/com/tidelet/app/widget/StreakWidgetProvider.kt` — extends `AppWidgetProvider`. On `onUpdate`, reads `UserPreferences.profile` synchronously (`runBlocking { prefs.profile.first() }` — acceptable on a widget update) and recomputes the streak via `streakFromStartDate`.
- `app/src/main/res/layout/streak_widget.xml` — simple `TextView` + background.
- `app/src/main/res/xml/streak_widget_info.xml` — `AppWidgetProviderInfo` metadata (min/target size, update period, configure activity = none).
- `app/src/main/AndroidManifest.xml` — register the `<receiver>` + `<meta-data>` for the widget.

**Modify:**
- Whenever start-date changes (onboarding, Settings) — call `AppWidgetManager.getInstance(context).updateAppWidget(...)` to force an immediate refresh.
- Consider a `WorkManager` periodic worker (every 1 hour) that re-triggers the widget update so the count increments without the user opening the app. Phase 2 already depends on WorkManager — reuse.

### How

- The widget runs in a separate process context, so it cannot share the `TideletApplication.repository` singleton. Instead, it:
  1. Constructs its own `UserPreferences(context)` instance.
  2. Calls `prefs.profile.first()` (cheap — DataStore is file-backed).
  3. Computes streak via the existing `streakFromStartDate` (lives in `util/Time.kt`, already Android-free).
- Widget text: "N days" for streak ≥ 1 day, otherwise a small "Start your streak" CTA. Localisable via `strings.xml`.

### Acceptance

- Manual: install debug build → long-press home → Widgets → Tidelet → drop widget on home screen → widget shows current streak day count. Edit start date in Settings → widget refreshes within 1 second. Leave phone overnight → next morning widget shows +1 day even if app wasn't opened (via WorkManager hourly trigger).
- Automated (instrumented): widget rendering tests require a real device; we can skip this gate and rely on manual verification + a small unit test of the "streak → widget text" pure function.

---

## Deferred — documented, not implemented this phase

These ideas came up in brainstorming and are worth recording. Do NOT implement as part of Phase 2. Revisit after the above lands and we have usage data from §6.

### Voice journaling via SpeechRecognizer
Let users dictate Thought Checks and Journal entries instead of typing. Uses Android's on-device `SpeechRecognizer` — no cloud. Lowest-friction way to journal mid-craving. Implementation: new input mode on the Journal and Thought Check write screens, with a mic button that falls back to keyboard if speech recognition is unavailable.

### Relapse drill mode
A "practise in calm moments" affordance — walks the user through the SOS tools as if a craving were happening, without logging anything real. Standard CBT-for-AUD technique; reduces the fumble factor when a real craving hits. Reuses every existing screen; adds a `DrillMode` flag that suppresses outcome logging.

### Localised crisis resources page
Country-appropriate helpline numbers (AA, SMART Recovery, national crisis lines) shown via `tel:` / `sms:` links. Keyed on `Locale.getDefault().country`, no account. When someone is in deep crisis, the app should hand them to a human.

### Revisit Android auto-backup
Phase 1 has `allowBackup=false` in the manifest — aggressive privacy stance, but means new-phone setup loses everything. Consider enabling selective auto-backup of DataStore + Room only, which stays encrypted in the user's own Google account. Needs a privacy review before flipping.

### Flagged as "do not build"

- **On-device LLM insights**: battery + storage cost doesn't justify nuance a SQL aggregation can produce. See §6 — counters give most of the value.
- **Anonymous social stats across users**: requires a network call. Breaks the "zero network permissions" promise, which is the app's differentiator.
- **Accountability buddy sharing in-app**: users can already export Markdown and share via any channel they choose. Don't build the channel.

---

## Cross-cutting verification checklist

After implementing the above, the following should all hold:

- [ ] `./gradlew :app:test` — unit layer green (existing + new: codec tests, currency format tests, new VM tests for log review, milestone letters, settings baseline edits, widget pure-function tests).
- [ ] `./gradlew :app:connectedAndroidTest` — integration + UI green (existing + new: export round-trip, corrupt-input, MilestoneLetter DAO, new journal/read-side UI tests).
- [ ] Manual: Breathe circle grows during first "In" phase at t=0–4 s (bug fix §1).
- [ ] Manual: Settings → Verify my backup shows ✓ after seeding realistic data.
- [ ] Manual: Settings → edit drinks/price → Stats money-saved reflects changes in the system-locale currency.
- [ ] Manual: Distortions library shows technique + challenging question per entry; "Not a thought?" callout navigates to Check-in.
- [ ] Manual: Journal tab → read old Thought Checks grouped by distortion; search Journal entries; "on this day" card appears on Home when a past-year entry exists.
- [ ] Manual: After 30-day streak, Home prompts for a letter; after 90-day streak, the 30-day letter is revealed.
- [ ] Manual: Streak widget added to home screen shows correct day count and refreshes when profile changes.
- [ ] Export → Wipe → Import round-trips all Phase 1 + Phase 2 data (check-ins, craving events, reasons, thought records, functional analyses, relapse plan, refusal phrases, journal entries, weekly reflections, tool-open counters, milestone letters, profile).

## Implementation ordering suggestion

Land in this order so each change is independently reviewable:

1. §1 Breathe bug (tiny, high user-visibility)
2. §2 Export/import tests + Verify backup (de-risks everything that follows)
3. §3 Settings baseline (isolated, small)
4. §4 Thought Pattern antidotes (copy-heavy but structurally small)
5. §6 Local-only usage counters (tiny schema; unlocks §5 data viz)
6. §5 Log review screens (biggest UX feature)
7. §7 Milestone letter-to-self (needs new entity + UI)
8. §8 Home-screen widget (new Android surface area; save for last)

Each slice should be a single PR with its own tests green before moving on.

---

## 9. v-next — Engagement & reflection layer

The features below are drawn from [gamification_research.md](../gamification_research.md), §5. They sit **after** Phase 2 proper (§1–§8) and should be scheduled only once the current spec ships. They are listed here so the implementation plan tracks the full v-next shortlist in one place.

### Theme-level non-goals (apply to every item in this section)

- **No leaderboards.** No public streaks, no social comparison, no "anonymous" cohort positioning.
- **No punishment mechanics.** No wilting pets, HP loss, "streak in danger" notifications, forfeited-progress warnings.
- **No notification-driven engagement.** Nothing in this section may trigger a push notification. All surfacing is in-app, inline, and visible only when the user opens the app.
- **No engagement-minute optimization.** Opening Tidelet *less* over time is a success signal; none of these features should fight that trend.
- **Everything opt-in or off-by-default** unless noted. Autonomy trumps salience.

### Build order within this section

Tier A first (A1, A2, A4 are small and high-leverage; A3 extends §7; A5 introduces one new surface). Tier B items last, and only after A1–A5 ship, because B-tier items benefit from the Stats and copy work being in place.

1. **A4 — Streak grace language** (copy-only; unblocks the rest tonally)
2. **A1 — Health-recovery timeline on Stats**
3. **A2 — Hours reclaimed on Stats**
4. **A3 — Milestone letter nudge** (rides §7 infrastructure)
5. **A5 — Evening mini-review**
6. **B2 — Variable-reward insight cards**
7. **B1 — Compassionate visual companion** (last — highest design risk)

---

### 9A1. Health-recovery timeline on Stats

**Intent.** Pin a small set of alcohol-recovery science milestones (24h, 72h, 2 weeks, 30d, 90d, 1yr) to the user's actual day count, so "day 14" carries meaning beyond a number.

**Surface.** New card on [StatsScreen.kt](app/src/main/java/com/tidelet/app/ui/stats/StatsScreen.kt), above the existing money-saved card. The current-or-most-recently-reached milestone is highlighted; future milestones are dim, with the remaining day count ("12 days until liver enzymes typically normalize").

**Data-layer impact.** None. Six canonical milestones and their body copy live in `strings.xml` under `health_recovery_24h_title` / `_body`, etc. Reuse the existing streak state from `HomeViewModel` or compute in a new `HealthRecoveryTimelineProvider`.

**Copy constraints.** Phrases every entry as *typical* and *hedged* ("most people start to see…", "commonly at this stage…"). No medical claims of certainty. Footer line: "Every body is different. Talk to a clinician if anything feels wrong." No links out; no country-specific resources (that's deferred).

**Acceptance.**
- Manual: day 1 → first item ("24h") highlighted; day 20 → "2 weeks" highlighted, "30d" dim with "10 days to go".
- Unit: `HealthRecoveryTimelineTest::selects latest reached milestone`, `::computes days-until for next upcoming`.

### 9A2. Hours reclaimed on Stats

**Intent.** Add a second "progress beyond dollars" dimension beside the existing money-saved card.

**Surface.** Stats screen. Two small cards in a row (money saved | hours reclaimed) instead of the single card today.

**Data-layer impact.** One new scalar in `UserPreferences`: `hoursPerDrink: Double` (default `1.0` — hedged estimate covering drinking + post-drink recovery/hangover fraction). Exposed in Settings (§3 already adds baseline editing; extend that same section), not onboarding.

**How.** `hoursReclaimed = typicalDrinksPerDay × hoursPerDrink × streakDays`. Format: "≈ 84 hours reclaimed" with the explicit leading "≈" so the user reads it as an estimate.

**Acceptance.**
- Manual: 3 drinks/day, 1h/drink, 14 day streak → "≈ 42 hours". Change to 2h/drink → "≈ 84 hours".
- Unit: `StatsViewModelTest::hoursReclaimed multiplies baseline × days`.

### 9A3. Milestone letter nudge — extends §7

**Intent.** The write-flow in §7 already prompts on the milestone day. This item adds the *framing* — the prompt copy, a matching ambient treatment on Home, and the decision about when *not* to prompt.

**Surface.** Home card slot added by §7. Copy change only + one gating rule.

**Data-layer impact.** None (reuses `MilestoneLetter` entity from §7).

**What changes from §7.**
- Prompt copy: "Day 30. Would you like to write to your day-1 self?" (not "Write a milestone letter"). Framing is reflective, not achievement-coded.
- Gate: suppress the prompt if the user has logged a craving event in the last 24 h (they don't need a reflective ask mid-wave). Re-surfaces the next day.
- Reveal card (already in §7): copy shifts from "Your 30-day letter" to "From you, 30 days ago." Small point; large tonal difference.

**Acceptance.**
- Manual: day 30, no craving in last 24h → prompt shows. Log a craving → open Home → prompt suppressed. Next day → prompt returns.
- Unit: `HomeViewModelTest::suppresses milestone letter prompt when craving logged in last 24h`.

### 9A4. Streak grace language

**Intent.** Replace every "you lost your streak" / "streak broken" string with "starting a new streak" framing. Surface a second number — lifetime total dry days — alongside the current streak so the user sees cumulative progress even after a reset.

**Surface.** [HomeScreen.kt](app/src/main/java/com/tidelet/app/ui/home/HomeScreen.kt) (primary streak card) + Stats (if the reset event is ever shown there). No new screens.

**Data-layer impact.** One derived value: `lifetimeDryDays: Int`. Computed from existing data — sum of (current streak) + (historical streak runs). Historical runs aren't stored explicitly today; we can either (a) add a lightweight `StreakEpisode(startEpochDay, endEpochDay)` Room entity written on each reset, or (b) keep a single `previousStreaksTotalDays: Int` scalar in DataStore and increment it on reset. **(b) is simpler and sufficient** — we don't need per-episode granularity for Phase 2.

**Copy.** Grep for any existing string that contains "lost", "broke", "failed" in the streak context and replace. New starter:
- "Starting again — day 1." (instead of "Streak broken")
- "You've had 47 dry days total with Tidelet." (subtitle on streak card, shown only when `lifetimeDryDays > currentStreak`.)

**Acceptance.**
- Manual: reset via Settings → confirm "starting again — day 1" copy and lifetime counter visible.
- String audit: `./gradlew lintDebug` or grep — no "lost your streak" / "streak broken" strings remain in release source.
- Unit: `UserPreferencesTest::reset accumulates previousStreaksTotalDays`.

### 9A5. Evening mini-review

**Intent.** A one-question evening companion to the morning check-in. Two short fields: "One win today." / "One challenge today." Fully optional, off by default.

**Surface.** New small `EveningReviewScreen.kt` under `ui/checkin/`. Entry points: a small "Evening review" button on Home, visible only when (a) the user has opted-in in Settings and (b) no evening review has been logged today.

**Data-layer impact.** New Room entity `EveningReview(dateEpochDay, winText, challengeText, createdAtEpochMillis)`. One DAO with `insert`, `findByDate`, `observeAll`. Bump DB version (destructive migration per Phase 2 policy).

**Also add.**
- Settings toggle: "Show evening review prompt" (default off). Copy: "A two-line optional reflection, after dinner or before bed."
- Read-side: evening reviews show up in the new Journal/Log Review section from §5 — one more feed alongside Thought Checks and Journal entries, grouped by date.
- Export: new `EveningReview` rows in the Markdown snapshot (§2).

**Acceptance.**
- Manual: opt-in via Settings → Home shows button in evening → tap → write two short lines → save → button disappears for rest of day. Next day → button reappears.
- Unit: `EveningReviewViewModelTest::persists when both fields are non-empty`, `::blocks save when both fields empty`.
- Integration: `EveningReviewDaoTest::findByDate returns today's entry only`.

### 9B1. Compassionate visual companion — *prototype 3 options, pick one after review*

**Intent.** A small ambient visual on Home that advances with streak + check-in consistency. **No wilt, no penalty state.** Stagnation is the only negative signal — and even that is read by the user, not stated by the app.

**Surface.** Top of [HomeScreen.kt](app/src/main/java/com/tidelet/app/ui/home/HomeScreen.kt), above the streak card. Opt-in via Settings (default off). Roughly 96 dp tall, full-width, centred motif.

**Approach: build all three options behind a feature flag, review side-by-side, ship one.**
The coding agent should implement Options A, B, and C as three separate Composables sharing one interface so the chosen one can be wired in by a single line change after review. Discarded options are deleted, not left dormant.

```kotlin
// Shared interface — every option implements this
@Composable
fun CompanionVisual(
    stage: Int,           // 1..5; see "Stage progression" below
    isAdvancing: Boolean, // true if either driver moved up in the last 24h
    modifier: Modifier = Modifier,
)
```

#### Shared design constraints (apply to all three options)

- **Data-layer impact.** None. Stage is derived from existing `streakDays` and a rolling count of check-ins in the last 14 days. No new entity, no DataStore key beyond the opt-in toggle.
- **Stage progression formula.** `stage = (1..5).coerceIn(1 + max(streakDays / 14, checkInsLast14d / 3))`. This means stage 1 from day 0; stage 5 reached at either 56 dry days *or* 12 check-ins in last 14d, whichever comes first. The check-in driver is what lets the visual advance even after a streak reset.
- **No regression on reset.** Once a stage is reached, the visual never visibly steps backward, even if the streak resets to 0. Internally the state may be lower, but the rendered stage uses `max(currentStage, highestStageEverReached)` — that high-water mark lives in DataStore as one int.
- **No anthropomorphic copy. Anywhere.** No "I missed you," no "feed me," no nicknames, no first-person from the visual. Tap = no-op (no interaction screen, no character profile).
- **No notifications tied to the visual.** Ever.
- **Palette.** Pull from the existing app palette only — deep teal (`#1C5E7A`), warm coral accent (`#D97757`), ice background. No introduction of new brand colours.
- **Animation cost.** Compose Canvas + value animations only. No Lottie dependency. Total animation budget per frame: < 1 ms on a mid-range device.
- **Accessibility.** Each visual has a single `contentDescription` like "Tidal companion, stage 3 of 5." No sound. Honours `Settings → Reduce motion` by snapping between stages instead of tweening.

#### Stage progression — what each stage broadly conveys

| Stage | Trigger (whichever first) | Tone |
|---|---|---|
| 1 | Day 0–13, or no recent check-ins | starting, sparse |
| 2 | Day 14, or 3 check-ins / 14d | early shape |
| 3 | Day 28, or 6 check-ins / 14d | settled |
| 4 | Day 42, or 9 check-ins / 14d | full |
| 5 | Day 56+, or 12 check-ins / 14d | rich |

---

#### Option A — Tide pool (literal to the brand)

**Concept.** A small horizontal landscape: a tide pool at the bottom of a rocky shelf, viewed from above. As stages advance, more elements emerge in the pool — first ripples, then a pebble, a piece of seaweed, a small starfish, a final cluster of life — and the water surface becomes clearer.

**Why this option.** Literally on-brand (Tidelet → tides). Pure landscape, zero creature. Hardest to read as cute or infantilizing. Closest to "ambient header illustration" — an honest 1990s nature-app feel.

**Stage details.**
- Stage 1: empty pool, low water, single ripple animation.
- Stage 2: + one pebble at the rim.
- Stage 3: + a frond of seaweed swaying slowly (3 s loop).
- Stage 4: + a small starfish on the floor.
- Stage 5: + a cluster of barnacles on a second rock; clearer reflection on the water surface.

**Implementation.** Single Compose `Canvas` per stage, drawing layered shapes. Water surface uses a slow sine-wave shader (or a hand-rolled `Path` with two animated control points). Total assets: zero — everything Canvas-drawn so the file is one Composable + a `TidePoolStage` sealed class.

**Risk.** A complex Canvas drawing can read as "busy" on small screens. Keep elements deliberately sparse; whitespace is the dominant element at every stage.

---

#### Option B — Lantern / hearth (highest restraint)

**Concept.** A single small lantern (or candle) centred above the streak card. As stages advance, the light grows warmer, steadier, and slightly larger; the surrounding glow extends further. Skipping a day = light stays at current state, no flicker-out.

**Why this option.** Maximum minimalism. One element, no scene to compose. Reads as "a small light is on for you" — emotionally warm without being cute. Plays well with the app's existing quiet posture.

**Stage details.**
- Stage 1: small flame, narrow glow radius, cool teal-tinted light.
- Stage 2: same flame, glow extends ~20% further.
- Stage 3: flame slightly larger, glow warmer (small coral tint).
- Stage 4: flame steady, glow has a soft outer ring.
- Stage 5: full warm coral-tinted glow with a subtle pulse on a 4 s cycle.

**Implementation.** Single Composable. The flame is a small Canvas drawing (two `Path`s, animated by a noise function for flicker). The glow is a radial `Brush` on a `Box` modifier. Day/night handling: between 18:00 and 06:00 local time, the surrounding background card switches to a darker treatment so the lantern reads as a light *in the dark*. Outside those hours, lighter background. Time-of-day is a presentation concern only — no new data.

**Risk.** Easiest of the three to over-design (drop shadows, particle sparkles). Keep it to one flame + one glow. Resist the urge to add a base/holder — the abstraction is the point.

---

#### Option C — Geometric pattern (data-as-art, coolest option)

**Concept.** A growing concentric pattern, like a topographic map or rings on water. Day 0 starts with a single dot at the centre. Each stage adds a ring outward, more density of dots inside the rings, and slightly more saturation. By stage 5, the pattern is a full mandala-style composition — but always abstract, never representational.

**Why this option.** Pure abstraction. Feels like data visualisation crossed with quiet art. Zero risk of feeling infantilizing because there is no "creature" or "scene" at all — it is closer to the app's existing typographic restraint than the other two options.

**Stage details.**
- Stage 1: single dot, teal.
- Stage 2: + one ring of 6 dots around the centre.
- Stage 3: + a second ring of 12 dots; centre dot adopts a coral tint.
- Stage 4: + a third ring of 18 dots; rings get a subtle gradient.
- Stage 5: + a fourth ring of 24 dots + a faint connecting web; entire pattern rotates very slowly (one revolution per 60 s) only at this final stage.

**Implementation.** Single Compose `Canvas`. Draw each dot at polar coordinates `(r, θ)`. Stage transitions tween dot positions from "off-canvas" to their final spot, so a stage-up looks like new dots arriving from outside the visual. 100% pure code, zero assets.

**Risk.** Coldest of the three. Some users may want more warmth from a "companion." If a reviewer says "this feels like a chart, not a companion," that's the right read — that's Option C's identity. Decide whether that distance is wanted or not.

---

#### Decision criteria (how to choose after seeing all three)

When all three are running side-by-side on a real device, weigh them on:

1. **Does it feel like Tidelet, or like a plug-in from another app?** A tells the brand story; B fits the existing tonal restraint; C extends the typographic rigour.
2. **Is anyone tempted to talk about "it"?** ("Look at my…") If yes for B or C, they have failed the no-anthropomorphism test. A is intentionally more landscape-than-character.
3. **At day 1, does the user feel encouraged or judged?** All three should read as "we're here for you," not "you have a long way to go."
4. **At day 400, does it still feel proportionate?** Stage 5 is the long tail. If stage 5 looks triumphant, it's wrong; it should look settled.

**Default if no clear preference emerges:** Option B (lantern). Lowest design surface area, least risk of any kind of "wrong" read. Option A is the fallback if B feels too plain. Option C is the fallback if both A and B feel too sentimental.

---

#### Must pass before implementing all three

The opt-in toggle, the shared `CompanionVisual` interface, the `highestStageEverReached` DataStore key, and the stage-progression util are all built first as a single PR. Then each option lands as its own follow-up PR (one Composable file per option). After the third option PR, raise a review PR with screenshots/screen recordings of all three at stages 1, 3, and 5. Pick one; delete the other two; remove the feature flag.

If during review **any reviewer calls any option infantilizing**, that option is killed. If all three are killed, B1 falls back to Tier C.

#### Acceptance (per option)

- Manual: opt-in via Settings → companion appears at stage 1. Set start date 14 days back → stage 2. Set 56 days back → stage 5. Reset to day 0 → stage stays at 5 (high-water mark holds).
- Manual: skip check-ins for 14 days at stage 3 → stage stays at 3 (no wilt, no flicker, no copy change).
- Manual: enable "Reduce motion" → stage transitions snap rather than tween.
- Unit: `CompanionStageTest::never decreases when streak resets`, `::high-water mark persists across cold starts`, `::reduce-motion path skips animation`.
- Visual regression (screenshot tests): one screenshot per option per stage (3 options × 5 stages = 15 screenshots). These are the artefacts used in the side-by-side review.

### 9B2. Variable-reward insight cards

**Intent.** Occasional (not daily) unexpected cards on Home — a quote, a recovery-science fact, or a pattern drawn from the user's own logs ("Your last three cravings were in the 6–8pm window"). Reinforces dopamine variability without daily-spinner mechanics.

**Surface.** Home card slot, below streak and above the SOS button. Appears at most every 2–4 days, probabilistically. Dismissible.

**Data-layer impact.**
- New Room entity `InsightCardShown(cardKey, shownAtEpochMillis)` to avoid repeating the same card within 60 days.
- Static card inventory in `strings.xml` under `insight_<key>_title` / `_body` — seed ~30 cards split across quote / science / user-pattern categories.

**How.**
- On Home open, if (`lastInsightAt > 2 days ago` AND `random() < dailyShowProbability` ~ 0.3) → pick a card that hasn't been shown in 60 days. User-pattern cards only fire when the underlying data meets a threshold (e.g., ≥5 cravings logged to run time-of-day analysis).
- **Category mix across a 30-day window:** target ~50% quotes, ~30% recovery science, ~20% user-pattern. If user-pattern can't be produced (insufficient data), the slot falls back to quote/science — never empty-shows.
- **No notification.** Cards are only seen when the user opens the app organically.

**Acceptance.**
- Manual: with seeded "last shown = never", open Home ~10 times across a few days with the time clock advanced → at least 2 and at most 5 cards seen (tuning-dependent).
- Manual: dismiss a card → same card does not reappear within 60 simulated days.
- Unit: `InsightCardSelectorTest::never repeats within 60d`, `::user-pattern cards require ≥5 craving events`, `::fallback category picked when pattern unavailable`.

---

## Quick-reference: mapping to the research report

| Research ID | Phase 2 §  | One-line recap |
|---|---|---|
| A1 | §9A1 | Health-recovery timeline on Stats |
| A2 | §9A2 | Hours reclaimed on Stats |
| A3 | §9A3 | Milestone letter nudge (extends §7) |
| A4 | §9A4 | Streak grace language + lifetime dry days |
| A5 | §9A5 | Evening mini-review |
| B2 (research) | §9B1 | Compassionate visual companion |
| B4 (research) | §9B2 | Variable-reward insight cards |

See [gamification_research.md](../gamification_research.md) for the full landscape review, evidence base, and the Tier C/D items explicitly held back.

---

## 10. Follow-up batch — user feedback round (2026-04-23)

Six items raised during a user test pass: two bugs, one discoverability regression, and three new features. The bugs (§10.1, §10.2) should land **before** any §9 v-next work — they degrade the current experience. The discoverability item (§10.3) requires investigation first (may already be fixed in code; may be UX-only). The three features (§10.4–§10.6) are net-new surfaces and should ship in order of size: heatmap, then the two static content pages.

**Suggested build order within §10:**

1. §10.1 Keyboard overlap (tiny, user-blocking)
2. §10.2 Distractions non-repetition (tiny)
3. §10.3 Journal entries discoverability (investigate → fix or UX)
4. §10.5 Resources page (smaller static content)
5. §10.6 Getting started guide (slightly larger static content)
6. §10.4 Craving heatmap in Log tab (largest — includes a schema migration)

---

### 10.1. Bug — keyboard overlaps text input on long entries

**Why.**
User reports that when writing a long Journal entry (and likely Thought Check, Evening Review, Milestone Letter, Relapse Plan — any multi-line TextField), the soft keyboard covers the cursor. The user can't see what they're typing past a certain point.

**Root cause (likely).**
Default Compose behaviour without IME-aware insets. One or both of:
- The Activity's `android:windowSoftInputMode` in the manifest isn't `adjustResize`, or edge-to-edge is on and the insets aren't being consumed by the content.
- The text-entry Composables aren't using `Modifier.imePadding()` / `Modifier.imeNestedScroll()`, so the content isn't scrolling to keep the caret visible when the IME opens.

**Where to fix.**
- [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) — confirm `android:windowSoftInputMode="adjustResize"` on `MainActivity`.
- [MainActivity.kt](app/src/main/java/com/tidelet/app/MainActivity.kt) — confirm `enableEdgeToEdge()` is present (standard since Compose 1.6). If present, the content root needs `Modifier.imePadding()` **or** the text-entry screens need it individually.
- Per-screen application of `Modifier.imePadding()` on the outer Column + wrapping long TextFields in a `verticalScroll` + `bringIntoViewRequester` so the caret stays visible as the user types past the fold:
  - [JournalScreen.kt](app/src/main/java/com/tidelet/app/ui/sos/JournalScreen.kt)
  - [ThoughtCheckScreen.kt](app/src/main/java/com/tidelet/app/ui/sos/ThoughtCheckScreen.kt)
  - [EveningReviewScreen.kt](app/src/main/java/com/tidelet/app/ui/checkin/EveningReviewScreen.kt)
  - [MilestoneLetterScreen.kt](app/src/main/java/com/tidelet/app/ui/milestones/MilestoneLetterScreen.kt)
  - [RelapsePlanScreen.kt](app/src/main/java/com/tidelet/app/ui/cbt/RelapsePlanScreen.kt)
  - Any screen with a multi-line `OutlinedTextField` or `TextField`. Grep: `minLines = [^1]` / `maxLines = ` / `singleLine = false`.

**How.**
Pattern to apply per screen:

```kotlin
val bringIntoViewRequester = remember { BringIntoViewRequester() }
val scope = rememberCoroutineScope()

Column(
    modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(...)
) {
    // ...
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { if (it.isFocused) scope.launch { bringIntoViewRequester.bringIntoView() } },
        minLines = 8,
    )
}
```

Alternative (simpler, less precise): wrap the whole screen content in a `Scaffold` with `Modifier.imePadding()` applied to its `Modifier` param, and rely on `verticalScroll` to keep the focused TextField in view. Start here; escalate to the `bringIntoViewRequester` pattern only if focus-jumping is still jumpy on real devices.

**Acceptance.**
- Manual: open Journal → tap the text box → type until the caret would be behind the keyboard → screen auto-scrolls so caret stays visible. Rotate device → same behaviour in landscape.
- Manual: repeat for Thought Check, Evening Review, Milestone Letter, Relapse Plan. Every multi-line editor behaves the same.
- UI test (per screen): `JournalScreenImeTest::caret stays in view when typing past fold` — using `mainClock` + `setSoftKeyboard` equivalent hooks.

---

### 10.2. Bug — "Do something else" shows the same suggestion twice in a row

**Why.**
User reports tapping the shuffle button sometimes returns the same distraction they're already looking at. This is a pure-random miss, not a planned feature.

**Root cause.**
[DistractionsScreen.kt](app/src/main/java/com/tidelet/app/ui/sos/DistractionsScreen.kt) line 118:

```kotlin
onClick = { pickedIndex = Random.nextInt(items.size) }
```

Pure uniform pick with no memory. Over N taps, collision rate with the previous pick is `1/items.size` per tap.

**Fix — stronger variant (recommended).**
Maintain a rolling "recently shown" set and pick outside it, reshuffling only when the set covers the whole inventory. This also feels less repetitive across a longer session.

```kotlin
var pickedIndex by remember { mutableIntStateOf(-1) }
val recentlyShown = remember { mutableStateListOf<Int>() }
val recentCap = (items.size - 1).coerceAtLeast(1)  // keep at least one candidate

fun pickNext() {
    val pool = items.indices.filter { it !in recentlyShown }
    val next = if (pool.isNotEmpty()) pool.random() else items.indices.random()
    pickedIndex = next
    recentlyShown.add(next)
    while (recentlyShown.size > recentCap) recentlyShown.removeAt(0)
}

Button(onClick = ::pickNext, ...) { ... }
```

**Fallback — minimum viable fix.**
If the coding agent wants to keep the diff tiny, at least exclude the current `pickedIndex`:

```kotlin
onClick = {
    val pool = items.indices.filter { it != pickedIndex }
    pickedIndex = if (pool.isNotEmpty()) pool.random() else Random.nextInt(items.size)
}
```

This satisfies the literal user request ("a different option each time you press it"). The stronger variant is preferred because the inventory is small and pure-random feels repetitive well before it literally repeats.

**Where.**
- [DistractionsScreen.kt](app/src/main/java/com/tidelet/app/ui/sos/DistractionsScreen.kt) — the `pickedIndex` state and the shuffle button's `onClick`.
- Consider hoisting the rotation logic into `DistractionsViewModel` so the state is testable and survives configuration changes. The current VM is thin and the list is owned by the Composable via `stringArrayResource`; moving just the rotation bookkeeping up is fine without moving the list.

**Acceptance.**
- Manual: tap shuffle 20 times with a 14-item `builtin_distractions` array → no two consecutive picks match. (Stronger variant: no repeat within any 13-tap window.)
- Unit (prefer pushing logic into VM for this): `DistractionRotatorTest::never picks same index twice in a row`, `::eventually covers the full inventory before repeating any index`.

---

### 10.3. Journal entries aren't accessible — investigate, then fix

**Why.**
User reports that Journal entries they've written are "not visible." Needs investigation — the Journal bottom-nav tab is wired (`Routes.JOURNAL` → `JournalHubScreen` → `JOURNAL_ENTRIES` → `JournalListScreen`) and the read-side screens exist, so this could be one of three different things. Each has a different fix.

**Three candidate diagnoses (run in order).**

**(a) Data-binding break — most likely.**
`JournalScreen.kt` (write-side, under `ui/sos/`) writes to a repository method. `JournalListScreen.kt` (read-side, under `ui/journal/`) subscribes to a flow. If those two aren't pointing at the same `JournalEntry` table / flow, entries vanish.

*Diagnostic.* Grep both files for every repo method they call. The write-side save and the read-side observe must touch the same `JournalEntry` rows. Write a test entry, dump the DB via `adb shell` + `sqlite3` on the device, and confirm the row exists.

*Fix, if broken.* Re-point whichever side is wrong. No schema change — both already model the same entity.

**(b) Discoverability — second-most likely.**
The user may not know the Journal bottom-nav tab is where their entries live. The icon (`Icons.Rounded.AutoStories`) may not read as "journal" to every user. The write screen is reached via SOS, which is a very different mental model from where entries are then reviewed.

*Fix (UX-only, no bug).* On the write-side `JournalScreen`, add a small "View past entries →" link at the top, navigating to `Routes.JOURNAL_ENTRIES`. On save, show a brief snackbar: "Saved. View all entries in the Journal tab." No changes to the data path.

**(c) Empty-state bug.**
`JournalListScreen` may show an unhelpful empty state (blank screen) even when entries exist but are filtered out by some query condition (wrong date range, wrong user, etc.). Would look to the user like "my entries aren't there."

*Fix.* Regardless of which diagnosis wins, the empty state for `JournalListScreen` should explicitly say "No journal entries yet. Write one from SOS → Journal → Write freely." with a nav link.

**Build order.**
Coding agent must reproduce first. Do not jump to a fix.

1. Reproduce: write 3 journal entries via SOS → Journal → Write freely. Open the Journal tab → JournalListScreen. Count entries.
2. If count matches, diagnosis (a) is ruled out. Fix is (b) + (c) — discoverability affordances.
3. If count doesn't match, diagnosis (a) is the real bug. Fix the data path, then still apply (b) + (c) for robustness.

**Where.**
- [JournalScreen.kt (write-side, ui/sos/)](app/src/main/java/com/tidelet/app/ui/sos/JournalScreen.kt) — repo write call; add the "View past entries" link.
- [JournalListScreen.kt (read-side, ui/journal/)](app/src/main/java/com/tidelet/app/ui/journal/JournalListScreen.kt) — repo read subscription; empty-state copy.
- [JournalListViewModel.kt](app/src/main/java/com/tidelet/app/ui/journal/JournalListViewModel.kt) — verify it observes the real entries flow, not a stub.

**Acceptance.**
- Write three entries → Journal tab → all three appear in reverse-chronological order.
- Empty state (with no entries) explicitly names where to write from, and the link works.
- Snackbar on save mentions the Journal tab.
- UI test: `JournalEndToEndTest::entries written in write-side appear in read-side`. This is the canonical regression test and should stay green forever.

---

### 10.4. Craving heatmap in the Log tab

**Why.**
The user wants to see how craving **frequency** and **intensity** shift over time, visually. A calendar-grid heatmap (GitHub-contribution style) is the established pattern for "days × density" data and reads in 2 seconds. Pattern recognition is the primary therapeutic value — a user who sees "most of my cravings cluster on weekday evenings" has just done the majority of functional-analysis work without a worksheet.

**What.**
A new card at the top of [LogScreen.kt](app/src/main/java/com/tidelet/app/ui/log/LogScreen.kt), above the existing check-in history. Shows the last ~12 weeks as a grid:

- **Grid layout.** 7 rows (days of week, Mon–Sun) × ~12 columns (weeks). Roughly 350 × 120 dp. Each cell is a rounded 12 dp square with 2 dp gap.
- **Cell colour.** Derived from *both* drivers: frequency (how many craving events that day) and intensity (mean intensity per event).
  - Base hue: existing teal (`#1C5E7A`).
  - Alpha scales with `clamp(count / 3, 0f, 1f)` — 0 events = transparent, 3+ events = full opacity.
  - Saturation shifts toward the warm coral accent as mean intensity approaches 10 — so a single-high-intensity day reads different from three-mild-intensity days.
- **Axes.** Day-of-week labels on the left (M/T/W/T/F/S/S, single letters). Month labels above the columns where a month boundary sits.
- **Tap a cell.** Filters the check-in list below to that day *and* shows the craving events for that day in a small bottom sheet (tool used + intensity + outcome for each event). Tap elsewhere to clear the filter.

**Data-layer impact — this is the biggest piece of §10.**

`CravingEvent` today has `id, timestampEpochMillis, tool, outcome` — no intensity field. Adding the heatmap requires:

1. **Schema migration.** Add `intensity: Int?` (nullable, 1–10, null = not captured) to `CravingEvent`. Bump DB version; destructive migration per Phase 2 policy is acceptable for now.
2. **Capture point.** Add an intensity slider (1 = "mild itch", 10 = "overwhelming") to the SOS entry point or to Ride the Wave's opening screen. Capture is **optional** — skip button present; heatmap handles `null` intensity by treating it as mean of captured values for that day or defaulting to 5 if the day has no captured intensities.
3. **New repo query.** `fun cravingEventsByDay(fromDate, toDate): Flow<Map<LocalDate, DayCravingSummary>>` where `DayCravingSummary(count: Int, meanIntensity: Double?, events: List<CravingEvent>)`. Computed in-memory from the existing `cravingEvents` flow.
4. **Export/import.** `intensity` round-trips through Markdown export (extends §2 contract).

**Where.**
- [app/src/main/java/com/tidelet/app/data/db/CravingEvent.kt](app/src/main/java/com/tidelet/app/data/db/CravingEvent.kt) — add `intensity: Int?` field, default null.
- [app/src/main/java/com/tidelet/app/data/db/Daos.kt] — new `observeCravingsByDay(from, to)` DAO query, or derive in the repository from the existing flow.
- `TideletRepository.kt` (interface + Room impl + fake) — expose the byDay summary.
- New `app/src/main/java/com/tidelet/app/ui/log/CravingHeatmapCard.kt` — pure-Compose Canvas drawing the grid from a `Map<LocalDate, DayCravingSummary>`. Self-contained.
- [LogViewModel.kt](app/src/main/java/com/tidelet/app/ui/log/LogViewModel.kt) — expose `val heatmapData: StateFlow<HeatmapData>` and `fun selectDay(date: LocalDate?)` for the filter behaviour.
- [LogScreen.kt](app/src/main/java/com/tidelet/app/ui/log/LogScreen.kt) — insert the card above the check-in history; wire the filter.
- New `app/src/main/java/com/tidelet/app/ui/sos/IntensityCapture.kt` or inline into [SosScreen.kt](app/src/main/java/com/tidelet/app/ui/sos/SosScreen.kt) — the 1–10 slider shown once when SOS is opened. Skippable.
- [ExportImportManager.kt](app/src/main/java/com/tidelet/app/data/export/ExportImportManager.kt) — extend `CravingEvent` encoding with the new field.

**How — coloring function.**

```kotlin
fun cellColor(count: Int, meanIntensity: Double?): Color {
    if (count == 0) return Color.Transparent
    val alpha = (count / 3f).coerceIn(0f, 1f) * 0.85f + 0.15f
    val warmth = ((meanIntensity ?: 5.0) / 10.0).coerceIn(0.0, 1.0).toFloat()
    return lerp(tealBase, coralBase, warmth * 0.6f).copy(alpha = alpha)
}
```

`warmth * 0.6f` caps the hue shift so even a 10/10 intensity day doesn't fully lose the brand teal.

**Build staging.**

1. Add `intensity` field, migration, repo query. No UI yet.
2. Build `CravingHeatmapCard` with a test data source. Land the card into Log tab. No intensity capture yet → heatmap uses nulls, renders frequency-only.
3. Add intensity capture in SOS flow. Heatmap starts showing the saturation shift.
4. Wire the tap-to-filter behaviour.

Each step independently reviewable.

**Non-goals.**
- No month-grid calendar view. Week-column grid is chosen because pattern-by-weekday is the most common insight ("weekday evenings", "Saturdays"), and a month grid hides that axis.
- No year-level zoom. 12 weeks is enough to see a pattern without overwhelming the screen.
- No sharing/export of the heatmap as an image. Privacy.

**Acceptance.**
- Manual: seed 40 craving events across 6 weeks with varying intensity → heatmap renders with visibly differing cells. A day with 5 events at intensity 8 reads distinctly warmer than a day with 1 event at intensity 2.
- Manual: tap a cell → check-in list below filters; bottom sheet shows that day's events. Tap elsewhere → filter clears.
- Unit: `CravingDaySummaryTest::groups events into day buckets`, `::meanIntensity ignores nulls`.
- Unit (rendering): `CellColorTest::count=0 is transparent`, `::count=3 is full-alpha`, `::high intensity shifts toward coral`.
- Integration: intensity field round-trips through export/import.

---

### 10.5. Resources page

**Why.**
Tidelet is explicitly not a substitute for medical care. When a user needs a crisis line, a clinician, a support group, or deeper reading, the app must hand them off cleanly. Today there is nowhere inside the app to find any of that. Even users who don't need crisis resources often want pointers — "what's a good book on this?" — and leaving the app to Google is a worse experience than giving them a curated list.

**What.**
A new read-only screen accessed from Settings → "Resources." Three sections, each a small set of rows with title, one-line description, and a tap-through.

**Section 1 — When you need help right now.**
- SAMHSA National Helpline (US) — 1-800-662-HELP (`tel:18006624357`)
- 988 Suicide & Crisis Lifeline (US) — 988 (`tel:988`)
- Locale-aware: if `Locale.getDefault().country` is `GB`, show Samaritans 116 123; if `IN`, show iCall 9152987821; if `AU`, Lifeline 13 11 14. Fall back to SAMHSA/988 for unrecognised locales, with a line "If you're outside these regions, your local emergency number is usually the fastest route."

**Section 2 — Non-profit support communities.**
- SMART Recovery — [smartrecovery.org](https://www.smartrecovery.org)
- Alcoholics Anonymous — [aa.org](https://www.aa.org)
- Moderation Management — [moderation.org](https://www.moderation.org) (for users whose goal isn't abstinence)
- Reddit r/stopdrinking — [reddit.com/r/stopdrinking](https://www.reddit.com/r/stopdrinking)

**Section 3 — Further reading and science.**
- NIAAA "Rethinking Drinking" — [rethinkingdrinking.niaaa.nih.gov](https://rethinkingdrinking.niaaa.nih.gov)
- This Naked Mind (Annie Grace) — book pointer, no affiliate link
- Alcohol Explained (William Porter) — book pointer, no affiliate link
- NIAAA's Treatment Navigator — [alcoholtreatment.niaaa.nih.gov](https://alcoholtreatment.niaaa.nih.gov)
- A plain-English primer on cognitive distortions (Psychology Tools, open article)

**Data source.**
Seed content lives in `app/src/main/res/raw/resources.json` (or a Kotlin `ResourceCatalog.kt`) so the PM can edit without touching code. Structure:

```kotlin
data class ResourceLink(
    val sectionKey: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val url: String,       // https or tel:
    val countryFilter: List<String> = emptyList(), // empty = global
)
```

**Where.**
- New `app/src/main/java/com/tidelet/app/ui/resources/ResourcesScreen.kt`, `ResourcesViewModel.kt`.
- [SettingsScreen.kt](app/src/main/java/com/tidelet/app/ui/settings/SettingsScreen.kt) — add a "Resources" row in the About/Help section.
- [Routes.kt (TideletNav.kt)](app/src/main/java/com/tidelet/app/ui/nav/TideletNav.kt) — add `const val RESOURCES = "resources"`.
- External link handling: open URLs via `Intent.ACTION_VIEW` with `Uri.parse(url)`. `tel:` links open the dialer via the same mechanism — do **not** place the call for the user.

**Privacy constraints.**
- **No link attribution or tracking.** URLs are plain — no utm params, no redirectors.
- **No embedded WebView.** All taps leave the app via the OS browser / dialer.
- **No search.** The list is curated; any find-a-therapist functionality requires a network call and is out of scope.
- Show a one-line hint at the top: "These links open in your browser. Tidelet doesn't share your data with them."

**Top-level disclaimer.**
At the top of the screen, one sentence: "Tidelet is a self-help tool, not a medical service. If you need care, the links below are where to start."

**Non-goals.**
- No paid-service listings. No rehab-center affiliate content ever.
- No promoted link ordering. Sections are curated alphabetically within each group (by title).

**Acceptance.**
- Manual: Settings → Resources → all three sections render; tap a link → OS browser opens correct URL. Tap a `tel:` link → dialer opens with the correct number.
- Manual: set system locale to GB → locale-specific helpline appears; to IN → different helpline appears. Default (e.g. DE) → falls back to SAMHSA with the "outside these regions" note.
- Unit: `ResourceCatalogTest::loads expected sections`, `::countryFilter picks correct helpline`.

---

### 10.6. Getting started — best-practices guide

**Why.**
The onboarding flow gets users to day 1, but doesn't teach them *how the app actually helps*. Users who poke around discover features individually; users who don't, don't. A short guide — accessible from Settings and optionally from the end of onboarding — tells the user: here's when to open SOS, here's when to log a check-in, here's what the Journal is for, here's how to read your Stats.

**What.**
A single scrollable screen. Structured as short sections (2–4 sentences each, not chapters):

1. **Day 1 — what to do first.** Pick a visible start date (already done in onboarding). Bookmark the SOS button's location. Everything else can wait.
2. **When a craving hits.** Tap SOS. Pick one tool. The point isn't finishing a specific tool — it's buying 15 minutes.
3. **Daily check-in (Log tab).** Two minutes, same time each day. Fill it in honestly, not aspirationally.
4. **Journal & Thought Check (Journal tab).** Use Journal for open-ended writing, Thought Check for stuck thought loops. Both are private and searchable.
5. **Stats — reading the pattern.** The heatmap shows *when* cravings cluster. The money-saved and hours-reclaimed cards compound slowly — check them weekly, not daily.
6. **Settings — making the app yours.** Edit drinking baseline, back up your data, turn on evening mini-review if you want a second daily touchpoint.
7. **When you need more than this app.** Link to §10.5 Resources page.

**Copy principle.**
Written in the same quiet voice as the rest of the app. Not motivational. Not corporate. Short paragraphs; no bullet lists with twelve items each.

**Where.**
- New `app/src/main/java/com/tidelet/app/ui/help/GettingStartedScreen.kt` (no ViewModel — pure static content).
- Content in `strings.xml` or in-code `@Composable` text blocks — keep it editable by PM either way.
- [SettingsScreen.kt](app/src/main/java/com/tidelet/app/ui/settings/SettingsScreen.kt) — add "How to use Tidelet" row in About/Help section, above Resources.
- [OnboardingScreen.kt](app/src/main/java/com/tidelet/app/ui/onboarding/OnboardingScreen.kt) — at the end of the flow, add a final screen with a single "Read a quick guide" button (default action) plus "Skip and start" (default-focused for users who want to jump in). Tapping "Read a quick guide" navigates to `GettingStartedScreen` and returns to Home on back.
- [Routes.kt (TideletNav.kt)](app/src/main/java/com/tidelet/app/ui/nav/TideletNav.kt) — add `const val GETTING_STARTED = "help/getting-started"`.

**Non-goals.**
- No interactive walkthrough (no "tap here next" coaching overlays). The guide is a page, not a product tour.
- No progress gamification ("you've read 3/7 sections"). It's a help doc, not a quest.
- No inline video. Text only — keeps the bundle small and translation simple.

**Acceptance.**
- Manual: Settings → How to use Tidelet → guide renders; scroll through all 7 sections. "Resources" link at the bottom navigates to §10.5.
- Manual: new install → onboarding end screen → "Read a quick guide" → guide → Home. "Skip and start" → Home directly.
- Screenshot test: one baseline screenshot of the guide at default text scale + one at 2× accessibility scale (confirms readability under large-type settings).

---

## Priority order summary — all of Phase 2 + follow-ups

Updated end-to-end ordering including §9 and §10:

1. §1 Breathe bug
2. §10.1 Keyboard overlap bug *(high user impact, tiny diff)*
3. §10.2 Distractions non-repetition bug *(tiny)*
4. §10.3 Journal discoverability *(investigation-first)*
5. §2 Export/import tests + Verify backup
6. §3 Settings baseline
7. §4 Thought Pattern antidotes
8. §6 Local-only usage counters
9. §5 Log review screens
10. §7 Milestone letter-to-self
11. §10.5 Resources page *(small static content)*
12. §10.6 Getting started guide *(small static content)*
13. §10.4 Craving heatmap *(largest §10 item; includes migration)*
14. §8 Home-screen widget
15. §9 v-next engagement layer (A4 → A1 → A2 → A3 → A5 → B2 → B1)
