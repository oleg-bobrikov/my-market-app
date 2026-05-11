plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "shop"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


dependencies {
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("org.projectlombok:lombok")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation ("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("com.github.f4b6a3:uuid-creator:5.3.7")
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("io.r2dbc:r2dbc-h2")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.github.codemonstur:embedded-redis:1.4.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

/**
 * Мы создаём свою конфигурацию mockitoAgent.
 * В ней будут лежать только jar-файлы, которые нужны для javaagent.
 * Это нужно, чтобы не смешивать agent-зависимости с обычным кодом.
 */
val mockitoAgent by configurations.creating

dependencies {
    mockitoAgent("net.bytebuddy:byte-buddy-agent:1.17.8")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off", "-javaagent:${mockitoAgent.asPath}")
}
