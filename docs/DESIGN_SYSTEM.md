# Tidelet — Design System

Version 0.1 (refresh). Supersedes the first-draft palette in `ui/theme/{Color,Theme,Type}.kt`. Treat this document as the single source of truth; code should follow it, not the other way round.

This guide covers colour, typography, spacing, shape, motion, iconography, component recipes, per-screen guidance, voice, and accessibility. It is the specification a coding agent will follow when it's time to implement the refresh.

---

## 1. Design principles

Five sentences you can read in 30 seconds and apply to any decision.

1. **Quiet over loud.** If a screen doesn't need a colour, it doesn't get one. Saturation is earned, not spent.
2. **Warm over cold.** Neutrals lean ivory and sage, not blue-white and steel. Tidelet is a companion, not a dashboard.
3. **Words over widgets.** Copy is primary UI. If a label can replace a glyph, remove the glyph.
4. **Respect over reward.** No streaks as shame. No gamification. No exclamation marks. The user is already a grown adult.
5. **Private by default.** Every design choice should reinforce the core product promise: your data never leaves your phone.

These principles are tie-breakers. When two options both "look fine", pick the quieter one.

---

## 2. Colour

Tidelet uses **two semantic families** that share a single neutral foundation:

- **Calm** — the default everywhere. Deep sage primary + warm ivory background.
- **Warm** — reserved for the SOS flow only. Terracotta accent + warm-sand surface.

The two families never appear on the same surface. The SOS palette signals "you are in a moment that matters" — using it elsewhere dilutes the signal.

### 2.1 Light mode tokens

| Token                      | Hex                   | Use                                               | Contrast vs background |
|----------------------------|-----------------------|---------------------------------------------------|------------------------|
| `background`               | `#FBF9F5` warm ivory  | Whole-screen default                              | —                      |
| `surface`                  | `#FFFFFF`             | Raised cards, sheets, inputs                      | —                      |
| `surface-raised`           | `#FFFFFF` + border    | Cards: `border(1dp, border-subtle)`, no shadow    | —                      |
| `surface-sunken`           | `#F5F2EC`             | Muted groupings (e.g. Settings section backs)     | —                      |
| `border-subtle`            | `#E8E4DB`             | 1dp separators, card outlines                     | —                      |
| `border-emphasis`          | `#D4CEC1`             | Focused input borders                             | —                      |
| `ink` / `on-surface`       | `#1A1C1A`             | Primary body text                                 | 16.3:1 ✓ AAA           |
| `text-secondary`           | `#404340`             | Secondary body, sub-labels                        | 10.8:1 ✓ AAA           |
| `text-muted`               | `#6B6F6B`             | Tertiary, captions, helper text                   | 4.85:1 ✓ AA            |
| `primary` (sage)           | `#2E3D35`             | Primary buttons, headings, brand moments          | 10.7:1 ✓ AAA           |
| `on-primary`               | `#F5F3EE`             | Text on sage buttons                              | 10.3:1 ✓ AAA           |
| `primary-container`        | `#E6EBE5`             | Tonal chips, soft primary backgrounds             | —                      |
| `on-primary-container`     | `#1A221D`             | Text on `primary-container`                       | 13.0:1 ✓ AAA           |
| `accent-warm` (terracotta) | `#C26B5A`             | **SOS flow only.** Accents, outlines.             | 3.6:1 (decorative/large text only) |
| `sos-surface`              | `#FAE8E2` warm sand   | SOS screens: whole-screen background              | —                      |
| `on-sos-surface`           | `#7A342B`             | Text on `sos-surface`                             | 7.4:1 ✓ AAA            |
| `sos-accent-container`     | `#F0D2C7`             | Chips / pill toggles inside SOS                   | —                      |
| `success`                  | `#5A8560`             | Positive confirmations, kept-streak indicator     | 4.9:1 ✓ AA             |
| `error`                    | `#A83E34`             | Destructive actions, validation errors            | 6.1:1 ✓ AA             |

### 2.2 Dark mode tokens

