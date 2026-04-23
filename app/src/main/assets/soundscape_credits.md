# Soundscape credits

The ambient loops shipped in `res/raw/` are redistributed under the
[Pixabay Content License](https://pixabay.com/service/license-summary/),
which permits royalty-free use (including commercial) with no attribution
required. Credits are recorded here anyway, for provenance.

| File (res/raw) | Title                                             | Artist        | Pixabay ID |
|----------------|---------------------------------------------------|---------------|------------|
| ocean.mp3      | Ocean Waves                                       | SolarMusic    | 112906     |
| river.mp3      | Birds Singing Calm River Nature Ambient Sound     | SoundsForYou  | 127411     |
| rain.mp3       | Calming Rain Loop                                 | Dragon Studio | 398653     |

All three were fetched from `cdn.pixabay.com` and verified as MPEG layer III
(256 kbps) before being committed.

This file lives under `assets/` (not `res/raw/`) because Android's resource
name validator rejects filenames with uppercase letters or multiple dots.
Placing the credits under `assets/` keeps the provenance inside the APK
without polluting the `R.raw.*` namespace.
