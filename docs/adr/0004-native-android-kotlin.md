# ADR-0004: Native Android (Kotlin/Compose) replaces React Native/Expo

## Status
Accepted

## Date
2026-11-22

## Context
The first mobile client was an Expo/React Native app in `mobile/`. We decided to build a fully native Android client instead.

## Decision
Replace `mobile/` (deleted 2026-11-22) with `android/`: Kotlin, Jetpack Compose, supabase-kt. It is a Gradle project outside the Bun workspace. Config comes from `android/local.properties` (`supabase.url`, `supabase.anonKey`; emulator uses `http://10.0.2.2:54321`). Distribution: debug APK via `./gradlew assembleDebug`, later GitHub Actions / Play internal track.

## Alternatives Considered
- Keep Expo/React Native: shares TS with web and covers iOS, but we preferred native performance and platform tooling.
- Kotlin Multiplatform: would cover iOS, but adds complexity now.

## Consequences
- Android only. No iOS client yet. Open question: Swift (SwiftUI) or Kotlin Multiplatform when iOS is needed.
- No code sharing with web; Kotlin models mirror the schema manually (ADR-0002).
- Android requires its own toolchain (JDK, Android SDK) and CI job.
