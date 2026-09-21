# MGtürk Signature — 2.9.2

The approved reference is **2.8.7.4**, recorded in `TURKRADYO_REFERENCE.md`.
This design is a separate development line and does not replace that reference.

## Design

- Theme-colored backgrounds, surfaces and dialogs, one type scale and spacing system.
- A compact MGtürk wordmark, instrument dial, prominent station name and circular
  transport controls establish a clear listening hierarchy.
- Upper transport retains main-list navigation. Lower transport retains current
  genre navigation across Türkiye Grupları, with the live group label visible.
- Library shortcuts, discovery/personalization and the listening studio are
  grouped under distinct headings. Existing feature nodes retain their handlers.
- Eight studio shortcuts use 40 px vector icons in compact horizontal cards,
  approximately 80 px tall. Audio/quality/repair tools remain in an expandable row.
- Baz is the smallest profile: the player and six 54 px shortcuts, with no
  decorative dial, studio grid, duplicate mini-player or discovery sections.
  Its visibility rules take precedence over the shared layout, including after
  delayed initialization, theme changes, profile switches and app restart.
- Themes remain accessible from the profile picker in Baz. Blue, purple and
  green palettes color the page, player, cards and feature panels, not just icons.
- The radio catalog, favorites, search, settings, EQ, DNA, sleep timer, alarm,
  genres, Slow mode, track history, NOTA AI, theme gallery and profile picker
  use the same surfaces, controls, typography and accent treatment.
- All three available profiles, existing themes, portrait lock and playback behavior are
  retained. The new artwork is local SVG/CSS; no image/font download is required.
- No new polling or MutationObserver is added. Mounting is bounded and subsequent
  updates respond to user/theme events. Reduced motion remains respected.
- Sheet keyboard navigation is contained within the dialog and closes back to
  the opener on UI close; station rows can be activated with Enter or Space.

## Build and review

Version: 2.9.2 / 302. Independent test app ID:
`com.muhammetgecgil.turkradyo.test.v302`. As in the reference, a stable production
signing key is unavailable, so this test APK installs alongside previous apps.
Favorites/settings are separate; no automatic migration is claimed.

Existing browser and native regression suites remain mandatory. UI captures
include all three profiles, a small screen, red/purple/blue/green themes and feature interiors.
Results and exact build revision are recorded in the design pull request.
Device playback, long streaming sessions and physical rotation require a phone;
browser previews use controlled catalog/telemetry data.

## Wake alarm and sleep timer (2.9.2)

- The last wake time, station and optional daily repeat are native persistent state.
  Closing the app, cancelling an alarm or pausing playback retains the last selection.
- Wake-ups use Android AlarmClockInfo. Missing exact-alarm permission is shown as
  pending, with a settings button; it is never advertised as a successfully armed alarm.
  Returning from permission settings, rebooting and clock/time-zone changes restore
  eligible schedules. A missed one-shot remains off instead of starting unexpectedly.
- A valid wake delivery starts the playback foreground service even when idle/paused.
  It clears a previous evening's sleep timer. Cancelled, replaced and duplicate wake
  deliveries are ignored. Optional daily repeat schedules the next local occurrence.
- Sleep durations use elapsed realtime, so changing the phone clock cannot shorten
  the timer. The service checks the same persisted deadline as the system receiver;
  stale/cancelled sleep callbacks cannot stop a newly selected timer or wake-up.
  Native scheduling results drive the visible countdown and failure messages.
- Theme surfaces combine both palette colors, subtle pearl highlights and shaded
  material layers. Baz sizing, compact controls and portrait lock remain intact.
- A live radio alarm requires internet and audible device media volume. Force-stop,
  a powered-off phone and vendor battery restrictions still require device validation.

Android scheduling design references:
https://developer.android.com/develop/background-work/services/alarms
https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
