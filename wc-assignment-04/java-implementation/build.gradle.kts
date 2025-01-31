plugins {
    id("java")
}

group = "at.hagenberg.fh.wc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val openCsvVersion = "5.10"
val apacheCommonsMathVersion = "3.6.1"
val slf4jVersion = "2.0.16"

dependencies {
    implementation("com.opencsv:opencsv:$openCsvVersion")
    implementation("org.apache.commons:commons-math3:$apacheCommonsMathVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
}

tasks.test {
    useJUnitPlatform()
}