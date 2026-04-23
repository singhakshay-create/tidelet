# Tidelet — Web Landing Page Spec

Version 0.1. Partners with [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) — every visual decision here defers to that document.

This spec is executable: a coding agent reading it should know what to build, what to write, what to host, and what to measure. All copy is drafted in place.

---

## 1. Goals

- Explain what Tidelet is in ≤ 30 seconds of reading.
- Drive Play Store downloads.
- Establish trust via privacy-first positioning and real scientific grounding.
- Preserve the calm/minimal aesthetic that matches the app.

## 2. Non-goals

- **No email capture, waitlist, or sign-up.** Not a lead-gen page.
- **No testimonials.** Premature for an early app; also risky in a sensitive category where visibility can harm users.
- **No cookies or analytics user identification.**
- **No blog, no CMS, no author pages.** Static content only.
- **No A/B testing or pop-ups.** We're optimising for respect, not conversion rate.

## 3. Target audience & primary job-to-be-done

### 3.1 Primary audience

An adult re-evaluating their relationship with alcohol. Usually:

- Already searching ("how to cut back", "sobriety app private", "CBT alcohol app").
- Skeptical of wellness-industrial-complex brands.
- Reluctant to create yet another account.
- Values privacy specifically because their drinking is private.

### 3.2 Secondary audience

Therapists, sponsors, and harm-reduction workers looking for a tool to recommend that doesn't compromise client privacy.

### 3.3 Primary job-to-be-done

> When I'm privately struggling with alcohol and Googling at 11 pm, I want to find a tool that will actually help me through a craving without judging me, signing me up for anything, or sending my information anywhere — so I can take one step tonight without committing to anything permanent.

## 4. Competitive positioning

| Competitor    | Their angle                                    | Our counter                                                          |
|---------------|------------------------------------------------|----------------------------------------------------------------------|
| **I Am Sober**    | Social pledge + heavy gamification           | No social, no leaderboards, no streaks-as-shame                      |
| **Reframe**       | AI coach + subscription ($99/yr)             | No AI, no subscription, no recurring cost                            |
| **Sober Time**    | Basic streak + ad-supported                  | Ad-free, deeper CBT tooling                                          |
| **Daybreak**      | Cloud-based behavioural science              | Same science, on-device only — no cloud                              |
| **Try Dry** (Alcohol Change UK) | Campaign-driven, Dry January focus | Year-round tool, not campaign-scoped                                 |
| **Nomo**          | Counter + community                          | Counter + CBT tools, no community                                    |

### Key differentiator we do NOT claim
We don't claim to be a replacement for treatment, therapy, or crisis services. The landing page links out to real human resources where relevant.

## 5. Differentiators (ordered for hero + feature grid)

1. **On-device, always.** Your data never leaves your phone.
2. **Scientifically grounded.** CBT, urge surfing, and Marlatt's relapse-prevention — not vibes.
3. **No account, no email, no sign-up.** Fully anonymous.
4. **Free today.** No ads, no in-app purchases, no subscription. (See §11 FAQ for the future-monetisation framing.)
5. **Compassionate reset.** A slip doesn't zero your history.
6. **Quiet by design.** No social feeds, no streak-shame, no push-notification theatre.

---

## 6. Page structure + full copy

Single-page scroll. Sections flow in this order.

### 6.1 Hero

**Layout**: left-aligned text stack on a `background` (warm ivory) surface, with a phone-frame screenshot of Home on the right. On mobile, text stacks above the screenshot.

**Copy**:

> ## A private companion for the days that are hard.
>
> Tidelet helps you ride out cravings, track your relationship with alcohol, and see patterns you'd miss — all on your phone, with nothing ever sent anywhere else.
>
> [Download on Google Play]

The headline uses Instrument Serif (weight 400, letter-spacing -0.02em) to carry the one moment of visual warmth on the page. Everything else is Inter.

**Phone screenshot**: real render of Home at 1080 × 2400, framed in a neutral dark phone silhouette (single SVG path, no chrome detail). Shows streak count + SOS pill.

### 6.2 How it works

