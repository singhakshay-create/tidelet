# Changelog

All notable changes to Tidelet are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Check-in reminders — a daily local notification prompting a check-in. The preference fields (time, on/off) already exist in the data layer, but there is no Settings UI or scheduled notification yet, so this is currently dead configuration rather than a working feature.
- Cut-back / moderation mode — a goal mode for users aiming to reduce rather than quit entirely, alongside the existing quit-mode goal.

## [0.1.0] - 2026-09-24

Initial public pre-release. Tidelet is fully offline: no accounts, no backend, no analytics, and no network permissions in the Android manifest — everything below runs and stores its data entirely on-device.

### Added

- **Onboarding** — a short first-run flow to set a sobriety start date and quit-mode goal.
- **Home** — day counter with progress toward the next milestone, and quick access to the SOS toolkit.
- **Craving SOS toolkit**:
  - *Ride the Wave* — a 15-minute urge-surfing timer with an animated wave and a choice of ambient soundscapes (Ocean / River / Rain / Silent).
  - *Breathe* — a 4-4-4-4 box-breathing exercise with a cycle count.
  - *My Reasons* — a personal, editable list of reasons to reread in a hard moment.
  - *Do Something Else* — a curated list of distraction ideas, with a "pick one for me" shuffle.
  - *Thought Check* — a CBT-style thought-record tool for examining a craving-related thought in the moment.
  - *Journal* — free-form writing during or after a craving.
  - *Compassionate Reset* — logs a slip ("I drank") without punishing language or resetting the user's history; the streak counter and past entries are never erased.
- **CBT tools** — a cognitive-distortions library with antidotes for each distortion, a refusal-rehearsal exercise, and a relapse-prevention plan.
- **Log** — a daily check-in form and a craving heatmap.
- **Journal history** — read-only browsing of past Thought Checks (grouped by distortion) and Journal entries, plus a list of evening reviews.
- **Weekly reflection** — a guided end-of-week check-in.
- **Milestone letters** — write a letter to your future self at a milestone, unlocked for rereading at a later one.
- **Stats** — streak statistics and a health/recovery timeline.
- **Settings** — customizable drinking baseline (typical drinks/day and price, used in money-saved calculations, formatted in the device's local currency), full data export/import, and a one-tap "verify my backup" self-check.
- **Resources** — crisis lines (988 and the SAMHSA National Helpline in the US, plus country-specific numbers for the UK, India, and Australia), community resources (Alcoholics Anonymous, SMART Recovery, Moderation Management, r/stopdrinking), and further reading — all opened via the device's browser or dialer, not fetched by the app.
- **Getting started guide** — an in-app orientation to the tools and their intended use.
- **Home-screen widget** — shows the current streak.