| Token                      | Hex                   | Use                                               | Contrast vs background |
|----------------------------|-----------------------|---------------------------------------------------|------------------------|
| `background`               | `#0F1110` warm black  | Whole-screen default                              | —                      |
| `surface`                  | `#181A19`             | Raised cards, sheets                              | —                      |
| `surface-raised`           | `#22241F`             | Second-level elevation (dialogs, menus)           | —                      |
| `border-subtle`            | `#2A2D2A`             | 1dp separators                                    | —                      |
| `ink` / `on-surface`       | `#ECECEA`             | Primary body text (never pure white)              | 15.9:1 ✓ AAA           |
| `text-secondary`           | `#B8BAB6`             | Secondary body                                    | 9.5:1 ✓ AAA            |
| `text-muted`               | `#8C908B`             | Captions, helper text                             | 5.8:1 ✓ AA             |
| `primary` (pale sage)      | `#A3BEB0`             | Primary buttons (text colour), links              | 9.0:1 ✓ AAA            |
| `on-primary`               | `#0F1110`             | Text on pale-sage buttons                         | —                      |
| `primary-container`        | `#263028`             | Tonal sage backgrounds                            | —                      |
| `on-primary-container`     | `#D5E2D8`             | Text on `primary-container`                       | 10.9:1 ✓ AAA           |
| `accent-warm` (terracotta) | `#D89482`             | SOS accents in dark mode                          | 6.7:1 ✓ AA             |
| `sos-surface`              | `#2A1A16`             | SOS whole-screen background                       | —                      |
| `on-sos-surface`           | `#F0D2C7`             | Text on `sos-surface`                             | 10.0:1 ✓ AAA           |
| `success`                  | `#8CBA92`             | Positive indicators                               | 8.7:1 ✓ AAA            |
| `error`                    | `#E59083`             | Destructive actions, validation errors            | 7.4:1 ✓ AAA            |

All contrast ratios are calculated against the token's natural background (`background` for top-level text, `sos-surface` for SOS text, etc.). Text tokens all pass WCAG AA for normal text (≥ 4.5:1). Decorative tokens like `accent-warm` in light mode sit below 4.5:1 and are restricted to large text, icons ≥ 3pt stroke, and non-text UI per WCAG 1.4.11.

### 2.3 Rules

- **Never put calm + warm on the same surface.** A teal button on a SOS-surface screen, or a terracotta button on the calm background, breaks the semantic.
- **No raw colour usage.** Compose code accesses `MaterialTheme.colorScheme.*` or `TideletTheme.extended.*`. A screen that imports a hex literal is a bug.
- **No gradients in v1.** They age fast and fight the minimal aesthetic.
- **Elevation is a last resort.** Default to `border(1.dp, border-subtle)` for raised surfaces; reserve `elevation` for sticky/scrim cases (bottom sheets, snackbars).

---

## 3. Typography

### 3.1 Families

- **Inter** (variable, OFL) — primary family. Self-hosted at `app/src/main/res/font/inter_variable.ttf`. Loaded via `FontFamily(Font(R.font.inter_variable, variationSettings = ...))`.
- **Instrument Serif** (OFL) — accent family, used *only* for milestone moments: the streak hero numeral on Home, letter-to-self reveals, and the landing page headline. Self-hosted alongside Inter.
- **Fallback** — system sans-serif, so rendering never fails.

Do not introduce a third family. A second typeface is already a budget; a third is a tax.

### 3.2 Scale

Modular scale, ratio 1.25. Sizes in `sp`.

| Slot              | Size | Line-height | Weight      | Letter-spacing | Use                                     |
|-------------------|------|-------------|-------------|----------------|-----------------------------------------|
| `displayLarge`    | 72   | 80          | 600 SemiBold| -0.02em        | Streak hero, landing hero number        |
| `displayMedium`   | 56   | 64          | 600         | -0.02em        | Milestone rewards, reveal cards         |
| `displaySmall`    | 40   | 48          | 600         | -0.02em        | Large section intros                    |
| `headlineLarge`   | 32   | 40          | 600         | -0.01em        | Screen titles                           |
| `headlineMedium`  | 24   | 32          | 600         | 0              | Major section headings                  |
| `titleLarge`      | 20   | 28          | 600         | 0              | Card titles, top-bar titles             |
| `titleMedium`     | 16   | 24          | 500 Medium  | 0              | Minor headings, list titles             |
| `bodyLarge`       | 16   | 24          | 400 Regular | 0              | Primary body text                       |
| `bodyMedium`      | 14   | 20          | 400         | 0              | Secondary body, helper text             |
| `labelLarge`      | 14   | 20          | 500         | +0.01em        | Button labels, tabs                     |
| `labelMedium`     | 12   | 16          | 500         | +0.02em        | Chips, small badges                     |
| `labelSmall`      | 12   | 16          | 500         | +0.04em        | All-caps micro labels (avoid when possible) |

