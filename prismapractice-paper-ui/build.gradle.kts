val paperApiVersion: String by project
val zmenuApiVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":prismapractice-config"))

    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("fr.maxlego08.menu:zmenu-api:$zmenuApiVersion")

    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("fr.maxlego08.menu:zmenu-api:$zmenuApiVersion")
}
