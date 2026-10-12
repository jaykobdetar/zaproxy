import org.zaproxy.gradle.addon.AddOnStatus

plugins {
    java
    id("org.zaproxy.add-on") version "0.13.1"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "org.zaproxy.addon"
version = "0.1.2"
description = "Preview, compare, and export private summaries of existing HTTP messages."

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

zapAddOn {
    addOnName.set("Private Evidence")
    addOnStatus.set(AddOnStatus.ALPHA)
    manifest {
        author.set("jaykobdetar")
        url.set("https://github.com/jaykobdetar/zaproxy")
        repo.set("https://github.com/jaykobdetar/zaproxy")
        zapVersion.set("2.17.0")
    }
}

dependencies {
    zap("org.zaproxy:zap:2.17.0")
    testImplementation("org.zaproxy:zap:2.17.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.named<Jar>("jarZapAddOn") {
    from(layout.projectDirectory.file("../../LICENSE")) {
        into("META-INF")
    }
    from("NOTICE") {
        into("META-INF")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxHeapSize = "192m"
    maxParallelForks = 1
    jvmArgs("-XX:ActiveProcessorCount=2")
    systemProperty("java.awt.headless", "true")
}

tasks.named<Test>("test") {
    exclude("**/PackagedAddOnTest.class")
}

val packageSmokeTest by tasks.registering(Test::class) {
    dependsOn(tasks.named("jarZapAddOn"), tasks.named("testClasses"))
    testClassesDirs =
        sourceSets.test
            .get()
            .output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/PackagedAddOnTest.class")
    val packageFile = tasks.named<Jar>("jarZapAddOn").flatMap { it.archiveFile }
    inputs.file(packageFile)
    systemProperty("privateevidence.package", packageFile.get().asFile.absolutePath)
}

tasks.named("check") {
    dependsOn(packageSmokeTest)
}

spotless {
    java {
        target("src/**/*.java", "qa/**/*.java")
        googleJavaFormat("1.24.0").aosp()
    }
    kotlinGradle {
        ktlint("1.3.1")
    }
}
