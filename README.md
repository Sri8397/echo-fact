# Suvidha Radar — GodsEye World Radar

Native Kotlin/Jetpack Compose migration, based on the approved Suvidha radar prototype.

This increment connects real USGS earthquakes and authenticated GodsEye flights. Google and email/password sign-in use the existing Supabase project. Set the public Supabase client configuration in ignored local.properties; Google requires Android package/signature registration. Guest access is limited to public radar data. No provider secrets or production signing files are embedded.

```sh
./gradlew -Pandroid.lint.useK2Uast=false :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK installs separately as `com.taurotechnologies.godseye.nativeapp.debug`. NASA imagery credits ship beside the assets; the launcher and header use the original GodsEye logo.
