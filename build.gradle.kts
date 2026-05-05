plugins {
    java
    id("org.springframework.boot") version "3.5.14" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "ru.yandex.practicum"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
