# Focus Guard 🛡️
Tiny, dependency-free Android app that sends you back to the home screen when you open a blocked app during a focus session.

## Build via GitHub
1. Create a new GitHub repo, push this folder to `main`.
2. Repo → Actions → "Build APK" (runs automatically on push, or press "Run workflow").
3. Open the finished run → Artifacts → download `FocusGuard-apk` → unzip → `app-debug.apk`.
4. Install on phone (allow "install unknown apps" for your browser/file manager).

## Enable the blocker
Settings → Accessibility → Installed apps → Focus Guard → ON.

Android 13+ greys it out for sideloaded apps:
Settings → Apps → Focus Guard → ⋮ (top-right) → **Allow restricted settings**, then retry.

## How it works
- AccessibilityService listens ONLY to "window changed" events (no screen content read, no internet permission).
- If foreground app is in your block list and a session is active -> performs HOME action.
- Strict mode also blocks Settings + package installer so you can't disable/uninstall mid-session.
- Test with a 1-minute session first!
