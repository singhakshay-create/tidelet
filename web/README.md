# Tidelet landing page

Static Astro site. Zero JS, zero trackers, self-hostable fonts. Deploys to Cloudflare Pages (or any static host).

See the authoritative spec at [../docs/LANDING_PAGE_SPEC.md](../docs/LANDING_PAGE_SPEC.md).

## Run locally

Prerequisites: Node 20+, npm.

```
cd web
npm install
npm run dev        # http://localhost:4321
```

## Build

```
npm run build      # writes static site to web/dist/
npm run preview    # serves web/dist/ locally at http://localhost:4321
```

## Deploy

Cloudflare Pages, Netlify, or GitHub Pages all work — the output is plain HTML + CSS. Point the host at the `web/` folder and use:

- Build command: `npm run build`
- Build output directory: `dist`

No environment variables are required. No build-time secrets.

## What's in the box

```
web/
├── astro.config.mjs          # zero-JS, static, no prefetch
├── package.json
├── public/
│   ├── favicon.svg           # simple two-colour mark
│   ├── robots.txt
│   ├── sitemap.xml
│   └── fonts/                # drop Inter + Instrument Serif .woff2 here
├── src/
│   ├── layouts/Base.astro    # <head> metadata + OG tags
│   ├── pages/
│   │   ├── index.astro       # the landing page
│   │   └── privacy.astro     # one-page privacy policy
│   └── styles/
│       ├── tokens.css        # design-system tokens (palette, spacing, type)
│       └── global.css        # base reset + components
└── README.md                 # this file
```

## Design tokens

`src/styles/tokens.css` mirrors the palette and spacing scale in [../docs/DESIGN_SYSTEM.md](../docs/DESIGN_SYSTEM.md). Keep them in sync when the app system changes.

## Fonts

The site ships without font files — the default `system-ui` fallback renders cleanly. To activate Inter + Instrument Serif:

1. Download `inter-variable.woff2` and `instrument-serif-regular.woff2` (both free / OFL).
2. Drop them into `public/fonts/`.
3. Uncomment the `@font-face` blocks at the top of `src/styles/global.css`.

Do **not** pull fonts from Google Fonts' CDN at runtime — that leaks visitor IPs to Google and breaks the privacy stance.

## Hero screenshot

The `index.astro` file ships with a pure-CSS phone mockup so the page is never broken by a missing asset. For the real thing:

1. Render the Home screen from an emulator at 1080 × 2400.
2. Export as `.webp` + `.avif`, drop into `public/images/`.
3. Swap the `.hero-mockup` block in `index.astro` for a `<picture>` element.

## Analytics

None ships by default. If you later decide you want first-party analytics, the only options that match the privacy stance are:

- **Plausible** (self-hosted) — cookieless, no banner needed.
- **Fathom** — same tradeoffs as a hosted SaaS.

Both are one `<script>` tag in `Base.astro`. Document the choice when you make it.
