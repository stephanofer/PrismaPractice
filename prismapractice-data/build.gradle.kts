val hikariVersion: String by project
val mysqlConnectorVersion: String by project
val lettuceVersion: String by project
val flywayVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":prismapractice-api"))
    api(project(":prismapractice-config"))
    api(project(":prismapractice-core"))

    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("com.mysql:mysql-connector-j:$mysqlConnectorVersion")
    implementation("io.lettuce:lettuce-core:$lettuceVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-mysql:$flywayVersion")
}
