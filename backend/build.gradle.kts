plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.stockpredictor"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Phase 6: cache + distributed rate limiting (OCI Always Free plan — see CLAUDE.md's Phase 6
    // architecture). Fixes ChatbotRateLimiter's documented single-JVM limitation and backs the
    // new PredictionRateLimiter; does not replace Postgres, which stays the durable source of
    // truth for prediction_cache/alert_cooldowns.
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Phase 6: structured (JSON) logging — Docker container logs are the primary logging story
    // (see CLAUDE.md's Phase 6 plan step 4), readable directly via `docker logs`/`docker compose
    // logs` with no ingestion limit to worry about, and shippable to OCI Logging later if its free
    // allowance proves sufficient.
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Firebase Admin SDK — verifies ID tokens server-side; never issues its own JWTs (Phase 2.5/3
    // design: Firebase stays the single auth system). Reads credentials via Application Default
    // Credentials, i.e. the GOOGLE_APPLICATION_CREDENTIALS env var — see config/FirebaseAdminTokenVerifier.
    implementation("com.google.firebase:firebase-admin:9.10.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // No Docker available in this environment, so integration tests run against the real local
    // Postgres instance (a dedicated stockpredictor_test database) rather than Testcontainers.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
