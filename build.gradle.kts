plugins {
    id("java")
    id("java-library")
    id("com.gradleup.shadow") version "8.3.9"
    eclipse
}

group = "com.extendedclip.papi.expansion.essentials"
version = "1.5.2"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://repo.purpurmc.org/snapshots") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.essentialsx.net/releases/") }
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.github.PlaceholderAPI:PlaceholderAPI:2.11.2")
    compileOnly("net.essentialsx:EssentialsX:2.21.0") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set(rootProject.name)
    manifest {
        attributes(
            "Implementation-Title" to rootProject.name,
            "Implementation-Version" to project.version,
            "Specification-Title" to rootProject.name,
            "Specification-Version" to project.version,
        )
    }
}

tasks.jar {
    archiveClassifier.set("part")
}

tasks.shadowJar {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
}
