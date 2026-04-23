import { defineConfig } from 'astro/config';

// Zero-JS, static-site Astro config for the Tidelet landing page.
// Deploys to Cloudflare Pages; see /web/README.md.
export default defineConfig({
  site: 'https://tidelet.app',
  output: 'static',
  compressHTML: true,
  prefetch: false, // respect the privacy-first stance; no preemptive fetches
});
