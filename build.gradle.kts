import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.DokkaTask
import com.jfrog.bintray.gradle.BintrayExtension
import java.util.Date
import java.net.URL

plugins {
    `java-library`
    kotlin("jvm") version "1.3.31"
    `maven-publish`
    id("org.jetbrains.dokka") version "0.9.18"
    id("com.jfrog.bintray") version "1.8.4"
}

group = "org.radarbase"
version = "0.1.0"
description = "LZFSE input stream"

repositories {
    jcenter()
}

dependencies {
    api(kotlin("stdlib-jdk8"))

    testImplementation("com.google.code.findbugs:jsr305:3.0.2")
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
    gradleVersion = "5.4.1"
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

tasks.withType(Tar::class.java){
    compression = Compression.GZIP
    archiveExtension.set("tar.gz")
}

// custom tasks for creating source/javadoc jars
val sourcesJar by tasks.creating(Jar::class) {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
    jdkVersion = 8
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(dokka)
}

artifacts.add("archives", dokkaJar)
artifacts.add("archives", sourcesJar)


publishing {
    publications {
        create<MavenPublication>("mavenJar") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)
            pom {
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

bintray {
    user = (if (project.hasProperty("bintrayUser")) project.property("bintrayUser") else System.getenv("BINTRAY_USER")).toString()
    key = (if (project.hasProperty("bintrayApiKey")) project.property("bintrayApiKey") else System.getenv("BINTRAY_API_KEY")).toString()
    override = false
    setPublications("mavenJar")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = project.group.toString()
        name = rootProject.name
        userOrg = "radar-base"
        desc = project.description
        setLicenses("Apache-2.0")
        websiteUrl = website
        issueTrackerUrl = issueUrl
        vcsUrl = githubUrl
        githubRepo = githubRepoName
        githubReleaseNotesFile = "README.md"
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version.toString()
            desc = project.description
            vcsTag = System.getenv("TRAVIS_TAG")
            released = Date().toString()
        })
    })
}
