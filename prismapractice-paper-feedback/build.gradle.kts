val paperApiVersion: String by project

dependencies {
    implementation(project(":prismapractice-feedback"))

    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")

    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
}
