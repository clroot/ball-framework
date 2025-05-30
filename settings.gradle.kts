plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ball-framework"

// # Shared modules
include("shared")
include("shared:arrow")
include("shared:lock")

// # Domain modules
include("domain")

// # Application modules
include("application")

// # Adapter modules
include("adapter")

// ## Adapter Shared modules
include("adapter:shared")
include("adapter:shared:messaging-common")

// ## Adapter Outbound modules
include("adapter:outbound")
// ### Data Access Adapter
include("adapter:outbound:data-access-core")
include("adapter:outbound:data-access-jpa")
include("adapter:outbound:data-access-redis")
// ### Event Publisher Adapter
include("adapter:outbound:event-publisher-core")
include("adapter:outbound:event-publisher-domain")

// ## Adapter Inbound modules
include("adapter:inbound")
// ### HTTP Adapter
include("adapter:inbound:rest")
// ### Event Consumer Adapter
include("adapter:inbound:event-consumer-core")
include("adapter:inbound:event-consumer-domain")

// # Starter
include("ball-starter")