Weights used across the app: **400 Regular, 500 Medium, 600 SemiBold.** 700 Bold is reserved for Instrument Serif accents only — Inter rarely needs heavier than 600.

### 3.3 Rules

- **Don't mix weights within a paragraph.** A single text block uses one weight. Emphasis comes from size, not weight jumps.
- **Display styles are used sparingly.** `displayLarge` appears at most once per screen.
- **Instrument Serif is exceptional.** It shows up for the streak count on Home and nowhere else in the app's steady state. Milestone reveals are the only other use.
- **No UPPERCASE transform.** If a label needs to be all-caps, set it as literal all-caps with `labelSmall` + +0.04em letter-spacing; don't apply a CSS/textTransform.

### 3.4 Compose mapping

The Compose `Typography` constructor in `Type.kt` maps directly: each slot name above is already a Material 3 slot name. A type swap is one file, not a grep-and-replace.

```kotlin
val TideletTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 72.sp,
        lineHeight = 80.sp,
        letterSpacing = (-0.02).em,
    ),
    // ...
)
```

---

## 4. Spacing & layout

Strict 8-point grid. No ad-hoc padding in composables — every number comes from a token.

| Token       | Value | Use                                           |
|-------------|-------|-----------------------------------------------|
| `space-1`   | 4 dp  | Between tightly-related inline elements       |
| `space-2`   | 8 dp  | Chip gaps, icon-to-label                      |
| `space-3`   | 12 dp | Dense list rows                               |
| `space-4`   | 16 dp | Default gap between related blocks            |
| `space-6`   | 24 dp | Horizontal page padding (everywhere)          |
| `space-8`   | 32 dp | Between major sections                        |
| `space-12`  | 48 dp | Top padding on hero sections                  |
| `space-16`  | 64 dp | Extreme whitespace (onboarding hero, landing) |

### 4.1 Layout rules

- **Horizontal page padding is always 24 dp.** Every screen. No exceptions. Content aligns.
- **Section spacing is 32 dp** between major vertical sections on the same screen.
- **Cards use 20 dp internal padding** (between the card border and its content).
- **Lists use 12 dp row padding** vertically inside each row, 24 dp side padding aligned with the page grid.
- **Minimum touch target is 48 × 48 dp** per WCAG 2.5.5. Small icons get invisible padding to hit this.

Store these as a Kotlin object for code:

```kotlin
object Spacing {
    val s1 = 4.dp; val s2 = 8.dp; val s3 = 12.dp; val s4 = 16.dp
    val s6 = 24.dp; val s8 = 32.dp; val s12 = 48.dp; val s16 = 64.dp
}
```

---

## 5. Shape

A single corner-radius scale. No more `RoundedCornerShape(14.dp)` floating in composables.

| Token          | Value | Use                                            |
|----------------|-------|------------------------------------------------|
| `radius-sm`    | 8 dp  | Inputs, chips, small buttons                   |
| `radius-md`    | 16 dp | Cards, tiles, standard buttons                 |
| `radius-lg`    | 24 dp | Bottom sheets, dialogs, hero surfaces          |
| `radius-full`  | 1000 dp | Pill buttons, avatar circles                 |

### Rules

- **One radius per surface.** Don't mix.
- **Pill buttons use `radius-full`** with vertical padding that naturally resolves to a half-oval shape.
- **No square corners anywhere.** Even form dividers use 2 dp pill ends where practical.

---

## 6. Motion

Low-key, functional, skippable. A wellness app that animates too much reads as performative.

### 6.1 Durations

