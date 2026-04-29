val paperApiVersion: String by project
val velocityApiVersion: String by project

plugins {
    `java-library`
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
}
