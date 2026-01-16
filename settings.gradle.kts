plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
	}
}

rootProject.name = "Volcano"

include(":Util")
include(":Balancer")
include(":Multicore")
include(":Encryption")
include(":DataTransfer")
include(":Bridge")