| Token         | ms   | Use                                         |
|---------------|------|---------------------------------------------|
| `motion-micro`   | 120 | Chip toggle, checkbox, focus ring           |
| `motion-standard`| 240 | Enter/exit, expand/collapse                 |
| `motion-page`    | 400 | Screen transitions                          |
| `motion-emotion` | 600 | Milestone reveals, letter-to-self unseal    |

### 6.2 Easing

- `easing-standard` = `CubicBezierEasing(0.4f, 0f, 0.2f, 1f)` — default.
- `easing-emphatic` = `CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)` — entries that arrive from nothing (modals, notifications).
- `easing-exit` = `CubicBezierEasing(0.4f, 0f, 1f, 1f)` — departures that just need to get off-screen.

### 6.3 Rules

- **Respect Reduce Motion.** Read `AccessibilityManager.isAnimationDisabled` (or check `Settings.Global.ANIMATOR_DURATION_SCALE`). When true, durations collapse to 0 and cross-fades replace slides.
- **No parallax or bounce.** The breathing circle and wave animation are deliberate, functional motion — not decoration.
- **Only one thing moves at a time.** Two simultaneous animations on the same screen is a bug.

---

## 7. Iconography

### 7.1 Library

- **Material Icons Outlined.** Replace the current `Icons.Rounded.*` usages. Outlined variants at 1.5 dp effective stroke feel modern-minimal; rounded variants feel cute and wellness-cliché.
- **No Material 3 Symbols.** The extended icons already in the catalogue are sufficient; introducing the Symbols library adds APK weight for zero benefit at v1.

### 7.2 Sizes

| Token      | Size  | Use                                   |
|------------|-------|---------------------------------------|
| `icon-sm`  | 16 dp | Inline text, badges                   |
| `icon-md`  | 20 dp | Default action icons in buttons       |
| `icon-nav` | 24 dp | Top-bar, bottom-nav                   |
| `icon-xl`  | 32 dp | Feature tiles (SOS grid), empty states |

### 7.3 Rules

- **Icons don't replace labels; they accompany them.** The one exception is the SOS grid, where tiles are large and tap-targeted.
- **Stroke weight is constant per screen.** Don't mix outlined + filled on the same view.
- **Colour defaults to `text-secondary`.** Icons only take on the accent colour when they're the primary actor (e.g. the terracotta dot on an SOS button).

---

## 8. Component recipes

Reference implementations. When a screen needs one of these, it composes the recipe — not a hand-rolled variant.

### 8.1 Primary button (filled)

```
height           48 dp (or 56 dp on hero surfaces)
padding          horizontal 24 dp, vertical 12 dp
background       primary (#2E3D35 light / #A3BEB0 dark)
text             on-primary
typography       labelLarge (14 sp, weight 500)
shape            radius-md
motion           ripple; no scale, no shadow change
```

### 8.2 Secondary button (outlined)

```
height           48 dp
padding          horizontal 24 dp, vertical 12 dp
background       transparent
border           1 dp border-emphasis
text             ink
typography       labelLarge
shape            radius-md
```

### 8.3 Pill button (for soft primary actions like the Home SOS CTA)

```
height           56 dp
padding          horizontal 24 dp
background       transparent
border           1.5 dp accent-warm
text             ink
leading          8dp round filled dot, colour accent-warm
typography       titleMedium
shape            radius-full
```

Visual: the terracotta dot is the only coloured element; the rest is quiet. Reads as "tap me if you need to" rather than "DANGER CLICK ME".

### 8.4 Card (default)

```
background       surface (#FFFFFF light / #181A19 dark)
border           1 dp border-subtle
padding          20 dp
shape            radius-md
elevation        0
spacing between cards   12 dp vertical
```

### 8.5 Input (text field)

```
height           56 dp (single-line) | 120 dp min (multi-line)
padding          horizontal 16 dp, vertical 16 dp
background       surface
border           1 dp border-subtle; on focus: 1.5 dp border-emphasis
label            above the field in text-muted, bodyMedium
placeholder      inside in text-muted
value            ink, bodyLarge
shape            radius-sm
```

### 8.6 Chip (selectable)

```
height           36 dp
padding          horizontal 12 dp
background       transparent (unselected) | primary-container (selected)
border           1 dp border-subtle (unselected) | none (selected)
text             text-secondary (unselected) | on-primary-container (selected)
typography       labelLarge
shape            radius-full
```

