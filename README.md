[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ARK-Builders_arklib-android&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ARK-Builders_arklib-android)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ARK-Builders_arklib-android&metric=bugs)](https://sonarcloud.io/summary/new_code?id=ARK-Builders_arklib-android)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=ARK-Builders_arklib-android&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=ARK-Builders_arklib-android)

<a href="https://www.buymeacoffee.com/arkbuilders" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>

# ArkLib for Android

This is a wrapper of <a href="https://github.com/ARK-Builders/arklib" target="_blank">ArkLib</a> which enables you to build Android apps, powered by resource indexing, previews generation and user metadata support such as tags or scores.

| :warning: WARNING          |
|:---------------------------|
| The following information is only for developers. |

## Importing the library
**Github packages with credentials is a workaround since JCenter is shutdown**

Add the following script to project's `build.gradle`:

```groovy
allprojects {
    repositories{
        maven {
            name = "GitHubPackages"
            url = "https://maven.pkg.github.com/ARK-Builders/arklib-android"
            credentials {
                username = "token"
                password = "\u0037\u0066\u0066\u0036\u0030\u0039\u0033\u0066\u0032\u0037\u0033\u0036\u0033\u0037\u0064\u0036\u0037\u0066\u0038\u0030\u0034\u0039\u0062\u0030\u0039\u0038\u0039\u0038\u0066\u0034\u0066\u0034\u0031\u0064\u0062\u0033\u0064\u0033\u0038\u0065"
            }
        }
    }
}
```

And add `arklib-android` dependency to app module's `build.gradle`:
```groovy
implementation 'dev.arkbuilders:arklib:0.3.1'
```

## Development of the library

### Prerequisites

- Rust toolchain
- Kotlin toolchain
- Android SDK + NDK r24 (latest)

### Build Rust

You need to have Rust targets installed:
```sh
rustup target add armv7-linux-androideabi
rustup target add aarch64-linux-android
rustup target add i686-linux-android
rustup target add x86_64-linux-android
```

For checking if Rust code compiles without problems, you can use this command:

```sh
./gradlew cargoBuild
```

### Build AAR

Before make a release build, ensure you have set `profile = "release"` in cargo config.

```sh
./gradlew lib:assemble
```

The generated release build is `lib/build/outputs/aar/lib-release.aar`

### Publish New Version

Ensure you have committed your changes.

```sh
./gradlew release
```

Then simply push to the repo.

### Debug

Make sure you have switch to `debug` profile in cargo config, which could be found at `lib/build.gradle` 

Run the command to build

```sh
./gradlew lib:assemble
```

Connect to a device or setup an AVD and check the functionality.

```sh
./gradlew appmock:connectedCheck
```

### Unit tests

Unit tests require native ARK library file for host machine in project root directory.

- ```libarklib.so``` for Linux
- ```libarklib.dylib``` for Mac
- ```libarklib.dll``` for Windows

Unit tests depend on ```buildRustLibForHost``` gradle task (Linux, Mac)

But you can do it manually:

- Find out host architecture ```rustc -vV | sed -n 's|host: ||p'```
- Change to `arklib` directory and build the library ```cargo build --target $host_arch```
- Copy library from ```arklib/target/$host_arch/debug/libarklib.(so|dylib|dll)``` to project root directory

Shortcut for Linux:
```sh
ARCH=$(rustc -vV | sed -n 's|host: ||p') cargo build --target $ARCH && cp arklib/target/$ARCH/debug/libarklib.so .
```

