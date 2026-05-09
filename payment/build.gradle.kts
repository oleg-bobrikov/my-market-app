import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.openapi.generator") version "7.17.0"
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/openapi.yaml")
    outputDir.set("$projectDir/build/generated")
    apiPackage.set("ru.yandex.practicum.payment.api")
    modelPackage.set("ru.yandex.practicum.payment.model")
    configOptions.set(mapOf(
        "interfaceOnly" to "true",
        "useSpringBoot3" to "true",
        "useSpringController" to "true",
        "reactive" to "true"
    ))
}

tasks.register<GenerateTask>("openApiGenerateClient") {
    description = "generate client"
    generatorName.set("java")
    library.set("webclient")
    inputSpec.set("$rootDir/openapi.yaml")
    outputDir.set("$projectDir/build/generated")
    apiPackage.set("ru.yandex.practicum.payment.client.api")
    modelPackage.set("ru.yandex.practicum.payment.model")
    configOptions.set(mapOf(
        "useSpringBoot3" to "true",
        "reactive" to "true"
    ))
}



openApiValidate {
    inputSpec.set("$rootDir/openapi.yaml")
}

description = "payment"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("io.asyncer:r2dbc-mysql") // optional, if needed, but properties say h2
    implementation("io.r2dbc:r2dbc-h2")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.22")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("io.projectreactor:reactor-tools")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