### 8.7 Top bar

```
height           56 dp
background       background (no tint, no elevation, no shadow)
title            titleLarge, ink
leading          24 dp outlined icon (back/close)
padding          16 dp horizontal
```

No divider below. Scroll is inferred from content offset.

### 8.8 Bottom navigation

```
height           72 dp
background       surface
border           1 dp border-subtle (top only)
label            labelSmall below icon
icon             outlined, 24 dp
selected colour  primary
unselected       text-muted
indicator        none (no pill/background behind selected tab)
```

Minimal version of Material 3 bottom nav: no pill indicator, label always visible.

### 8.9 Tile (SOS grid)

```
aspect-ratio     1 : 1
background       surface-sunken (#F5F2EC light on sos-surface, or on top-level)
border           none
padding          20 dp
icon             32 dp outlined, ink
title            titleMedium, ink
spacing          12 dp between icon and title
shape            radius-md
```

No subtitle on mobile. No accent colour on the tile itself — it's the arrangement and restraint that make it feel considered.

### 8.10 Sheet (bottom)

```
background       surface
shape            radius-lg top corners, square bottom
drag handle      48 × 4 dp pill, border-subtle, top 12 dp
padding          24 dp
max height       75% of screen
scrim            ink at 40% alpha
```

---

## 9. Per-screen redesign

### 9.1 Home

**Today:** dense vertical layout with a card-bound milestone bar and a solid coral SOS button.

**Target:**

- Very top of the screen: breathing room (48 dp).
- Streak count rendered in Instrument Serif, 72 sp, weight 400, centred. Underneath, "days" in Inter `titleMedium`, `text-muted`. For the `isWithinFirstDay` case, render "12h 43m" in `titleMedium` below "days".
- Milestone line becomes a single text row: "24 hours → 1 week". No progress bar. The caption below it (`bodySmall`, `text-muted`) reads "6 days to go".
- Weekly reflection card (when shown) loses its Card chrome — becomes a sage-underlined paragraph with a single text-button CTA.
- SOS button is a full-width pill (see §8.3) anchored 48 dp from the bottom. Label: "I'm having a craving". Small terracotta dot leading.

Net effect: when the user opens Home, they see one big number and one quiet button. Everything else is calm.

### 9.2 SOS grid

**Today:** 2×2 Card tiles with icons + titles + subtitles + a wide Journal card at the bottom.

**Target:**

- Whole screen sits on `sos-surface`.
- Page title "Take care of yourself" in `headlineMedium`, `on-sos-surface`, left-aligned at 24 dp padding.
- Sub-line "What feels right?" in `bodyMedium`, `text-muted` (on sos-surface variant).
- 2×2 grid of tiles (see §8.9): Ride the Wave, Breathe, Reasons, Distractions. Drop the subtitle.
- Journal becomes a single full-width tile below the grid, styled identically.
- Close (X) in the top-right, outlined icon, `on-sos-surface`, 48 × 48 dp touch target.

### 9.3 Ride the Wave

**Today:** three overlaid wave lines + moving dot + rotating copy + soundscape chips + end-early button.

**Target:**

- Single wave line (keep the sine; drop the opacity stack and the moving dot). Stroke 1.5 dp, `accent-warm`.
- Countdown in Inter `displayLarge` (72 sp), centred, `on-sos-surface`. No ornament.
- Rotating line drops to `bodyLarge` (not titleMedium), `text-muted-on-sos-surface`. Still rotates every 30 s.
- Soundscape picker becomes pill chips (see §8.6). Selected chip = `sos-accent-container` fill.
- End-early = outlined pill button at the bottom, full-width.
- Wave analysis panel (after non-DRANK outcome) keeps its current shape but sits on `surface` inside an `radius-lg` bottom sheet instead of inline — this separates reflection from action.

### 9.4 Breathe

**Today:** solid filled circle expanding/contracting + phase label inside + cycle count below.

**Target:**

- Single outlined ring (2 dp stroke, `accent-warm`). Not filled.
- Phase label inside the ring, Inter `titleLarge`, `on-sos-surface`.
- Cycle count moves from below the circle to a small chip in the top-right (e.g. "3/∞"), `labelMedium`.
- Stop button at the bottom, outlined pill, full-width.
- Background is `sos-surface` as today.

