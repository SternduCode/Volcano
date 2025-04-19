plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
	}
}

rootProject.name = "Volcano"

include(":Util")
include(":JSONLib")
include(":Balancer")
include(":Multicore")
include(":Encryption")
include(":DataTransfer")
include(":Bridge")