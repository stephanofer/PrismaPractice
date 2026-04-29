val velocityApiVersion: String by project

plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.0"
    id("xyz.jpenilla.run-velocity") version "3.0.2"
}

dependencies {
    implementation(project(":prismapractice-api"))
    implementation(project(":prismapractice-command"))
    implementation(project(":prismapractice-config"))
    implementation(project(":prismapractice-core"))
    implementation(project(":prismapractice-data"))

    compileOnly("com.velocitypowered:velocity-api:$velocityApiVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityApiVersion")
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        destinationDirectory.set(rootProject.layout.projectDirectory.dir("target"))
        mergeServiceFiles()
        relocate("org.yaml.snakeyaml", "com.stephanofer.prismapractice.libs.snakeyaml")
    }

    build {
        dependsOn(shadowJar)
    }
}
