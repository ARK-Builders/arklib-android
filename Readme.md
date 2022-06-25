# Arklib For Android

An arklib wrapper for Android.

## Prerequisites

- Kotlin toolchain
- Android SDK + NDK r24 (latest)

## Debug

Run the command to build

```sh
./gradlew lib:assemble
```

Connect to a device or setup an AVD and check the functionality.

```sh
./gradlew appmock:connectedCheck
```

## Build

Before building for release, change those line in `lib/build.gradle`

```groovy
cargo {
    module  = "../arklib"
    libname = "arklib"
    targets = ["arm64", "x86", "x86_64", "arm"]
    prebuiltToolchains = true
//    Set the profile to "release"
    profile = "release"
}

```

Then run the command to build

```sh
./gradlew lib:assemble
```