# Quicklinks — AI Assistant & Beauty Calendar Widgets

Three Android home-screen widgets in one tiny app, designed and built entirely on a phone with [Termux](https://termux.dev) — no PC, no Android Studio.

**Designed by [@baliboyish](https://github.com/baliboyish) · Built with Muse AI × CopyPaste Department**

## The widgets

### 1. AI Assistant — "Different Minds. Different Strengths."
Five cards that open the native apps (with a web fallback if an app isn't installed): **ChatGPT, Claude, DeepSeek, Muse AI, Gemini** — each with its icon, tagline, accent color and feature list.

### 2. AI Assistant — Strip (4×1)
The slim edition: icons, names, taglines, and one accent line per card, in a single row. (Gemini carries a tiny extra "Search" line, because of course it does.)

### 3. Beauty Calendar
A real month calendar (month arrows, today pill, event dots from your calendar after a one-time permission tap) plus **Quick Controls**:
- **Real one-tap Wi-Fi, Bluetooth and flashlight toggles** — not shortcuts to settings, actual toggles
- Media & ring volume bars that open a floating panel with a **truly draggable slider** (widgets alone can't receive drag gestures, so a tiny activity does it)

## The trick that makes the toggles possible

The app targets **API 28** (`android.target-sdk: 28` in `project.yml`). Android blocks `WifiManager.setWifiEnabled()` for apps targeting API 29+, and `BluetoothAdapter.enable()` for API 33+ — which is why the big widget apps lost their real toggles years ago. A sideloaded app that still targets 28 keeps them. The honest trade-off: this build can't go to the Play Store (which demands a current target API), so it's sideload-only — the "full power" edition.

## Build it yourself

You need Termux (from F-Droid) on an Android phone and [termux-builder](https://github.com/rianprei/termux-builder):

```bash
pkg install termux-builder   # plus its documented setup
# termux-builder builds against the TARGET platform; for target 28 you also
# need the official android-28 platform in ~/.termux-builder/sdk/platforms/
cd quicklinks
termux-builder build .
```

Then install `.build/quicklinks-debug.apk`. The project signs with a standard Android **debug** keystore (`debug.keystore`, not included in this repo — drop in your own, or let your builder generate one; `project.yml` expects the usual `androiddebugkey` / `android` defaults).

> Note: phone-side build outputs (`.build/`) are gitignored. The icon PNGs under `src/res/drawable*` are added to this repo as separate uploads — if a few look missing in the file list, they live in the release APK.

## Install (no building)

Grab the APK from [Releases](../../releases), allow "install from unknown sources" for your file manager, and add the widgets from your launcher's widget picker (they're listed under **Quicklinks**). Play Protect will warn about an "unknown developer" — that's normal for sideloads.

## Permissions (and why)

`CHANGE_WIFI_STATE`, `BLUETOOTH`/`BLUETOOTH_ADMIN` — the toggles. `FLASHLIGHT`, `MODIFY_AUDIO_SETTINGS` — torch and volume. `READ_CALENDAR` — only for event dots, requested in-app when you tap the month title; deny it and everything else still works.

---
*Born as an "ide gabut" (a random idle idea) on 25 October 2026, live on a home screen the same day.*
