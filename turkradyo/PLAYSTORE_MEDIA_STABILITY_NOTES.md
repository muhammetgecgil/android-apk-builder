# Türk Radyo v2.0.7 — Android Auto + Stream Stability

Base: canonical `TurkRadyo v2.0.7 Genre Groups` commit `41df11c8c0b3f7d48c108eff43f2c819490865a4`.

## Android Auto
- MediaBrowserService root now exposes Ana 80 and Son Dinlenenler.
- Play/Pause/Stop/Next/Previous and play-from-media-id are supported.
- RadioService MediaSession exposes transport controls for lock screen, Bluetooth and car controls.
- Current station metadata is persisted for Android Auto resume.

## Stream stability
- Per-station preferred stream is learned after verified playback.
- Failed sources are quarantined temporarily.
- One fast retry is attempted on the primary source.
- If still unavailable, Radio Browser Turkey mirrors are searched for the same station only.
- Candidate URLs are name-matched, health-ranked and probed before adoption.
- Verified alternative is saved and reused until it fails.
- Startup and long-buffer watchdogs trigger recovery without switching to a different station.

The canonical Genre Groups reference branch remains unchanged.
