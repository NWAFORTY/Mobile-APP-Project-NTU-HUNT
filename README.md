# Mobile-APP-Project-NTU-HUNT
Android treasure hunt app that gamifies exploring NTU campus with GPS clues, badges and a live leaderboard. Built with Java, Google Maps and Firebase.
# NTU Hunt — GPS Treasure Hunt App for Campus Exploration

An Android app that turns Nottingham Trent University's campus into a GPS-based treasure hunt. Players follow riddle-style clues to real locations, collect treasures when they arrive, earn points and badges, and compete on a live leaderboard.

Built in Java for the **ITEC31041 Mobile Platform Applications** module at Nottingham Trent University.

**Presentation (YouTube):** [https://youtu.be/VTrgHHcw0wM](https://youtu.be/VTrgHHcw0wM)

## The problem

Many students stick to a few familiar routes between lectures, accommodation and food, and never discover the wider campus: libraries, study hubs, sports facilities, support services. NTU Hunt gamifies exploration to encourage students to move around, discover facilities and meet people. The full proposal, market research and impact analysis are in [`docs/App-Proposal-and-Impact-Report.docx`](docs/App-Proposal-and-Impact-Report.docx).

## Features

- **GPS treasure hunting:** pick a treasure, read its riddle clue (with an optional hint), then walk to the area. The target pin only appears when you are within about 40 m, and you must be within about 28 m to collect it.
- **Seven campus treasures** across three rarity tiers (Common, Rare, Legendary), each worth points.
- **Walk tracking:** accumulates walking distance from GPS updates and unlocks distance milestone badges.
- **Achievements:** badges for walking distance and number of treasures found, shown on the profile.
- **Live leaderboard:** top players ranked by score, updated in real time from Cloud Firestore. Tap a player to view their public stats.
- **Accounts:** Firebase Authentication, restricted to NTU student email addresses (`@my.ntu.ac.uk`), with input validation and password reset.
- **Cloud sync:** score, walk distance and found treasures are synced to Firestore per user.
- **In-app support:** a form (bug, feedback, GPS issue, account, other) that submits reports to Firestore.
- **First-launch tutorial**, profile and settings screens.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 11 |
| Platform | Android (minSdk 24, targetSdk 36) |
| Maps and location | Google Maps SDK, Fused Location Provider |
| Backend | Firebase Authentication, Cloud Firestore, Firebase Analytics |
| UI | Material Components, ViewBinding, RecyclerView, Fragments |
| Build | Gradle (Kotlin DSL), version catalog |

## Project structure

```
app/src/main/java/com/example/treasurehuntapplication/
  MainActivity, Login, Signup, navigation   - entry, auth and main navigation
  MapFragment                               - map, proximity detection, collecting
  TreasureFragment, TreasureManager         - treasure catalogue and found state
  ActiveTreasureManager                     - the treasure currently being hunted
  WalkDistanceTracker, *Achievements        - distance tracking and badges
  LeaderboardFragment / Adapter / Model     - live leaderboard
  ProfileFragment, SettingsFragment, SupportFragment
  *FirestoreSync, ScoreRepository           - local state and cloud sync
  AuthInputValidator                        - email and password validation
docs/                                       - proposal report and testing report
```

## Getting started

You need Android Studio, a Google Maps API key and your own Firebase project.

1. Clone the repo and open it in Android Studio.
2. **Maps key:** copy `local.properties.example` to `local.properties` and set `MAPS_API_KEY` (this file is gitignored).
3. **Firebase:** create a Firebase project, add an Android app with package name `com.example.treasurehuntapplication`, enable **Authentication (Email/Password)** and **Cloud Firestore**, then download `google-services.json` into `app/`. See `app/google-services.json.example` for the expected shape. It is gitignored.
4. Sync Gradle and run on a device or emulator with Google Play services. Grant location permission when prompted.

Treasure coordinates in `TreasureManager.java` are real NTU campus locations, so collecting treasures requires being physically nearby.

## Testing and user feedback

Testing notes and feedback from two participants are in [`docs/Testing-Report-and-User-Feedback.docx`](docs/Testing-Report-and-User-Feedback.docx). Early feedback asked for a first-launch tutorial and the ability to view other players' stats on the leaderboard. The app now includes both.

A walkthrough of the app is in the [YouTube presentation](https://youtu.be/VTrgHHcw0wM).

## Future work

Augmented reality clues, QR code checkpoints, light and dark mode, seasonal and event hunts, multi-campus support, and anonymised engagement analytics for university staff.

## Author

**Tobenna Nwafor** — BSc Computer Science, Nottingham Trent University
