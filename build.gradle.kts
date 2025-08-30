plugins {
    id("java")
    `java-library`
}

group = "io.aitchn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io"){
        name = "jitpack"
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.20.0"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    api("org.slf4j:slf4j-api:2.0.17")
}

tasks.test {
    useJUnitPlatform()
}