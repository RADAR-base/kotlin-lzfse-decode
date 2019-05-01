import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `java-library`
    application
    kotlin("jvm") version "1.3.31"
}

val run: JavaExec by tasks
run.standardInput = System.`in`

application {
    mainClassName = "com.github.horrorho.ragingmoose.MainKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testImplementation("org.hamcrest:hamcrest-core:1.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.apiVersion = "1.3"
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs = listOf(
            "-Xno-call-assertions",
            "-Xno-param-assertions"
    )
}

tasks.test {
    useJUnitPlatform()
}

tasks.wrapper {
    gradleVersion = "5.4"
}
