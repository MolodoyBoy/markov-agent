plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("com.google.cloud.tools.jib") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.markov.agent"
version = "0.0.1-SNAPSHOT"
description = "markov-agent"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.4.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot4:2.4.0")
    implementation("org.springframework.retry:spring-retry:2.0.12")
    implementation("org.springframework:spring-aspects:7.0.6")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0-M2"))

    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.ai:spring-ai-starter-model-openai:2.0.0-M2")

    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val dockerHubUsername: String by project
val imageVersion = System.getenv("IMAGE_VERSION")
val dockerHubPassword = System.getenv("DOCKER_HUB_PASSWORD")

jib {
    from {
        image = "eclipse-temurin:21-jdk"
    }

    to {
        image = "molodoyboy777/markov-agent:${imageVersion}"

        auth {
            username = dockerHubUsername
            password = dockerHubPassword
        }
    }

    container {
        jvmFlags = listOf("-XX:MaxRAMPercentage=80")
        mainClass = "com.markov.agent.MarkovAgentApplication"
    }
}
