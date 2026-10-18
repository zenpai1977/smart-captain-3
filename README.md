# Smart Captain Android — rule-based Accessibility automation

This project includes a real Android AccessibilityService, editable on-device rules, a conservative offer-text parser, rule evaluation, and sync with the existing Smart Captain driver endpoint.

## Implemented in source
- Minimum fare, maximum pickup km, minimum trip km, destination keyword.
- Platform filters (Rapido, Uber, Porter) and vehicle/order filters (Bike, Auto, Cab, Parcel).
- Cloud pass and rule sync from `https://smart-captain.onrender.com/api/driver?phone=...`.
- Accessibility service restricted to exact configurable package IDs.
- Local-only latest screen text and decision diagnostics.
- Optional auto-accept, default OFF. It requires Accessibility consent, active cloud-pass status, filters enabled, a complete parsed offer, and all rules matching.
- The action finds an enabled control with a clearly labelled Accept-like text and sends `ACTION_CLICK`; it does not click when parsing is incomplete, rules do not match, the pass is inactive, or the label is unknown.
- 90-second duplicate-screen debounce.
- Unit tests for rule evaluation and sample parser behavior.
- GitHub Actions workflow that runs tests and creates a debug APK artifact.

## Not yet verified — important
The shared rule engine and sample offer parser compile independently with Kotlin and their sample parsing checks passed. I could not compile the full Android app in this environment because the Android SDK/Gradle toolchain is not installed. The source has **not** been tested on a real phone. The package-ID defaults are starting values and must be verified on the phone. Each driver app/version/language may expose different text, labels, and buttons. Until you compare the captured text and decisions with actual offers, auto-accept should remain OFF. Even if the app sends a click, it cannot guarantee that the platform accepted the ride.

The parser deliberately fails closed if it cannot confidently find a currency amount and labelled pickup and trip distances. Some app versions may not expose those elements through Accessibility, in which case per-app parser changes are required.

## Cloud sync
The app expects the existing backend response's `active` boolean and `rules` object fields: `activeSwitch`, `minTripKm`, `maxPickupKm`, `minFare`, and `goToArea`. It does not embed a Supabase service-role key and does not replace the existing site or database. The current server endpoint identifies drivers by phone number alone; backend authentication should be reviewed separately before distributing the APK.

## Build APK with GitHub Actions
1. Extract the ZIP.
2. Upload/commit the extracted **contents** to the repository root, preserving `.github/workflows/build-android.yml`. Do not place the workflow under an extra Android subfolder.
3. Commit to `main`, or go to GitHub → Actions → **Build Smart Captain Android APK** → Run workflow.
4. Download the `SmartCaptain-debug-apk` artifact from the completed workflow run.
5. The artifact is a debug APK for testing, not a signed release APK.

For local Android Studio use, open this project root, let Android Studio sync/install the SDK, then run `app > Tasks > build > assembleDebug`. This ZIP does not include a Gradle wrapper binary; GitHub Actions installs Gradle 8.9. If Android Studio needs a wrapper, generate one from the project root with `gradle wrapper --gradle-version 8.9`.

## First test
1. Install the debug APK on a test phone while parked.
2. Enter the same phone number saved on the Smart Captain website and tap **Sync rules & check pass**.
3. Verify every filter and package ID.
4. Read the disclosure and grant local-screen monitoring consent.
5. Open Android Accessibility settings and enable Smart Captain.
6. Leave auto-accept OFF. Open a driver app, then refresh the monitor result and compare the captured text/decision to the visible offer.
7. Only test automatic clicks while stationary and where the driver platform permits this use. Always verify the resulting screen.

Never configure or depend on this app while the vehicle is moving.
