# MGtürk Signature — 2.9.0

The approved reference is **2.8.7.4**, recorded in `TURKRADYO_REFERENCE.md`.
This design is a separate development line and does not replace that reference.

## Design

- Graphite surfaces, restrained theme accents, one type scale and spacing system.
- A compact MGtürk wordmark, instrument dial, prominent station name and circular
  transport controls establish a clear listening hierarchy.
- Upper transport retains main-list navigation. Lower transport retains current
  genre navigation across Türkiye Grupları, with the live group label visible.
- Library shortcuts, discovery/personalization and the listening studio are
  grouped under distinct headings. Existing feature nodes retain their handlers.
- Eight studio cards use detailed vector artwork, larger spacing and readable
  descriptions. Audio/quality/repair tools remain available in an expandable row.
- The radio catalog, favorites, search, settings, EQ, DNA, sleep timer, alarm,
  genres, Slow mode, track history, NOTA AI, theme gallery and profile picker
  use the same surfaces, controls, typography and accent treatment.
- Both profiles, all existing themes, portrait lock and playback behavior are
  retained. The new artwork is local SVG/CSS; no image/font download is required.
- No new polling or MutationObserver is added. Mounting is bounded and subsequent
  updates respond to user/theme events. Reduced motion remains respected.
- Sheet keyboard navigation is contained within the dialog and closes back to
  the opener on UI close; station rows can be activated with Enter or Space.

## Build and review

Version: 2.9.0 / 300. Independent test app ID:
`com.muhammetgecgil.turkradyo.test.v300`. As in the reference, a stable production
signing key is unavailable, so this test APK installs alongside previous apps.
Favorites/settings are separate; no automatic migration is claimed.

Existing browser and native regression suites remain mandatory. UI captures
include both profiles, a small screen, red/purple themes and feature interiors.
Results and exact build revision are recorded in the design pull request.
Device playback, long streaming sessions and physical rotation require a phone;
browser previews use controlled catalog/telemetry data.
