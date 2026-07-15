import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.spotbugs.snom.SpotBugsTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    checkstyle
    id("com.github.spotbugs") version "6.5.9"
    id("com.gradleup.shadow") version "9.5.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    java
}

group = "com.feloheger.shardzone"

fun getTime(): String {
    val sdf = SimpleDateFormat("yyMMdd-HHmm")
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

version = (if (!hasProperty("ver")) {
    "${getTime()}-SNAPSHOT"
} else {
    val ver = property("ver") as String
    val base = if (ver.startsWith("v")) ver.drop(1) else ver.replace('/', '-')
    if (ver.startsWith("v") && !ver.lowercase().contains("-rc-")) base else "$base-SNAPSHOT"
}).uppercase()

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

repositories {
    mavenLocal()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net")
    maven("https://repo.fancyinnovations.com/releases")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/") {
        name = "opencollab"
    }

    mavenCentral()
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")

    compileOnly("com.github.MilkBowl:VaultAPI:master-SNAPSHOT")
    compileOnly("de.oliver:FancyNpcs:2.9.2")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")

    compileOnly("com.github.spotbugs:spotbugs-annotations:4.10.2")
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")
    testCompileOnly("com.github.spotbugs:spotbugs-annotations:4.10.2")

    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.1")

    implementation("com.github.CrimsonWarpedcraft:cw-commons:v0.3.0") {
        exclude(group = "org.mockbukkit.mockbukkit")
    }
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.22.1")
    implementation("dev.jorel:commandapi-paper-shade:11.2.0") {
        exclude(group = "org.mockbukkit.mockbukkit")
    }
    implementation("org.hibernate.validator:hibernate-validator:9.1.2.Final")

    testImplementation("org.mockito:mockito-core:5.23.0")
    mockitoAgent("org.mockito:mockito-core:5.23.0") { isTransitive = false }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}

tasks.processResources {
    filesMatching("**/plugin.yml") {
        expand(mapOf("NAME" to rootProject.name, "VERSION" to version, "PACKAGE" to project.group))
    }
}

spotbugs {
    ignoreFailures = true
}

checkstyle {
    toolVersion = "13.6.0"
    maxWarnings = 1000
}

configurations.named("checkstyle") {
    resolutionStrategy.capabilitiesResolution
        .withCapability("com.google.collections:google-collections") {
            select("com.google.guava:guava:23.0")
        }
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

tasks.withType<SpotBugsTask>().configureEach {
    reports.create("html") {
        required.set(true)
    }
    reports.create("xml") {
        required.set(false)
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    mergeServiceFiles()
    dependencies {
        exclude(dependency("dev.jorel:commandapi-spigot-test-toolkit:.*"))
    }
    relocate("dev.jorel.commandapi", "${project.group}.commandapi")
    relocate("com.fasterxml", "${project.group}.fasterxml")
    relocate("org.yaml.snakeyaml", "${project.group}.snakeyaml")
    relocate("org.hibernate.validator", "${project.group}.hibernatevalidator")
    relocate("jakarta.validation", "${project.group}.jakartavalidation")
    relocate("org.jboss.logging", "${project.group}.jbosslogging")


    minimize {
        exclude(dependency("dev.jorel:commandapi-paper-shade:.*"))
        exclude(dependency("dev.jorel:commandapi-spigot-test-toolkit:.*"))
        exclude(dependency("com.fasterxml.jackson.core:.*:.*"))
        exclude(dependency("com.fasterxml.jackson.dataformat:.*:.*"))
        exclude(dependency("com.fasterxml:classmate:.*"))
        exclude(dependency("org.hibernate.validator:.*:.*"))
        exclude(dependency("jakarta.validation:.*:.*"))
        exclude(dependency("org.yaml:snakeyaml:.*"))
        exclude(dependency("org.jboss.logging:.*:.*"))
        exclude(dependency("com.github.CrimsonWarpedcraft:cw-commons:.*"))

    }
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(shadowJar)
}

tasks.register("printProjectName") {
    doLast {
        println(rootProject.name)
    }
}

tasks.register("release") {
    dependsOn("build")
    doLast {
        if (!version.toString().endsWith("-SNAPSHOT")) {
            val releaseJar = layout.buildDirectory.file("libs/${rootProject.name}.jar").get().asFile
            shadowJar.get().archiveFile.get().asFile.renameTo(releaseJar)
        }
    }
}