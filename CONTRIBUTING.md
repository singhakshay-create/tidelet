# Contributing to Tidelet

Thanks for your interest in Tidelet. This is a solo/small-maintainer open-source project, so response times on issues and PRs may vary — please be patient, and feel free to ping a stale PR after a couple of weeks.

## Building locally

**Requirements:**
- Android Studio (a recent stable release)
- JDK 17 (Android Studio bundles one — point Gradle at it via File → Project Structure → SDK Location)
- Android SDK 35 (Android Studio will offer to install it on first sync)
- An emulator or device running Android 8.0 (API 26) or higher

**Steps:**

1. Clone the repository:
   ```
   git clone https://github.com/<org>/tidelet.git
   cd tidelet
   ```
2. Open the project folder in Android Studio (the one containing `settings.gradle.kts`) and let it finish indexing and syncing Gradle. First sync downloads AGP, Kotlin, Compose, Room, and other dependencies — it can take a few minutes.
3. Build a debug APK from the command line:
   ```
   ./gradlew assembleDebug
   ```
   or just hit Run in Android Studio to launch it on an emulator or connected device.

If Gradle sync fails, double check you're pointed at JDK 17 and that the SDK components Android Studio asks to install actually finished installing.

## Code style

- The app is 100% Kotlin, using Jetpack Compose for UI and a straightforward MVVM structure (`ui/<feature>/<Feature>Screen.kt` + `<Feature>ViewModel.kt`).
- There's no dependency-injection framework — dependencies are wired manually through `TideletApplication`. Follow that pattern rather than introducing a new DI approach in a PR.
- Match the formatting and structure of the surrounding code (standard Kotlin style, trailing commas in multi-line parameter lists, `MaterialTheme`/`TideletTheme` tokens rather than hardcoded colors).
- Keep new UI text in `strings.xml`, not hardcoded in Composables.

## Running tests

```
./gradlew test
```
runs the JVM unit test suite. Instrumented/UI tests (where present) run via:
```
./gradlew connectedAndroidTest
```
on a connected device or emulator.

Please add or update tests for behavior you change, especially anything touching the data layer (Room entities/DAOs, DataStore preferences) or export/import.

## Filing issues and PRs

- **Bug reports and feature requests:** use the issue templates under `.github/ISSUE_TEMPLATE/`. Include enough detail (steps to reproduce, device/Android version, app version) for the bug to be reproducible.
- **Pull requests:** keep them focused on one change. Describe what changed and why in the PR description. Link the issue it addresses, if any.
- Small, incremental PRs are easier to review than large ones — if you're planning a big change, consider opening an issue first to discuss the approach.

## A note on tone

Tidelet is a mental-health-adjacent app — people may open it in a difficult moment. If your PR touches copy in the SOS flow, crisis resources, or anywhere the app talks directly to someone having a craving, please keep the tone in mind: plain and warm, not clinical, not preachy, and never dismissive of what someone's going through. When in doubt, read the existing copy in `ui/sos/` and `ui/resources/` for the tone to match, and feel free to ask for feedback on wording before or during review.

## License

By contributing, you agree that your contributions will be licensed under the project's GPL-3.0 license (see `LICENSE`).
