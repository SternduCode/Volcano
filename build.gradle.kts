plugins {
	alias(libs.plugins.kotlinJvm)
}

java {
	modularity.inferModulePath.set(true)
}

kotlin {
	jvmToolchain(25)
	compilerOptions {
		freeCompilerArgs.add("-Xjvm-default=all")
	}
}