**Layout**: three columns on desktop, three rows on mobile. Each column has a small outlined icon (24 dp), a short label, and one sentence of copy.

**Copy**:

> ### How it works
>
> **1. Pick your starting date.**
> Today, yesterday, or any day. No account, no email.
>
> **2. Open the SOS toolkit when a craving hits.**
> Ride the wave for 15 minutes. Try box breathing. Re-read your own reasons. Pick something else to do.
>
> **3. Come back when you're ready.**
> See your own patterns. Keep what helps. Everything stays on your phone.

### 6.3 What makes Tidelet different

**Layout**: 3×2 card grid on desktop, single column on mobile. Each card = outlined icon + short heading + 1–2 sentence body. No borders between cards — just generous whitespace.

**Copy**:

> ### What makes Tidelet different

**Card 1 — On-device, always**
> Your data lives on your phone. No servers. No accounts. No syncing. We literally cannot see what you write, because we don't have a backend.

**Card 2 — Grounded in research, not vibes**
> Every tool in Tidelet comes from cognitive behavioural therapy (CBT), Marlatt's relapse-prevention model, or urge surfing. The science is linked below.

**Card 3 — Anonymous by design**
> No email, no phone number, no sign-up. Open the app and you're in. The only identity Tidelet knows is the date you chose to start.

**Card 4 — Free today**
> No ads. No in-app purchases. No subscription. If that ever changes, core features stay free — we'll tell you first.

**Card 5 — Compassionate about slips**
> A slip isn't a failure of you. Tidelet logs it, asks what helped and what didn't, and keeps your history. No streak reset shame.

**Card 6 — Quiet on purpose**
> No social feeds, no public badges, no daily push notifications trying to win your attention back. Tidelet is there when you need it and silent when you don't.

### 6.4 The science

**Layout**: single centred column, 56 em max width. Body copy in `bodyLarge`. Citations as a small list in `bodyMedium`, `text-muted`.

**Copy**:

> ### The science
>
> Tidelet's toolkit is built on techniques that clinical trials have shown to help people change their drinking:
>
> - **CBT (cognitive behavioural therapy) for alcohol use** — the standard of care for mild-to-moderate alcohol problems, and the most consistently effective non-medication intervention in the research literature. Every thought check in Tidelet is a mini thought record.
> - **Urge surfing** — a mindfulness technique that turns a craving into an observed wave that rises and falls. Developed by Alan Marlatt and colleagues; validated in multiple RCTs for substance-use cravings. "Ride the wave" is a direct implementation.
> - **Marlatt's relapse-prevention model** — high-risk situations, early warning signs, coping plan. Tidelet's compassionate-reset flow and functional-analysis prompts are built on this framework.
> - **Distraction and alternative-activity scheduling** — the behavioural lever behind "Do something else". Boring-sounding, and among the most durable interventions when practiced.
>
> Further reading:
>
> - Kadden, Carroll et al., *Cognitive-Behavioral Coping Skills Therapy Manual* (NIAAA)
> - Marlatt, Donovan, *Relapse Prevention* (Guilford)
> - Bowen, Chawla, Marlatt, *Mindfulness-Based Relapse Prevention for Addictive Behaviors*
> - Beck & Liese, *Cognitive Therapy of Substance Abuse* (Guilford)
> - Brewer et al., 2011, "Mindfulness training and stress reactivity in substance abuse"

Each citation hyperlinks to the book's publisher page or a DOI landing page — no affiliate links, no ad tracking.

### 6.5 Privacy, really

**Layout**: same centred column as §6.4. This is a quiet, factual section — the design is as plain as the promise.

**Copy**:

> ### Privacy, really
>
> Most "private" apps mean "we encrypt what we collect." Tidelet doesn't collect anything. Here's what that actually means:
>
> - The app **does not request the INTERNET permission** in its Android manifest. It literally cannot connect to the network.
> - `android:allowBackup="false"` — your data is not included in Google cloud backups.
> - No third-party SDKs. No analytics. No crash reporting. No advertising IDs.
> - No account, no email, no device fingerprint.
> - Your data lives in a local Room database + DataStore file, on your phone only. If you want a backup, you export a Markdown file yourself and put it wherever you like.
>
> *(Verification: the app is open about its architecture; the full source is available at [link placeholder — insert GitHub URL if/when public].)*

