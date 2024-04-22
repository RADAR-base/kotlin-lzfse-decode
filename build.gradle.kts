import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.github.ben-manes.versions") version "0.38.0" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

group = "org.radarbase"
version = "0.1.1"
description = "LZFSE input stream"

repositories {
    mavenCentral()

    // Temporary until Dokka is fully published on maven central.
    // https://github.com/Kotlin/kotlinx.html/issues/81
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

dependencies {
    api(kotlin("stdlib-jdk8"))

    val findbugsVersion: String by project
    testImplementation("com.google.code.findbugs:jsr305:$findbugsVersion")

    val junitVersion: String by project
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.hamcrest:hamcrest-core:2.2")

    val dokkaVersion: String by project
    configurations["dokkaHtmlPlugin"]("org.jetbrains.dokka:kotlin-as-java-plugin:$dokkaVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.apiVersion = "1.4"
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs = listOf(
        "-Xno-call-assertions",
        "-Xno-param-assertions",
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.wrapper {
    gradleVersion = "8.4"
}

val githubRepoName = "RADAR-base/kotlin-lzfse-decode"
val githubUrl = "https://github.com/$githubRepoName.git"
val issueUrl = "https://github.com/$githubRepoName/issues"
val website = "https://radar-base.org"

//---------------------------------------------------------------------------//
// Packaging                                                                 //
//---------------------------------------------------------------------------//

val jar by tasks.getting(Jar::class) {
    manifest.attributes.apply {
        put("Implementation-Title", rootProject.name)
        put("Implementation-Version", project.version)
        put("Built-JDK", System.getProperty("java.version"))
        put("Built-Gradle", project.gradle.gradleVersion)
    }
}

tasks.withType<Tar> {
    compression = Compression.GZIP
    archiveExtension.set("tar.gz")
}

// custom tasks for creating source/javadoc jars
val sourcesJar by tasks.creating(Jar::class) {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
    val classes by tasks
    dependsOn(classes)
}

val dokkaJar by tasks.registering(Jar::class) {
    from("$buildDir/dokka/javadoc")
    archiveClassifier.set("javadoc")
    val dokkaJavadoc by tasks
    dependsOn(dokkaJavadoc)
}

val assemble by tasks
assemble.dependsOn(sourcesJar)
assemble.dependsOn(dokkaJar)

publishing {
    publications {
        create<MavenPublication>("mavenJar") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set(githubUrl)

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("blootsvoets")
                        name.set("Joris Borgdorff")
                        email.set("joris@thehyve.nl")
                        organization.set("The Hyve")
                    }
                }
                issueManagement {
                    system.set("GitHub")
                    url.set(issueUrl)
                }
                organization {
                    name.set("RADAR-Base")
                    url.set(website)
                }
                scm {
                    connection.set("scm:git:$githubUrl")
                    url.set(githubUrl)
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    isRequired = true
    sign(tasks["sourcesJar"], tasks["dokkaJar"])
    sign(publishing.publications["mavenJar"])
}

tasks.withType<Sign> {
    onlyIf { gradle.taskGraph.hasTask(project.tasks["publish"]) }
}

fun Project.propertyOrEnv(propertyName: String, envName: String): String? {
    return if (hasProperty(propertyName)) {
        property(propertyName)?.toString()
    } else {
        System.getenv(envName)
    }
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(propertyOrEnv("ossrh.user", "OSSRH_USER"))
            password.set(propertyOrEnv("ossrh.password", "OSSRH_PASSWORD"))
        }
    }
}
