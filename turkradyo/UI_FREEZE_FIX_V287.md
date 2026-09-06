TurkRadyo v2.8.7 UI freeze fix

Root cause found in v2.8.6 UI scripts:
- product-hardening-v3.js used a document-wide MutationObserver.
- render() removed/appended its own DOM block.
- That mutation retriggered the observer and scheduled another render indefinitely.
- product-health-v2.js had the same self-triggering pattern.

Symptoms match field report: RadioService/Media3 continues audio while WebView buttons become unresponsive.

v2.8.7 changes:
- Remove self-triggering MutationObserver loops.
- Render health panels only when their data signature changes.
- Reduce health polling frequency and DOM churn.
- Remove product-health-v1 global subtree observer.
- Bump WebView asset cache key to v=287.
- Add WebView renderer priority and Android 10+ unresponsive-renderer fail-safe.
- Keep Media3 ExoPlayer, Son 50 history and theme-aware Slow Mode from v2.8.6.
