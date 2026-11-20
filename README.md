# GreenO2 🌿

An Android app that tracks low-carbon trips (walking, cycling, e-bikes, e-scooters, bus, train) with GPS, calculates the CO₂ you saved compared with driving, and rewards you with credits you can spend in an in-app mart.

Built with **Kotlin** and **Jetpack Compose**. (Originally scoped as "Green Miles" in the project vision.)

## Features

| Capability (from project data sheet) | Where it lives |
| --- | --- |
| Registration / login / logout | `ui/screens/AuthScreen.kt`, `data/GreenRepository.kt` (salted PBKDF2 passwords) |
| Choose transport mode (Walk, Bicycle, E-bike, E-scooter, Bus, Train, Car) | `data/Models.kt` → `TransportMode` |
| User profile with total distance, emissions, credits | `ui/screens/ProfileScreen.kt` |
| Start/stop GPS trip tracking, works with the screen off | `tracking/TrackingService.kt` (foreground service, fused location) |
| Real-time distance with GPS noise filtering | `domain/DistanceAccumulator.kt` |
| Distance × mode → carbon saved + credits earned | `domain/CarbonCalculator.kt` |
| Anti-cheat: speed checks per mode, minimum trip, daily credit cap | `domain/TripValidator.kt`, `GreenRepository.saveTrip` |
| Green Points redemption for rewards | `ui/screens/MartScreen.kt` (`RewardsScreen`) |
| Community challenges + ranking | `ui/screens/LeaderboardScreen.kt` (`CommunityScreen`) |
| Trip history with weekly CO₂ chart | `ui/screens/HistoryScreen.kt` |
| Eco levels (Seedling → Forest guardian), achievements, daily eco tips | `domain/Gamification.kt` |

## How the carbon numbers work

- Every trip is compared with driving the same distance alone in an average car (**0.17 kg CO₂e/km**).
- Saved = distance × (car factor − mode factor). Car trips are recorded but save nothing.
- **1 Green Point = 100 g CO₂ avoided.** At most 300 trip points per day; quiz and challenge bonuses come on top.
- Factors are rounded estimates based on UK government (DESNZ/DEFRA) greenhouse-gas conversion factors. They all live in `TransportMode` so they can be reviewed and adjusted in one place.

| Mode | kg CO₂e / km |
| --- | --- |
| Walk, Bicycle | 0 |
| E-bike | 0.005 |
| E-scooter, Train | 0.035 |
| Bus | 0.10 |
| Car | 0.17 |

### Anti-cheat rules
- Trips under 100 m or 1 minute earn nothing.
- If the average speed is impossible for the chosen mode (for example, "walking" at 40 km/h), the trip is **flagged**, earns no credits and is excluded from the leaderboard.
- A sustained GPS segment faster than twice the mode's limit is also flagged.
- GPS fixes worse than 35 m accuracy are ignored, and jitter while standing still is not counted as distance.

## Alignment with the GreenMiles UX case study

The app follows the GreenMiles UX case study (amadezign.com/green-miles-app), including the changes its usability tests called for:

| Case study finding / IA item | In the app |
| --- | --- |
| Users wanted to **skip the onboarding quiz** | Welcome page with a "Take Quiz" modal: **Let's start!** or **Skip quiz**. Skippers get a reminder card on Track and can take it later from Profile. (`OnboardingScreen.kt`) |
| Users wanted **quiz progress indicators** | One question per page with **"Question n/5"** and a progress bar |
| 5-question personalisation quiz | Gives a 0–100 sustainability score, a default travel mode and a +50 Green Points welcome bonus (`domain/Quiz.kt`) |
| Users were **confused how rewards work** | **How Rewards Work** guide at the top of Rewards until dismissed, and always available from "ⓘ How it works" |
| Add an **Invite & Earn** section | "Help the Planet, Help Your Wallet – Invite & Earn!" card in Profile (Android share sheet with an invite code) |
| Achievements headboard with **Certificate** | Congratulations banner + shareable certificate in Profile |
| Green Points wallet & redeemable rewards | Credits are now **Green Points**; rewards are grouped into Transport / Eco-stays / Food & drink / Activities tabs |
| Community & Challenges, social sharing | **Community** tab: weekly challenges you join, track and claim points for, and share |
| User impact leaderboard | Leaderboard with podium inside Community |
| Trip planner with real-time CO₂ and alternative green travel | **Plan a trip** card on Track: distance slider, CO₂ per mode, greener suggestion |
| Location access step | A short explanation shown before the system location prompt |
| Support & feedback (Help center, FAQs) | **Help & FAQ** in Profile |
| Visual style | Deep olive `#2F4423` on white, outlined cards, serif headings, solid olive splash |

Not built (they need a backend or partners): social login (Facebook/Google/Apple), the community forum, verified referral bonuses, and real partner vouchers.

## Design

- **App icon:** a white map pin (tracking) holding a green leaf (sustainability), with O₂ bubbles rising from it, on a deep olive gradient. It's an adaptive icon with a monochrome layer for Android 13+ themed icons (`res/drawable/ic_launcher_*.xml`). A 512 px version for the Play Store is at `app/src/main/ic_launcher-playstore.png`.
- **Look:** brand gradients (`ui/theme/Color.kt` → `Brand`), glass-style stat chips on gradient cards, a pulsing LIVE card while recording, a podium for the top 3, and a gold balance card in the Mart. Shared building blocks live in `ui/components/Components.kt`.

## Architecture

```
ui/            Compose screens + AppViewModel (state holder)
tracking/      TrackingService (foreground GPS) + TrackingStore (live trip state)
domain/        Pure Kotlin logic: CarbonCalculator, TripValidator, DistanceAccumulator, PasswordHasher
data/          GreenRepository (single entry point), GreenDatabase (SQLite), SessionManager
```

The project data sheet asks for a client–server structure, but cost is a fixed constraint. So v1 keeps data on the device in SQLite, and **all reads and writes go through `GreenRepository`**. A backend (Firebase or Supabase free tier, or a REST API) can replace the SQLite calls later without changing any screen. The scoring logic in `domain/` has no Android dependencies, so it can be reused on a server.

The leaderboard is seeded with four users marked **demo** so it isn't empty on a fresh install. Remove `seedDemoCommunity` in `GreenDatabase.kt` once there's a real backend.

## Running it

1. Open the `GreenO2` folder in Android Studio (Narwhal / AGP 9.1 or newer).
2. Let Gradle sync, then run the `app` configuration on a device or emulator (Android 8.0+, API 26).
3. Create an account, choose a mode, then tap **Start trip**. Allow precise location and notifications.

**Testing on an emulator:** open *Extended controls → Location → Routes*, set a route and press play. The app will track it like a real trip.

Unit tests (calculator, anti-cheat, distance filter, password hashing):

```bash
./gradlew testDebugUnitTest
```

## CI

`.github/workflows/android.yml` runs the unit tests and builds a debug APK on every push to `main`. You can download the APK from the workflow run's **Artifacts** section.

## Roadmap ideas
- Sync to a backend so the leaderboard covers all users, not just this device.
- Use Activity Recognition to auto-detect the mode (currently the user picks it, and the speed checks catch mismatches).
- Community challenges (e.g. "campus bike week").
- Partner-verified vouchers and carbon-offset marketplace.
- Change the `applicationId` from `com.example.greeno2` before publishing to Google Play (Play rejects `com.example`).
