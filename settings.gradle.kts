plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ball-framework"

// # Shared modules
include("shared")
include("shared:arrow")
include("shared:jackson")
include("shared:lock")

// # Domain modules
include("domain")

// # Application modules
include("application")

// # Adapter modules
include("adapter")

// ## Adapter Outbound modules
include("adapter:outbound")
// ### Data Access Adapter
include("adapter:outbound:data-access-core")
include("adapter:outbound:data-access-jpa")
include("adapter:outbound:data-access-redis")

// ## Adapter Inbound modules
include("adapter:inbound")
// ### HTTP Adapter
include("adapter:inbound:rest")

// # Starter
include("ball-starter")