Quieter, less visually busy, reads as an object of attention rather than a graphic.

### 9.5 Onboarding

**Today:** three cards with titles, body copy, and full-width primary buttons.

**Target:**

- Each step: centre-aligned block with 64 dp top breathing room.
- Title in `displaySmall` (40 sp). Body in `bodyLarge`, `text-secondary`, constrained to 32 em width.
- Primary CTA: pill button, sage fill, full-width. Secondary (e.g. "Yesterday"): outlined pill.
- No progress indicator. Three screens is short enough that a dots indicator would add more noise than help.

### 9.6 Distortions library (CBT)

**Today:** Card list with name, definition, example.

**Target:**

- Drop the Card chrome entirely. This is reading material.
- Each entry: section with 32 dp vertical gap.
  - **Label** — Inter `titleMedium`, ink, no colour.
  - Definition — Inter `bodyLarge`, `text-secondary`, indented 0.
  - Example — Inter `bodyLarge` *italic*, `text-muted`.
  - "Try:" + technique — `bodyLarge`, ink, bold-prefixed label "Try: ".
  - Challenging question — Inter `bodyLarge` *italic*, `text-muted`.
- Soft "Not a thought — more of a habit?" callout at the bottom (per Phase 2 spec): 1 dp border card, no fill, pointing to Check-in.

### 9.7 Check-in

**Today:** scrollable form with chips and stepper.

**Target:**

- Three sections separated by 32 dp vertical gaps, no section cards.
- Section headers in `titleMedium`, ink.
- "Did you drink?" — two pill toggles side by side, equal width.
- Drink count (when revealed) — outlined step buttons on either side of the count in `displaySmall`, centre-aligned.
- Trigger chips (when revealed) — FlowRow of pill chips (§8.6).
- Mood chips — five outlined chips, selected state = `primary-container`.
- Note field — §8.5 multi-line, no border unless focused.
- Save button bottom, pill, primary fill.

### 9.8 Settings

**Today:** standard Material sections.

**Target:**

- Linear-style rows. Section label in `text-muted`, `labelLarge`, +0.04em letter-spacing, all-caps literal ("REMINDERS"). 48 dp row height.
- Rows separated by 1 dp `border-subtle`. No card backgrounds.
- Each row: title (ink, bodyLarge) + optional caption (text-muted, bodyMedium) + trailing control (switch, or `>` icon).
- Destructive action (Delete all my data) rendered in `error`, same row style. Tap opens a confirm sheet.

### 9.9 Stats (Phase 2)

**Today:** placeholder.

**Target (guidance for Phase 2):**

- One metric per row. Big number in `displayMedium`, tiny label beneath in `bodyMedium`, `text-muted`. No bar charts in v1.
- Examples: "347 days / since you picked up Tidelet", "82 % / craving events you got through", "₹24,500 / estimated money saved this year".
- Each row has 24 dp vertical padding and a 1 dp `border-subtle` below it.

---

## 10. Voice & tone

This is as important as any colour choice. A beautifully typeset screen with the wrong copy feels off.

### 10.1 Principles

- **First-person, present tense.** "I'm done" beats "Finish". "Save this" beats "Submit".
- **Short nouns, short verbs.** "Craving" (not "urge moment"). "Slip" (not "lapse experience"). "Note" (not "journal entry").
- **No exclamation marks. Anywhere.** Tidelet is earnest, not peppy.
- **No emojis.** Breaks the minimal tone.
- **No wellness jargon.** Ban-list: "journey", "grateful", "wellness", "mindset", "vibes", "energy".
- **No motivational empty-calories.** "You've got this!" reads hollow. Silence is better.

### 10.2 Examples

| Don't                              | Do                                 |
|------------------------------------|------------------------------------|
| "Nice work! 🎉 7 days sober!"      | "Seven days."                      |
| "Save your daily check-in"         | "Keep this note"                   |
| "How are you feeling today?"       | "How was today?"                   |
| "Start your sobriety journey"      | "Pick your starting date"          |
| "You slipped. Don't give up!"      | "You logged a slip. That's okay."  |

### 10.3 Dates and numbers

