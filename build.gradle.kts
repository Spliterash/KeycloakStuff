plugins {
    java
}

group = "ru.spliterash"
version = "1.0.0"

repositories {
    mavenCentral()
}
java.toolchain.languageVersion = JavaLanguageVersion.of(17)

val autoServiceVersion = "1.1.1"
val keycloakVersion = "24.0.3"

dependencies {
    compileOnly("com.google.auto.service:auto-service:$autoServiceVersion")
    annotationProcessor("com.google.auto.service:auto-service:$autoServiceVersion")


    // Keycloak dependencies
    compileOnly("org.keycloak:keycloak-server-spi:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-server-spi-private:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-services:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-saml-core-public:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-model-jpa:$keycloakVersion")

    // Additional dependencies
    compileOnly("com.google.guava:guava:32.0.0-jre")
    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
}

