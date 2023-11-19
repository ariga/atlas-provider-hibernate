package io.atlasgo

import com.github.ajalt.clikt.testing.test
import io.other.other_package.OtherPackageEvent
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.internal.util.PropertiesHelper
import org.hibernate.service.ServiceRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.Properties
import java.util.function.Function
import kotlin.test.assertNotNull


@Entity
class Event {
    var title: String? = null

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: Long? = null
}

class PrintSchemaCommandTest {
    private val command = PrintSchemaCommand()

    @BeforeEach
    fun initCommand() {
        val echoStream = object : ByteArrayOutputStream() {
            override fun flush() {
                toString().takeIf { it.isNotEmpty() }?.let {
                    command.echo(it, trailingNewline = false)
                }
                this.buf = ByteArray(buf.size)
                this.count = 0
            }
        }
        System.setOut(PrintStream(echoStream, true))
    }

    data class Inputs(val expectedOutput: String,
                      val encodedPackages: List<String> = emptyList(),
                      val classes: List<String> = emptyList(),
                      val builderClassName: String? = null,
                      val registryClassName: String? = null,
                      val propertiesFileName: String = "hibernate.properties")

    @ParameterizedTest
    @MethodSource("successCases")
    fun testSuccessCases(input: Inputs) = input.run {
        val args = mutableListOf("--properties", propertiesFileName)
        if (encodedPackages.isNotEmpty()) {
            args += listOf("--packages", encodedPackages.joinToString(","))
        }
        if (classes.isNotEmpty()) {
            args += listOf("--classes", classes.joinToString(","))
        }
        registryClassName?.let {
            args += listOf("--registry-builder", it)
        }
        builderClassName?.let {
            args += listOf("--metadata-builder", it)
        }
        assertEquals(expectedOutput, command.test(args).output)
    }

    companion object {
        @JvmStatic
        fun successCases() = listOf(
            Inputs(
                expectedOutput = "",
            ),
            Inputs(
                expectedOutput = "create table Event (id bigint not null, title varchar(255), primary key (id));\n",
                encodedPackages = listOf(Event::class.java.encodedPackageName)
            ),
            Inputs(
                expectedOutput =
                    "create sequence OtherPackageEvent_SEQ start with 1 increment by 50;\n" +
                    "create table Event (id bigint not null, title varchar(255), primary key (id));\n" +
                    "create table OtherPackageEvent (id bigint not null, title varchar(255), primary key (id));\n",
                encodedPackages = listOf(Event::class.java.encodedPackageName, OtherPackageEvent::class.java.encodedPackageName)
            ),
            Inputs(
                expectedOutput = "create table Event (id bigint not null, title varchar(255), primary key (id));\n",
                classes = listOf(Event::class.java.canonicalName)
            ),
            Inputs(
                expectedOutput =
                    "create sequence OtherPackageEvent_SEQ start with 1 increment by 50;\n" +
                    "create table Event (id bigint not null, title varchar(255), primary key (id));\n" +
                    "create table OtherPackageEvent (id bigint not null, title varchar(255), primary key (id));\n",
                classes = listOf(Event::class.java.canonicalName, OtherPackageEvent::class.java.canonicalName)
            ),
            Inputs(
                expectedOutput =
                    "create sequence OtherPackageEvent_SEQ start with 1 increment by 50;\n" +
                    "create table Event (id bigint not null, title varchar(255), primary key (id));\n" +
                    "create table OtherPackageEvent (id bigint not null, title varchar(255), primary key (id));\n",
                encodedPackages = listOf(OtherPackageEvent::class.java.encodedPackageName),
                classes = listOf(Event::class.java.canonicalName)
            ),
            Inputs(
                expectedOutput = "create table Event (id bigint not null, title varchar(255), primary key (id)) engine=InnoDB;\n",
                registryClassName = WithMySQLRegistry::class.java.name,
                classes = listOf(Event::class.java.canonicalName)
            ),
            Inputs(
                expectedOutput =
                    "create sequence OtherPackageEvent_SEQ start with 1 increment by 50;\n" +
                    "create table OtherPackageEvent (id bigint not null, title varchar(255), primary key (id));\n",
                builderClassName = WithOtherPackageEventMetadata::class.java.name,
            ),
            Inputs(
                expectedOutput = "create table Event (id bigint not null, title varchar(255), primary key (id)) engine=InnoDB;\n",
                propertiesFileName = "mysql_hibernate.properties",
                classes = listOf(Event::class.java.canonicalName)
            )
        )
    }
}

private val <T> Class<T>.encodedPackageName: String
    get() {
        val packageName = getResource("")
        assertNotNull(packageName)
        return packageName.toURI().toString().toBase64()
    }

class WithMySQLRegistry : Function<Properties, ServiceRegistry> {
    override fun apply(t: Properties) = StandardServiceRegistryBuilder()
        .applySettings(PropertiesHelper.map(t) + mapOf(
            "hibernate.temp.use_jdbc_metadata_defaults" to false,
            "jakarta.persistence.database-product-name" to "MySQL",
            "jakarta.persistence.database-major-version" to 8
        ))
        .build()
}

class WithOtherPackageEventMetadata : Function<ServiceRegistry, Metadata> {
    override fun apply(t: ServiceRegistry): Metadata {
        return MetadataSources(t)
            .addAnnotatedClasses(OtherPackageEvent::class.java)
            .buildMetadata()
    }

}