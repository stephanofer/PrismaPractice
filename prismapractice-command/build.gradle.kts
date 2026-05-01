val paperApiVersion: String by project
val velocityApiVersion: String by project

plugins {
    `java-library`
}

dependencies {
    implementation(project(":prismapractice-api"))
    implementation(project(":prismapractice-core"))
    implementation(project(":prismapractice-data"))
    implementation(project(":prismapractice-debug"))

    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
}
