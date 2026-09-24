# Privacy Policy

Last updated: 2026-09-24

Tidelet is built to be used privately, without giving up any data about you or your drinking. This page explains exactly what that means.

## The short version

- There is no backend server. Tidelet doesn't talk to the internet at all.
- There are no user accounts. Nobody signs up, nobody logs in.
- There is no analytics and no telemetry. Nothing about how you use the app is measured, recorded, or sent anywhere.
- There are no ad SDKs. Tidelet doesn't show ads and doesn't embed any ad or tracking libraries.
- The Android app requests no network-related permissions — it is not capable of making a network call. Check `app/src/main/AndroidManifest.xml` in the source code yourself; there's no `INTERNET` permission declared.

## Where your data lives

Everything you enter into Tidelet — your streak start date, craving check-ins, journal entries, thought records, and any other notes — is stored locally on your device, in a local database (Room/SQLite) and local preferences storage. None of it is copied anywhere else, because the app has no way to copy it anywhere else.

If you uninstall the app or clear its data, that data is gone. If you want a copy for yourself, use the export feature in Settings, which writes a file to your own device — Tidelet never uploads it.

## What Tidelet does not do

- It does not collect your name, email, location, or any identifying information.
- It does not use device identifiers, advertising IDs, or fingerprinting.
- It does not share, sell, or transmit your data to any third party, because it never transmits data anywhere.
- It does not use cookies or any web-based tracking (it's not a website).

## Links to outside resources

Tidelet's Resources screen links out to external organizations and helplines (for example crisis lines, Alcoholics Anonymous, and SMART Recovery) and can dial a phone number for you when you tap one. Tapping these opens your device's browser or dialer — Tidelet itself is not making a network request, and once you leave the app, that outside site or service has its own privacy practices, which this policy doesn't cover.

## Not medical advice

Tidelet is a self-help tool for managing cravings and tracking your own progress. It is not a medical device, it does not provide medical advice, diagnosis, or treatment, and it is not a substitute for professional care. If you are struggling, please reach out to a real person:

- **US:** call or text **988** (Suicide & Crisis Lifeline), or call the **SAMHSA National Helpline** at **1-800-662-4357** (free, confidential, 24/7).
- **Outside the US:** please look up your local crisis line or emergency number — Tidelet's Resources screen lists a few country-specific helplines, but if yours isn't listed, a quick search for "[your country] crisis line" or "[your country] suicide prevention helpline" will get you there.

If you are in immediate danger, contact your local emergency services.

## Changes to this policy

If Tidelet's data practices ever change (for example, if a future version adds an optional, clearly-disclosed feature that requires network access), this document will be updated alongside that change, and the changelog will note it. As of this version, the app is and has always been fully offline.

## Questions

Tidelet is a small, open-source project. If you have questions about this policy, please open an issue on the project's GitHub repository.
