# Lockdown

Android app blocker. Pick apps and a duration; until the timer ends the apps can't be opened and Lockdown can't be uninstalled.

## Build
- **GitHub:** push this repo, open the **Actions** tab, run **Build APK**, download `Lockdown-apk` (app-debug.apk).
- **Android Studio:** open the folder and press Run.

## First run
1. Enable the **Lockdown accessibility service**.
2. Enable **uninstall protection** (device admin).
3. Choose apps and minutes, then Start lock.

## Notes
- During a lock, **Settings and the package installer are blocked too**, so the lock can't be disabled or the app removed.
- Restart-safe: the end time is stored; the service restarts on boot.
- Safe mode can bypass this. Full protection needs Device Owner mode (ADB), not included.
