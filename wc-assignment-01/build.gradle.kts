plugins {
    id("java")
}

group = "cicd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val junitVersion = "5.10.0"
    val apacheCommonsMathVersion = "3.6.1"
    val sl4jVersion = "2.0.16"
    val logbackVersion = "1.5.11"

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.apache.commons:commons-math3:$apacheCommonsMathVersion")
    implementation("org.slf4j:slf4j-api:$sl4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("ch.qos.logback:logback-core:$logbackVersion")

}

tasks.test {
    useJUnitPlatform()
}