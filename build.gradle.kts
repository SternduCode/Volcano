plugins {
	alias(libs.plugins.kotlinJvm)
}

java {
	modularity.inferModulePath.set(true)
}

kotlin {
	jvmToolchain(libs.versions.jvm.get().toInt())
	compilerOptions {
		freeCompilerArgs.add("-jvm-default=enable")
	}
}