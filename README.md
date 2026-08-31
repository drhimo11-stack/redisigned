# حصن (Hisn)

Designed by Ebrahim Sadeq Alhemyary.

Shield is a free, open-source Android content blocker. It uses an
Accessibility Service to read the URL bar and visible on-screen text of
16 supported browsers in real time, checks that text against a
regex-based keyword blocklist, and shows a full-screen "You are
Protected" overlay the instant a match is found. It also blocks the
Shorts feed inside YouTube and the Reels feed inside Instagram — without
blocking the rest of either app — and supports an optional lock timer
(1–365 days) that prevents disabling protection early, even if the
device clock is rolled back.

## How to build the APK (no Android Studio required)

1. Download this project as a ZIP (or clone it) so you have the
   `shield-2` folder on your computer.
2. Create a free GitHub account at [github.com](https://github.com) if
   you don't already have one.
3. Click the **+** icon in the top right → **New repository** → name it
   `shield` → set it to **Public** → click **Create repository**.
4. On the new, empty repo page, click **uploading an existing file**.
5. Drag in the **contents** of the `shield-2` folder (not the folder
   itself — the files should land directly at the repo root, so you end
   up with `app/`, `.github/`, `build.gradle.kts`, etc. at the top
   level). Click **Commit changes**.

## How to download the built APK

1. Click the **Actions** tab at the top of your repository and wait a
   few minutes for the **Build Shield APK** workflow run to finish.
2. Click the latest **Build Shield APK** run.
3. Scroll down to **Artifacts** and download **shield-debug-apk**, then
   unzip it to get `app-debug.apk`.

## How to install it on your phone

Copy `app-debug.apk` to your Android phone (e.g. via a cloud drive,
USB, or email to yourself) and tap it in your file manager. If Android
blocks the install, go to **Settings → Apps → [your file manager] →
Install unknown apps** and enable it, then try again.

## Permissions explained

| Permission | Why Shield needs it |
|---|---|
| Accessibility Service | Reads URL bars and visible text in supported apps to detect and block matching content. This is the core blocking mechanism. |
| Device Admin | Lets Shield refuse to be uninstalled while a lock timer is active. |
| Foreground Service / Special Use | Keeps a low-priority "Shield is active" notification running so Android doesn't kill the Accessibility Service under memory pressure. |
| Post Notifications | Required on Android 13+ to show the persistent "Shield is active" notification. |
| Receive Boot Completed | Restarts protection automatically after the phone reboots. |
| Query All Packages | Lets Shield detect which of the 16 supported browsers are installed. |
| Wake Lock | Prevents the protection service from being paused mid-check. |
| Internet / Access Network State | Declared for future use (e.g. update checks); not currently used to send any data anywhere. |

## Limitations

- Shield relies on the Accessibility Service being able to read the
  screen. If a browser changes its internal view IDs, URL-bar detection
  may need updated resource IDs.
- Shield cannot see inside encrypted, incognito, or heavily obfuscated
  web views that don't expose text nodes to accessibility services.
- Some Android OEMs aggressively kill background services; the
  KeepAlive notification helps but isn't a guarantee on every device.
- Blocking is text-pattern based, not content-classification based —
  it can't detect matching content that never appears as on-screen
  text (e.g. an image with no surrounding text or label).
- This is a self-blocking / accountability tool, not a bulletproof
  parental-control system; a technically sophisticated user with full
  device access could disable Accessibility Service permissions
  outright (though Device Admin + the lock timer make this harder
  during an active lock).

## License

MIT — see [LICENSE](LICENSE).
