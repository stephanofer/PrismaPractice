import org.gradle.api.plugins.JavaPluginExtension

plugins {
    base
}

val junitVersion: String by project
val mockitoVersion: String by project

allprojects {
    group = "com.stephanofer.practice"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/releases/")
        maven("https://maven.maxhenkel.de/repository/public")
        maven("https://jitpack.io")
        maven("https://repo.groupez.dev/releases")
    }
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    dependencies {
        add("testImplementation", platform("org.junit:junit-bom:$junitVersion"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testImplementation", "org.mockito:mockito-core:$mockitoVersion")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("-Dnet.bytebuddy.experimental=true")
    }

    tasks.withType<Jar>().configureEach {
        destinationDirectory.set(rootProject.layout.projectDirectory.dir("target-internal"))
        archiveVersion.set(project.version.toString())
    }
}


tasks.named<Delete>("clean") {
    delete(layout.projectDirectory.dir("target"))
    delete(layout.projectDirectory.dir("target-internal"))
}
