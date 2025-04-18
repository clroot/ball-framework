plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ball-framework"

// Shared modules
include("shared")
include("shared:core")
include("shared:arrow")
include("shared:attribute")
include("shared:lock")
include("domain")
include("application")