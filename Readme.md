# Arklib For Android

An arklib wrapper for Android.

## Prerequisites

- Kotlin toolchain
- Android SDK + NDK r24 (latest)

## Debug

Make sure you have switch to `debug` profile in cargo config, which could be found at `lib/build.gradle` 

Run the command to build

```sh
./gradlew lib:assemble
```

Connect to a device or setup an AVD and check the functionality.

```sh
./gradlew appmock:connectedCheck
```

## Build

Before make a release build, ensure you have set `profile = "release"` in cargo config.

```sh
./gradlew lib:assemble
```

The generated release build is `lib/build/outputs/aar/lib-release.aar`

## Publish New Version

Ensure you have committed your changes.

```sh
./gradlew release
```

Then simply push to the repo.

## Installation
Add the following script to project's build.gradle
```groovy
buildscript {
    repositories {
        maven { url "https://plugins.gradle.org/m2/" }
    }
    dependencies {
        classpath 'io.github.0ffz:gpr-for-gradle:1.2.1'
    }
}

allprojects {
    apply plugin: "io.github.0ffz.github-packages"
    repositories{
        maven githubPackage.invoke("ARK-Builders/arklib-android")
    }
}
```

And add arklib-android dependency to app module's build.gradle
```groovy
implementation 'space.taran:arklib:0.1.0-SNAPSHOT-7df9a4e581'
```
