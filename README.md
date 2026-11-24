# Activate Android

An Android port of the famous [activate-linux](https://github.com/MrGlockenspiel/activate-linux) / "Activate Windows" desktop watermark prank.

Displays an un-clickable, transparent watermark in the lower-right corner across your entire Android OS:

```text
Activate Android
Go to Settings to activate Android.
```

---

## 🚀 Key Features

* **True System-Wide Passthrough Overlay**:
  * Built using Android's `WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE` and `FLAG_NOT_FOCUSABLE`.
  * Touches, gestures, keyboards, and notifications pass through without any interference.
* **Modern Foreground Service (Android 8 to 14/15+)**:
  * Compliant with Android 14+ `FOREGROUND_SERVICE_SPECIAL_USE` requirements.
  * Low-priority, silent notification to keep the overlay active without battery drains.
* **Customization**:
  * Live edit title and subtitle (Presets included: *Activate Android*, *Activate Pixel*, *Activate One UI*, *Activate LineageOS*, *Activate Windows*).
  * Opacity slider (10% to 100%).
  * Screen offsets (Bottom margin & Right margin in dp).
  * Start on Boot (`BOOT_COMPLETED`).
* **Quick Settings Tile**:
  * Toggle the watermark ON/OFF directly from the Quick Settings notification shade pull-down.

---

## 🛠️ How It Works (Technical Architecture)

In Linux, `activate-linux` uses X11 overlay windows or Wayland's `wlr-layer-shell`. On Android, the architecture translates to:

| Component | Linux (`activate-linux`) | Android (`activate-android`) |
| :--- | :--- | :--- |
| **Window Type** | X11 Override-Redirect / Wayland Layer | `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` |
| **Touch Passthrough**| XShape / input transparent region | `FLAG_NOT_TOUCHABLE \| FLAG_NOT_FOCUSABLE` |
| **Screen Coverage** | Full screen overlay | `FLAG_LAYOUT_IN_SCREEN \| FLAG_LAYOUT_NO_LIMITS` |
| **Permission Gate** | Root / Display server access | `android.permission.SYSTEM_ALERT_WINDOW` |
| **Persistence** | Systemd service / daemon | Foreground `Service` with `START_STICKY` |

---

## 📱 How to Open & Build

1. Open **Android Studio** (`/opt/android-studio/bin/studio.sh`).
2. Select **Open** and choose the `activate-android` folder:
   `/home/knomp/Desktop/Projects/activate-android`
3. Connect an Android phone or emulator with USB debugging enabled.
4. Click **Run (`Shift + F10`)**.
5. In the app:
   - Tap **Grant Permission** to allow "Display over other apps".
   - Turn on the **Enable Watermark** switch.