If the app becomes open-source, add a live GitHub link. If not, the manifest + permission claims can be verified by anyone who installs the app and inspects the APK.

### 6.6 What Tidelet is NOT

**Layout**: short list, left-aligned, `bodyLarge`, `text-secondary`.

**Copy**:

> ### What Tidelet is not
>
> - Not a replacement for therapy or medical care. If you're in crisis, please see the resources below.
> - Not a social network. No leaderboards, no friends, no public streaks.
> - Not a wellness subscription. No tiers, no premium, no "unlock insights for $9.99".
> - Not a therapist. The tools inside are for self-guided practice between real support.
> - Not an AI chatbot. There's nothing for the app to "learn" because nothing leaves your phone.

### 6.7 Crisis resources

**Layout**: single bordered card, `border-subtle`. Short, calm copy.

**Copy**:

> ### If you're in crisis
>
> Tidelet is a tool for the long, steady work of changing a habit. It is not crisis support. If you need someone right now:
>
> - **United States** — 988 (Suicide & Crisis Lifeline); SAMHSA Helpline 1-800-662-4357
> - **United Kingdom** — Samaritans 116 123; Drinkline 0300 123 1110
> - **India** — iCall +91 9152987821; AASRA +91 9820466726
> - **Elsewhere** — [Find a Helpline](https://findahelpline.com/)

Links open in new tabs. These resources are geographic; the page detects the visitor's locale and surfaces the region's entries first, with an "Other countries" expand.

### 6.8 FAQ

**Layout**: plain HTML `<details>` disclosures, no JS. Summary in `titleMedium`, expanded body in `bodyLarge`.

**Copy**:

> ### Frequently asked

**Is Tidelet really free?**
> Yes. There are no ads, no in-app purchases, and no subscription. Core features will always be free. If we ever introduce an optional paid tier for non-essential extras, we'll tell you before anything changes.

**Can I export my data?**
> Yes. Settings → Export writes a Markdown file to a location you pick. You can re-import it later into a fresh install. The format is human-readable, so you can also read it in any text editor.

**What if I slip?**
> Tidelet logs the slip, asks what was going on, and keeps your history. Your "days" counter reflects your current streak, but the events log keeps the full picture. Slips are data, not failures.

**Is it only for alcohol?**
> Right now, yes. The tools (urge surfing, CBT thought checks, etc.) generalise to other compulsive behaviours, but the copy and defaults are built for alcohol. We'd rather do one thing well than four things vaguely.

**Is it on iOS?**
> Not yet. Android-only. If we see strong interest and find the right iOS contributor, we'll consider it.

**Is the source code available?**
> [To confirm — set when the open-source decision is made.]

**Do you have a privacy policy?**
> A one-page policy is linked in the footer. The short version: we don't collect data because the app has no way to.

**Can I suggest a feature?**
> Yes, please email [support@tidelet.app — placeholder]. We read everything. We reply when we can.

### 6.9 Download CTA

**Layout**: centred, generous whitespace above and below. A single Play Store badge. On mobile, a full-width pill-button mirroring the app's style links to the Play Store listing.

**Copy**:

> ### Ready to start?
>
> [Google Play badge — official asset]

Play Store badge uses Google's official asset (PNG or SVG from their branding guidelines). Link `rel="noopener"`.

### 6.10 Footer

**Layout**: single row on desktop, stacked on mobile. Small `labelMedium`, `text-muted`.

**Copy**:

> © 2026 Tidelet · [Privacy policy](privacy.html) · [Support](mailto:support@tidelet.app) · [Source](https://github.com/placeholder)

No social icons. No newsletter. No app-store badges repeated (the CTA section above handles that).

---

## 7. Visual design

### 7.1 Colour + typography

- Defer entirely to [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md).
- Body background: `#FBF9F5` (warm ivory).
- Sections with visual weight (science, privacy) stay on the same background — separation is whitespace, not colour.
- The hero gets one tint accent: a small terracotta `#C26B5A` dot next to the Play Store button, echoing the app's SOS button motif.
- Headlines (`<h1>`, `<h2>`) use Instrument Serif. Everything else is Inter.

### 7.2 Hero layout detail

- Desktop (≥ 1024 px): 2-column grid. Left = text stack (headline, subhead, CTA). Right = phone mockup, centred vertically, max-height `640px`.
- Tablet: single column, phone below text.
- Mobile: single column, phone between subhead and CTA (so the visual anchors the scroll).
- Max content width: 1120 px, centred.
- Horizontal padding: 24 px (mobile) / 48 px (desktop).

### 7.3 Images

- Only one raster image on the page: the hero phone screenshot. Everything else is SVG (icons, phone silhouette, OG image).
- Screenshot rendered at 1080 × 2400, served at 2× density, lazy-loaded below the fold; above-the-fold version is `fetchpriority="high"`.
- Provide `webp` + `avif` via `<picture>`.

### 7.4 No animations

- No scroll-triggered fades.
- No hover transforms on cards.
- Just the browser's default `<a>` hover underline + focus ring.

---

## 8. Technology & architecture

### 8.1 Stack

- **Astro 4.x** static site generator.
    - Zero JavaScript by default. Any interactive affordance (FAQ `<details>` is native HTML, no JS needed) stays HTML-native.
    - MDX for content sections (easier author flow for copy edits).
- **No framework integrations** (React/Vue/Svelte). The page doesn't need them.
- **Tailwind CSS v4** for styling — configured to produce CSS variables matching the design-system tokens. Alternative: hand-written CSS with CSS custom properties. Pick Tailwind only if the author is comfortable with it; the token discipline is more important than the tool.
- **Self-hosted fonts** — Inter Variable + Instrument Serif Regular/Italic, served from `/fonts/`. `font-display: swap`.

### 8.2 File structure

```
/web/                           # new top-level folder in the Tidelet repo
├── astro.config.mjs
├── package.json
├── tailwind.config.js          # if using Tailwind
├── public/
│   ├── fonts/
│   │   ├── inter-variable.woff2
│   │   └── instrument-serif-regular.woff2
│   ├── images/
│   │   ├── hero-home.webp
│   │   ├── hero-home.avif
│   │   └── phone-frame.svg
│   ├── og-image.png            # 1200x630, generated once
│   ├── favicon.svg
│   ├── robots.txt
│   └── sitemap.xml
├── src/
│   ├── layouts/
│   │   └── Base.astro           # html shell, head meta, fonts
│   ├── pages/
│   │   ├── index.astro          # the landing page
│   │   └── privacy.astro        # one-page privacy policy
│   ├── components/
│   │   ├── Hero.astro
│   │   ├── HowItWorks.astro
│   │   ├── DifferentiatorGrid.astro
│   │   ├── Science.astro
│   │   ├── Privacy.astro
│   │   ├── NotList.astro
│   │   ├── Crisis.astro
│   │   ├── Faq.astro
│   │   ├── DownloadCta.astro
│   │   └── Footer.astro
│   └── styles/
│       ├── tokens.css            # CSS variables for design-system tokens
│       └── global.css
└── README.md                    # build + deploy instructions
```

### 8.3 Build & run

Starter command (run from the repo root):

```
npm create astro@latest web -- --template minimal --typescript relaxed --install --no-git
```

Then:

```
cd web
npm install -D tailwindcss @tailwindcss/vite
npm run dev     # local dev at http://localhost:4321
npm run build   # outputs static site to web/dist/
```

### 8.4 Token mapping (CSS)

`src/styles/tokens.css` maps the design-system tokens onto CSS variables. Example:

```css
:root {
  --color-background: #FBF9F5;
  --color-surface: #FFFFFF;
  --color-border-subtle: #E8E4DB;
  --color-ink: #1A1C1A;
  --color-text-muted: #6B6F6B;
  --color-primary: #2E3D35;
  --color-accent-warm: #C26B5A;

  --space-4: 1rem;       /* 16 px */
  --space-6: 1.5rem;     /* 24 px */
  --space-8: 2rem;       /* 32 px */
  --space-12: 3rem;      /* 48 px */
  --space-16: 4rem;      /* 64 px */

  --radius-sm: 0.5rem;
  --radius-md: 1rem;
  --radius-full: 999px;

  --font-sans: "Inter", system-ui, sans-serif;
  --font-serif: "Instrument Serif", Georgia, serif;
}

@media (prefers-color-scheme: dark) {
  :root {
    --color-background: #0F1110;
    --color-surface: #181A19;
    --color-border-subtle: #2A2D2A;
    --color-ink: #ECECEA;
    --color-text-muted: #8C908B;
    --color-primary: #A3BEB0;
    --color-accent-warm: #D89482;
  }
}
```

### 8.5 No analytics by default

The page ships with zero tracking scripts. If first-party analytics become necessary, add one of:

- **Plausible** (self-hosted) — cookieless, privacy-friendly, GDPR-compliant without a banner.
- **Fathom** — similar, SaaS.

Both are optional and off by default. Document the decision explicitly before turning anything on.

---

## 9. SEO & metadata

### 9.1 `<head>` tags

```html
<title>Tidelet — a private companion for your relationship with alcohol</title>
<meta name="description" content="A quiet, on-device Android companion for cravings and sobriety. CBT tools, urge surfing, no account, no cloud. Your data never leaves your phone.">
<meta name="theme-color" content="#FBF9F5">
<link rel="canonical" href="https://tidelet.app/">

<meta property="og:title" content="Tidelet — a private companion for your relationship with alcohol">
<meta property="og:description" content="Cravings, sobriety, and CBT on your phone. Nothing sent anywhere else.">
<meta property="og:image" content="https://tidelet.app/og-image.png">
<meta property="og:type" content="website">
<meta property="og:url" content="https://tidelet.app/">

<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="Tidelet — a private companion for your relationship with alcohol">
<meta name="twitter:description" content="Cravings, sobriety, and CBT on your phone.">
<meta name="twitter:image" content="https://tidelet.app/og-image.png">
```

### 9.2 Keywords

Target (in rough priority):

1. `sobriety app private`
2. `anonymous sobriety tracker`
3. `CBT alcohol app`
4. `quit drinking app no account`
5. `on-device sobriety tracker`
6. `cognitive behavioural therapy drinking app`

Work these naturally into body copy — do not keyword-stuff.

### 9.3 Open Graph image

- Size: 1200 × 630 px.
- Background: warm ivory `#FBF9F5`.
- Centre text: *"A private companion for the days that are hard."* in Instrument Serif 64 px, ink `#1A1C1A`.
- Below: small "tidelet" wordmark in Inter 32 px.
- No screenshot. Text-only OG images travel better across platforms.

### 9.4 `robots.txt`

```
User-agent: *
Allow: /
Sitemap: https://tidelet.app/sitemap.xml
```

### 9.5 `sitemap.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://tidelet.app/</loc>
    <changefreq>monthly</changefreq>
    <priority>1.0</priority>
  </url>
  <url>
    <loc>https://tidelet.app/privacy.html</loc>
    <changefreq>yearly</changefreq>
    <priority>0.3</priority>
  </url>
</urlset>
```

---

## 10. Performance targets

Measured on a mid-tier Android device over 4G via Lighthouse or WebPageTest.

| Metric                          | Target     |
|---------------------------------|------------|
| Lighthouse Performance          | ≥ 95       |
| Lighthouse Accessibility        | = 100      |
| Lighthouse Best Practices       | = 100      |
| Lighthouse SEO                  | ≥ 95       |
| First Contentful Paint (FCP)    | < 1.0 s    |
| Largest Contentful Paint (LCP)  | < 1.8 s    |
| Cumulative Layout Shift (CLS)   | < 0.05     |
| Total Blocking Time (TBT)       | < 100 ms   |
| Total page weight (excl. hero)  | < 300 KB   |
| Third-party requests            | **0**      |

### 10.1 How we get there

- Astro ships zero JS by default.
- Fonts preloaded with `<link rel="preload" as="font" crossorigin>` for the weight used above the fold (Inter 400, Inter 600, Instrument Serif 400).
- Hero image served as AVIF first, WebP fallback, resized to 2× device pixel ratio, lazy-loaded below-fold.
- Icons as inline SVG (no icon font, no sprite sheet).
- CSS inlined in `<head>` under the critical threshold (~14 KB); remaining styles in a single small stylesheet.
- Zero third-party scripts (verify in DevTools Network tab on every deploy).

---

## 11. Hosting & deployment

### 11.1 Primary hosting

**Cloudflare Pages**
- Free tier.
- Global CDN.
- HTTPS auto-provisioned.
- Deploy on push to `main` via GitHub integration.
- Custom domain support (e.g. `tidelet.app`).

Alternative: **Netlify** (functionally equivalent) or **GitHub Pages** (simpler but no preview URLs for PRs).

### 11.2 Domain

To acquire (flagged for the user): `tidelet.app`, `tidelet.io`, or `tidelet.com`. Not part of this spec's delivery — check availability separately.

### 11.3 Deploy flow

```
push → Cloudflare Pages → `npm run build` → publish web/dist/ → global CDN
```

PR previews get auto-generated URLs so copy changes can be reviewed without merging.

### 11.4 Analytics / monitoring (optional)

- **Plausible**: add `<script defer src="https://plausible.io/js/plausible.js" data-domain="tidelet.app"></script>` to `Base.astro` *only if* explicitly opted in. Cookieless, GDPR-compliant, no banner needed.
- **Uptime**: Cloudflare's built-in analytics already covers availability.

Default is **no analytics**.

---

## 12. Implementation checklist for the coding agent

Copy this list into the PR description; tick as you go.

### Setup
- [ ] Run `npm create astro@latest web -- --template minimal --typescript relaxed`.
- [ ] Install Tailwind (or skip if using hand-CSS).
- [ ] Copy `tokens.css` from §8.4 into `src/styles/`.
- [ ] Download Inter Variable + Instrument Serif `.woff2` from Google Fonts / npm, place in `public/fonts/`.
- [ ] Wire font `@font-face` in `tokens.css` or a dedicated `fonts.css`.

### Content
- [ ] Implement every component listed in §8.2.
- [ ] Paste copy verbatim from §6 — do not improvise.
- [ ] Render the Home screenshot from an emulator at 1080 × 2400 and export `.webp` + `.avif`. Place in `public/images/`.
- [ ] Create the OG image per §9.3 (Figma, Photoshop, or any image tool — one-time asset).

### Build & polish
- [ ] `npm run build` exits clean.
- [ ] `npm run preview` shows the full page in both light and dark mode (dark via `prefers-color-scheme`).
- [ ] Lighthouse (mobile, slow 4G): all four scores pass §10 targets.
- [ ] DevTools Network tab shows zero third-party requests.
- [ ] Test `tab` key navigation reaches every interactive element in visual order.
- [ ] Test with VoiceOver / TalkBack enabled — every section is readable in order.

### Deploy
- [ ] Create Cloudflare Pages project pointing at the `web/` folder.
- [ ] Wire domain (if acquired) or use the default `*.pages.dev` subdomain.
- [ ] Add the URL to the repo README.

---

## 13. Out of scope

- Logo / wordmark redesign (the word "Tidelet" in Instrument Serif is the interim wordmark).
- A press kit or media page.
- A pricing page (free, no pricing).
- An onboarding explainer video.
- Sharing / referral functionality.
- Multi-language versions (English-only for v1; the app itself is locale-aware, the page isn't).
- Cookies / GDPR consent banner (not needed — no cookies).

---

## 14. Review checklist before shipping

Use this before the coding agent marks the landing page done:

- [ ] Every copy block on the page is drafted in §6 and appears verbatim.
- [ ] All contrast ratios on the page verified ≥ 4.5:1 for body text via an online contrast checker.
- [ ] No external script requests in the Network panel.
- [ ] OG image renders correctly on `metatags.io`.
- [ ] `robots.txt` + `sitemap.xml` present and fetchable.
- [ ] Lighthouse audit meets targets in §10.
- [ ] Privacy policy page exists (separate spec — single page, simple copy).
- [ ] Support email works and is monitored.
