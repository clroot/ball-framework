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

// Domain modules
include("domain")

// Application modules
include("application")

// Adapter modules
include("adapter")
// Adapter Outbound modules
include("adapter:outbound")
include("adapter:outbound:persistence-jpa")
include("adapter:outbound:persistence-redis")
include("adapter:outbound:messaging-producer-core")
include("adapter:outbound:messaging-producer-kafka")
// Adapter Inbound modules
include("adapter:inbound")
include("adapter:inbound:rest")