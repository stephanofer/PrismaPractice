val snakeYamlVersion: String by project

plugins {
    `java-library`
}

dependencies {
    implementation("org.yaml:snakeyaml:$snakeYamlVersion")
}
