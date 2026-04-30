val paperApiVersion: String by project
val mockBukkitPaperVersion: String by project
val scoreboardLibraryVersion: String by project
val voicechatApiVersion: String by project
val lettuceVersion: String by project

plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

dependencies {
    implementation(project(":prismapractice-api"))
    implementation(project(":prismapractice-command"))
    implementation(project(":prismapractice-config"))
    implementation(project(":prismapractice-core"))
    implementation(project(":prismapractice-data"))

    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("de.maxhenkel.voicechat:voicechat-api:$voicechatApiVersion")

    implementation("net.megavex:scoreboard-library-api:$scoreboardLibraryVersion")
    runtimeOnly("net.megavex:scoreboard-library-implementation:$scoreboardLibraryVersion")

}

tasks {
    processResources {
        val props = mapOf("version" to version, "lettuceVersion" to lettuceVersion)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        destinationDirectory.set(rootProject.layout.projectDirectory.dir("target"))

        dependencies {
            exclude(dependency("io.lettuce:lettuce-core"))
            exclude(dependency("io.netty:.*"))
            exclude(dependency("io.projectreactor:reactor-core"))
            exclude(dependency("org.reactivestreams:reactive-streams"))
            exclude(dependency("redis.clients.authentication:redis-authx-core"))
        }

        mergeServiceFiles()
        relocate("org.yaml.snakeyaml", "com.stephanofer.prismapractice.libs.snakeyaml")
    }

    build {
        dependsOn(shadowJar)
    }
}
