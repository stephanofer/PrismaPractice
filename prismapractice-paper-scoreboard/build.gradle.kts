val paperApiVersion: String by project
val placeholderApiVersion: String by project
val scoreboardLibraryVersion: String by project

dependencies {
    implementation(project(":prismapractice-api"))
    implementation(project(":prismapractice-config"))

    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("me.clip:placeholderapi:$placeholderApiVersion")

    implementation("net.megavex:scoreboard-library-api:$scoreboardLibraryVersion")
}