- Dates in the app use `DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)` — honours the system locale. No manual formatting.
- Currency uses `NumberFormat.getCurrencyInstance(Locale.getDefault())` — symbol comes from the user's system.
- Numbers with units use a thin space between number and unit: "24 hours", not "24hours".

---

## 11. Accessibility

Non-negotiable.

- **WCAG AA minimum for all text.** Verified contrast ratios documented in §2.
- **Minimum touch target 48 × 48 dp.** Invisible padding is fine; small tappable areas are not.
- **Reduced motion respected.** Gate all non-essential animation behind `AccessibilityManager`.
- **TalkBack compatible.**
  - Every interactive element has a `contentDescription` (or visible label).
  - Decorative icons use `contentDescription = null`.
  - Group semantically related elements with `Modifier.semantics(mergeDescendants = true)`.
- **Dynamic text size.** Inter scales cleanly up to 200 %. Layouts must not overflow at that setting.
- **Keyboard/switch navigation.** Focus order follows visual order; focused state gets a 1.5 dp `border-emphasis` ring plus the existing fill change.
- **Test tags preserved.** All `testTag` modifiers from the Phase 1 testing work stay in place.

---

## 12. Do's and don'ts

| ✅ Do                                                    | ❌ Don't                                                   |
|----------------------------------------------------------|------------------------------------------------------------|
| Use 24 dp horizontal page padding on every screen        | Add per-screen padding values "because it looks better"    |
| Pull every colour from `MaterialTheme.colorScheme` or `TideletTheme.extended` | Import a hex literal into a composable                     |
| Default cards to `border(1.dp, border-subtle)` on `surface` | Use `elevation = 4.dp` on cards                            |
| Write copy in first person, present tense                | Write motivational exclamations ("You can do this!")       |
| Put warmth only on SOS surfaces                          | Use terracotta accents on Home or Settings                 |
| Use Inter for all UI text                                | Reach for Instrument Serif outside the streak + milestone moments |
| Keep animations short (120–240 ms for most cases)        | Add bounce, parallax, or simultaneous multi-element motion |

---

## 13. Implementation notes (for the later coding pass)

This section is a map, not a task list. Actual work happens in a follow-up PR after the design is signed off.

### Files affected

| Area              | File(s)                                                                                  |
|-------------------|------------------------------------------------------------------------------------------|
| Colour            | `app/src/main/java/com/tidelet/app/ui/theme/Color.kt`, `Theme.kt`                        |
| Typography        | `Type.kt`; new `app/src/main/res/font/inter_variable.ttf`, `instrument_serif_regular.ttf` + (Italic) |
| Spacing/shape/motion | new `app/src/main/java/com/tidelet/app/ui/theme/Tokens.kt`                            |
| Icons             | Every screen — swap `Icons.Rounded.*` → `Icons.Outlined.*`                               |
| Per-screen polish | `HomeScreen.kt`, `SosScreen.kt`, `RideTheWaveScreen.kt`, `BreatheScreen.kt`, `OnboardingScreen.kt`, `DistortionsLibraryScreen.kt`, `CheckInScreen.kt`, `SettingsScreen.kt` |

### Approximate PR breakdown

1. **Tokens PR** — Color.kt + Theme.kt + Type.kt + new Tokens.kt + font files. No visual regression on any screen (tokens look up roughly the same slots).
2. **Icon swap PR** — global find/replace `Icons.Rounded.*` → `Icons.Outlined.*`. Visual diff is obvious in screenshot tests.
3. **Home polish PR** — apply §9.1.
4. **SOS flow polish PR** — apply §9.2 through §9.4 in one PR (they share `sos-surface`).
5. **Onboarding + Check-in polish PR** — §9.5 and §9.7.
6. **Distortions + Settings polish PR** — §9.6 and §9.8.
7. **Stats polish PR** — §9.9, if Phase 2 Stats has landed by then.

### What not to do in implementation

- Do not change screen structure (the Compose hierarchy). The design refresh is a token + modifier pass, not a rewrite.
- Do not introduce a new navigation pattern. The existing `TideletNav.kt` is fine.
- Do not touch ViewModels. State shapes are stable.
- Do not break `testTag` selectors — tests from Phase 1 must still pass.
