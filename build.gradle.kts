plugins {
	kotlin("jvm") version "2.1.0"
}

java {
	modularity.inferModulePath.set(true)
}

kotlin {
	jvmToolchain(23)
	compilerOptions {
		freeCompilerArgs.add("-Xjvm-default=all")
	}
}