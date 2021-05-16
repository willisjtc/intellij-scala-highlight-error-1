import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin ("jvm") version "1.4.21"
  application
  id("com.github.johnrengelman.shadow") version "6.1.0"
  scala
}

group = "com.gamebot"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
  mavenLocal()
}

val vertxVersion = "4.0.3"
val junitJupiterVersion = "5.7.0"

val mainVerticleName = "autoinvest-bot.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClassName = launcherClassName
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-config")
  implementation("io.vertx:vertx-web-client")
  implementation("io.vertx:vertx-web-graphql")
  implementation("io.vertx:vertx-rx-java2")
  implementation("io.vertx:vertx-pg-client")
  implementation("io.vertx:vertx-service-discovery")
  implementation("io.vertx:vertx-lang-kotlin")
  implementation("io.vertx:vertx-rx-java2")
  implementation("io.vertx:vertx-config")
  implementation("io.vertx:vertx-jdbc-client")
  implementation("com.graphql-java:graphql-java-extended-scalars:16.0.1")
  implementation("org.postgresql:postgresql:42.2.20.jre7")
  implementation(kotlin("stdlib-jdk8"))
  implementation("org.scala-lang:scala-library:2.13.5")
  implementation("io.vertx:vertx-lang-scala_2.13:4.0.3-SNAPSHOT")
  implementation("com.github.akarnokd:rxjava2-jdk8-interop:0.3.5")
  implementation("org.flywaydb:flyway-core:7.9.0")
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}
