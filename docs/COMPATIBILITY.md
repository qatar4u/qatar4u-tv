# تقرير توافق الجهاز والتلفزيون · Device & TV compatibility

Read from the tablet over ADB on 23 October 2026.

| Item | Value |
|---|---|
| Model | Samsung Galaxy Tab S6, SM-T865 (LTE), region XSG |
| Android | 10 (API 29), build QP1A.190711.020.T865XXU2BTC7 |
| Security patch | 1 March 2020 |
| SoC | Qualcomm Snapdragon 855 (msmnile), arm64-v8a |
| RAM | 5.3 GB (5,581,232 kB) |
| Screen | 2560×1600 (16:10), 360 dpi, 60 Hz, AMOLED |
| WebView | 155.0.8059.43 |
| DeX | Present (desktoplauncher, desktopsystemui, desktopmode.uiservice) |
| Window features | freeform windows, picture-in-picture |
| Leanback (TV) feature | Not declared: the app treats leanback as optional |
| Bootloader | Unlocked (verified boot "orange"); Magisk and TWRP apps installed earlier by the owner. Qatar4u TV does not use either. |

## What this means

- **Android 10 is supported.** The app targets API 34 and runs from API 29. Samsung released Android 12
  (One UI 4.1) for this model; the tablet has not been updated. Updating is optional and recommended for
  security, but an unlocked bootloader with Magisk may block or undo official updates.
- **16:10 screen, 16:9 TV.** Layouts use weights and dp, not fixed pixels. On a 16:9 TV through screen
  mirroring the picture is letter-boxed by the tablet; in DeX the app gets a 16:9 window. The TV edge
  margin setting adds padding for sets that crop the edges.
- **DeX.** The manifest declares `resizeableActivity`, handles size and density changes without
  restarting, and carries Samsung's keep-alive keys. DeX keeps its own home screen, so Qatar4u TV is opened
  from the DeX app list there. The app does not claim it can replace the DeX home.
- **Remote.** No HDMI-CEC path from a TV remote to the tablet should be assumed. Bluetooth remotes,
  keyboards and mice deliver standard key events (DPAD_*, ENTER, BACK); the app is navigable with those
  alone. To be confirmed on the real remote: see TEST_REPORT.md.
- **AMOLED.** Wallpapers change on a schedule, the screensaver dims and moves its text every minute.
  The clock in the header is the only element that stays in one place while Home is shown.
- **Performance budget.** 5.3 GB RAM leaves plenty of room; wallpapers are decoded at screen size only,
  icons are loaded off the main thread and cached by the list.
