pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Reown/WalletConnect AppKit depends on a few artifacts only published to JitPack
        // (e.g. com.walletconnect.Scarlet). Scoped to that group so no other dependency in the
        // project can ever resolve from JitPack.
        maven("https://jitpack.io") {
            // Exactly the JitPack-only groups Reown AppKit 1.6.12 needs -- listed one by one rather
            // than "com.github.*" so an unrelated dependency can never silently resolve from here.
            content {
                includeGroupByRegex("com\\.walletconnect.*")
                includeGroupByRegex("com\\.github\\.komputing.*")        // kethereum: BIP-39/32, secp256k1
                includeGroup("com.github.alexzhirkevich")                // QR code generator (connect modal)
                includeGroup("com.github.multiformats")                  // java-multibase
            }
        }
    }
}

rootProject.name = "My Application"
include(":app")
 