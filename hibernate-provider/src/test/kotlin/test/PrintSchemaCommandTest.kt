package io.atlasgo.test

import com.github.ajalt.clikt.testing.test
import io.atlasgo.PrintSchemaCommand
import io.atlasgo.UnsupportedGenerationType
import models.example.Student
import io.atlasgo.toBase64
import models.otherpackage.OtherPackageEvent
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import models.gentyped.WithGenerationTypeAuto
import models.gentyped.WithGenerationTypeSequence
import models.gentyped.WithGenerationTypeTable
import models.gentyped.WithoutGenerationType
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.internal.util.PropertiesHelper
import org.hibernate.service.ServiceRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.Function
import kotlin.io.path.pathString
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull


@Entity
class Event {
    var title: String? = null

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: Long? = null
}

class TestExceptionType : RuntimeException("Test Exception")

class PrintSchemaCommandTest {
    private val command = PrintSchemaCommand().also {
        it.exitOnUnsupportedGenerationType = false
    }

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
                      val propertiesFileName: String = "hibernate.properties",
                      val sourcesListFile: String? = null)
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
        val sourcesFile = sourcesListFile ?: createSourcesListFile()
        args += listOf("--sources-list-file", sourcesFile)
        registryClassName?.let {
            args += listOf("--registry-builder", it)
        }
        builderClassName?.let {
            args += listOf("--metadata-builder", it)
        }
        assertEquals(expectedOutput + "\n", command.test(args).output)
    }

    @Test
    fun errorOnServiceRegistry() {
        class ThrowingRegistryBuilder : Function<Properties, ServiceRegistry> {
            override fun apply(t: Properties): ServiceRegistry {
                throw TestExceptionType()
            }
        }
        assertFailsWith<TestExceptionType>{
            command.test("--registry-builder ${ThrowingRegistryBuilder::class.java.name}")
        }
    }

    @Test
    fun errorOnMetadataBuilder() {
        class ThrowingMetadataBuilder : Function<ServiceRegistry, Metadata> {
            override fun apply(t: ServiceRegistry): Metadata {
                throw TestExceptionType()
            }
        }
        assertFailsWith<TestExceptionType>{
            command.test("--metadata-builder ${ThrowingMetadataBuilder::class.java.name}")
        }
    }

    @Test
    fun mutuallyExclusiveFlags() {
        var error = assertFailsWith<RuntimeException> {
            command.test("--properties hibernate.properties --metadata-builder something --classes org.example.model")
        }
        assertEquals("--metadata-builder is mutually exclusive with --packages and --classes", error.message)

        error = assertFailsWith<RuntimeException> {
            command.test("--properties hibernate.properties --metadata-builder something --packages ${OtherPackageEvent::class.java.encodedPackageName}")
        }
        assertEquals("--metadata-builder is mutually exclusive with --packages and --classes", error.message)
    }

    @Test
    fun missingConfigurationsFails() {
        val error = assertFailsWith<RuntimeException> {
            command.test("--properties empty-properties")
        }
        assertEquals("Unable to load properties file 'empty-properties', is it in the classpath?", error.message)
        assertFails {
            command.test("")
        }
    }

    @Test
    fun testDifferentGenerationTypes() {
        assertFailsWith<UnsupportedGenerationType> {
            command.test("--properties hibernate.properties --classes ${WithGenerationTypeSequence::class.java.name}")
        }.also {
            assertEquals("unsupported GenerationType.SEQUENCE found for entity 'models.gentyped.WithGenerationTypeSequence. See https://atlasgo.io/guides/orms/hibernate", it.message)
        }
        assertFailsWith<UnsupportedGenerationType> {
            command.test("--properties hibernate.properties --classes ${WithGenerationTypeTable::class.java.name}")
        }.also {
            assertEquals("unsupported GenerationType.TABLE found for entity 'models.gentyped.WithGenerationTypeTable. See https://atlasgo.io/guides/orms/hibernate", it.message)
        }
        assertFailsWith<UnsupportedGenerationType> {
            command.test("--properties hibernate.properties --classes ${WithGenerationTypeAuto::class.java.name}")
        }.also {
            assertEquals("unsupported GenerationType.AUTO found for entity 'models.gentyped.WithGenerationTypeAuto. See https://atlasgo.io/guides/orms/hibernate", it.message)
        }
        // Not throwing
        command.test("--properties hibernate.properties --classes ${WithoutGenerationType::class.java.name}")
        val result = command.test("--properties hibernate.properties --classes ${WithGenerationTypeSequence::class.java.name} --enable-table-generators")
        assert(result.output.startsWith("create sequence"))
    }

    companion object {
        @JvmStatic
        fun successCases() = listOf(
            Inputs(
                expectedOutput = "\n",
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos Event src/test/kotlin/test/PrintSchemaCommandTest.kt:40

                    create table Event (id bigint not null, title varchar(255), primary key (id));
                """.trimIndent(),
                encodedPackages = listOf(Event::class.java.encodedPackageName)
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos Event src/test/kotlin/test/PrintSchemaCommandTest.kt:40
                    -- atlas:pos OtherPackageEvent src/test/kotlin/models/otherpackage/OtherPackageEvent.kt:9

                    create table Event (id bigint not null, title varchar(255), primary key (id));
                    create table OtherPackageEvent (id bigint generated by default as identity, title varchar(255), primary key (id));
                """.trimIndent(),
                encodedPackages = listOf(Event::class.java.encodedPackageName, OtherPackageEvent::class.java.encodedPackageName)
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos Event src/test/kotlin/test/PrintSchemaCommandTest.kt:40

                    create table Event (id bigint not null, title varchar(255), primary key (id));
                """.trimIndent(),
                classes = listOf(Event::class.java.canonicalName)
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos Event src/test/kotlin/test/PrintSchemaCommandTest.kt:40
                    -- atlas:pos OtherPackageEvent src/test/kotlin/models/otherpackage/OtherPackageEvent.kt:9

                    create table Event (id bigint not null, title varchar(255), primary key (id));
                    create table OtherPackageEvent (id bigint generated by default as identity, title varchar(255), primary key (id));
                """.trimIndent(),
                classes = listOf(Event::class.java.canonicalName, OtherPackageEvent::class.java.canonicalName)
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos Event src/test/kotlin/test/PrintSchemaCommandTest.kt:40
                    -- atlas:pos OtherPackageEvent src/test/kotlin/models/otherpackage/OtherPackageEvent.kt:9

                    create table Event (id bigint not null, title varchar(255), primary key (id));
                    create table OtherPackageEvent (id bigint generated by default as identity, title varchar(255), primary key (id));
                """.trimIndent(),
                encodedPackages = listOf(OtherPackageEvent::class.java.encodedPackageName),
                classes = listOf(Event::class.java.canonicalName)
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos Event src/test/kotlin/test/PrintSchemaCommandTest.kt:40

                    create table Event (id bigint not null, title varchar(255), primary key (id)) engine=InnoDB;
                """.trimIndent(),
                registryClassName = WithMySQLRegistry::class.java.name,
                classes = listOf(Event::class.java.canonicalName)
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos OtherPackageEvent src/test/kotlin/models/otherpackage/OtherPackageEvent.kt:9

                    create table OtherPackageEvent (id bigint generated by default as identity, title varchar(255), primary key (id));
                """.trimIndent(),
                builderClassName = WithOtherPackageEventMetadata::class.java.name,
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos Event src/test/kotlin/test/PrintSchemaCommandTest.kt:40
                    
                    create table Event (id bigint not null, title varchar(255), primary key (id)) engine=InnoDB;
                """.trimIndent(),
                propertiesFileName = "mysql_hibernate.properties",
                classes = listOf(Event::class.java.canonicalName)
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos Person src/test/kotlin/models/example/VariousModels.kt:101
                    -- atlas:pos Student src/test/kotlin/models/example/VariousModels.kt:127

                    create table Person (id bigint generated by default as identity, city varchar(255), addresspostalcode varchar(255), street varchar(255), firstName varchar(255), lastName varchar(255), universityId varchar(255) not null unique, primary key (id));
                    create table Student (enrollmentDate date, id bigint not null, primary key (id));
                    alter table if exists Student add constraint FK3bkmp4kohbl54m14tb2fcqya3 foreign key (id) references Person;
                """.trimIndent(),
                classes = listOf(Student::class.java.name)
            ),
            Inputs(
                expectedOutput = """
                    -- atlas:pos academics[type=schema].Professor src/test/kotlin/models/example/VariousModels.kt:147
                    -- atlas:pos hrmanagement[type=schema].department src/test/kotlin/models/example/VariousModels.kt:250
                    -- atlas:pos hrmanagement[type=schema].employee_details src/test/kotlin/models/example/VariousModels.kt:289
                    -- atlas:pos Author src/test/kotlin/models/example/VariousModels.kt:269
                    -- atlas:pos authoremails src/test/kotlin/models/example/VariousModels.kt:269
                    -- atlas:pos Category src/test/kotlin/models/example/VariousModels.kt:328
                    -- atlas:pos CEO src/test/kotlin/models/example/VariousModels.kt:360
                    -- atlas:pos Company src/test/kotlin/models/example/VariousModels.kt:343
                    -- atlas:pos companyceo src/test/kotlin/models/example/VariousModels.kt:343
                    -- atlas:pos coursecatalog src/test/kotlin/models/example/VariousModels.kt:172
                    -- atlas:pos Customer src/test/kotlin/models/example/VariousModels.kt:393
                    -- atlas:pos customer_addresses src/test/kotlin/models/example/VariousModels.kt:393
                    -- atlas:pos CustomerAddress src/test/kotlin/models/example/VariousModels.kt:410
                    -- atlas:pos Employee src/test/kotlin/models/example/VariousModels.kt:289
                    -- atlas:pos employeepreferences src/test/kotlin/models/example/VariousModels.kt:289
                    -- atlas:pos Enrollment src/test/kotlin/models/example/VariousModels.kt:195
                    -- atlas:pos Order src/test/kotlin/models/example/VariousModels.kt:373
                    -- atlas:pos ordercustomer src/test/kotlin/models/example/VariousModels.kt:373
                    -- atlas:pos Person src/test/kotlin/models/example/VariousModels.kt:101
                    -- atlas:pos Product src/test/kotlin/models/example/VariousModels.kt:311
                    -- atlas:pos productcategories src/test/kotlin/models/example/VariousModels.kt:328
                    -- atlas:pos Student src/test/kotlin/models/example/VariousModels.kt:127
                    
                    create schema academics;
                    create schema hrmanagement;
                    create table academics.Professor (profilePicture blob, rank varchar(255) check (rank in ('ASSISTANT_PROFESSOR','ASSOCIATE_PROFESSOR','FULL_PROFESSOR','EMERITUS')), id bigint not null, primary key (id));
                    create table hrmanagement.department (id bigint generated by default as identity, budget numeric(38,2), deptname varchar(255), managerid bigint, primary key (id));
                    create table hrmanagement.employee_details (biography varchar(255), id bigint not null, primary key (id));
                    create table Author (id bigint generated by default as identity, name varchar(255), primary key (id));
                    create table authoremails (Author_id bigint not null, emails varchar(255));
                    create table Category (id bigint generated by default as identity, name varchar(255), primary key (id));
                    create table CEO (id bigint generated by default as identity, firstName varchar(255), lastName varchar(255), primary key (id));
                    create table Company (id bigint generated by default as identity, name varchar(255), primary key (id));
                    create table companyceo (ceoid bigint unique, companyid bigint not null, primary key (companyid));
                    create table coursecatalog (id bigint generated by default as identity, title varchar(255), tuitionFee varchar(50), instructorid bigint, primary key (id));
                    create table Customer (id bigint generated by default as identity, name varchar(255), primary key (id));
                    create table customer_addresses (customerid bigint not null, addressid bigint not null unique, primary key (customerid, addressid));
                    create table CustomerAddress (id bigint generated by default as identity, city varchar(255), street varchar(255), zipCode varchar(255), primary key (id));
                    create table Employee (id bigint generated by default as identity, firstName varchar(255), lastName varchar(255), primary key (id));
                    create table employeepreferences (notificationEnabled boolean, preferredLanguage varchar(255), id bigint not null, primary key (id));
                    create table Enrollment (enrollmentDate timestamp(6) with time zone, grade varchar(255), courseid bigint not null, studentid bigint not null, primary key (courseid, studentid));
                    create table Order (id bigint generated by default as identity, orderNumber varchar(255), primary key (id));
                    create table ordercustomer (customerid bigint, orderid bigint not null, primary key (orderid));
                    create table Person (id bigint generated by default as identity, city varchar(255), addresspostalcode varchar(255), street varchar(255), firstName varchar(255), lastName varchar(255), universityId varchar(255) not null unique, primary key (id));
                    create table Product (id bigint generated by default as identity, name varchar(255), primary key (id));
                    create table productcategories (productid bigint not null, categoryid bigint not null, primary key (productid, categoryid));
                    create table Student (enrollmentDate date, id bigint not null, primary key (id));
                    alter table if exists academics.Professor add constraint FK25kftnj3k6j8ebw8imjbqsolo foreign key (id) references Person;
                    alter table if exists hrmanagement.department add constraint FKblqgjxeyk3ix0r68mw3hiu6td foreign key (managerid) references academics.Professor;
                    alter table if exists hrmanagement.employee_details add constraint FKbvwf9n44twx23qx6ci9a72x2b foreign key (id) references Employee;
                    alter table if exists authoremails add constraint FK9aq51l74tp6qdabsg0e84hh7b foreign key (Author_id) references Author;
                    alter table if exists companyceo add constraint FKra5cwemf6r9slvd0rec59ebix foreign key (ceoid) references CEO;
                    alter table if exists companyceo add constraint FKn6wtw7cfv78lt7eqhrb55dif6 foreign key (companyid) references Company;
                    alter table if exists coursecatalog add constraint FKl08oe4240lhp5i5qun626s1rb foreign key (instructorid) references academics.Professor;
                    alter table if exists customer_addresses add constraint FKm5yfs64j4q1aefem5dxv9w702 foreign key (addressid) references CustomerAddress;
                    alter table if exists customer_addresses add constraint FKcu5vrfiohivj5928eajv1nd8b foreign key (customerid) references Customer;
                    alter table if exists employeepreferences add constraint FKao5vdt9267mq8ka18k4sah8av foreign key (id) references Employee;
                    alter table if exists Enrollment add constraint FKrsyteadepy9pnputrpg628hge foreign key (courseid) references coursecatalog;
                    alter table if exists Enrollment add constraint FKp7tirc7n07m88div0ooj5ygig foreign key (studentid) references Student;
                    alter table if exists ordercustomer add constraint FK7my3p5yqkmohdef4nhlnsw78n foreign key (customerid) references Customer;
                    alter table if exists ordercustomer add constraint FKq34ggisc946yeff3kqdf3f21q foreign key (orderid) references Order;
                    alter table if exists productcategories add constraint FKdt44n5wp1rilckdfhbiwfwgfy foreign key (categoryid) references Category;
                    alter table if exists productcategories add constraint FKntp44h25shvsr213632jjwwre foreign key (productid) references Product;
                    alter table if exists Student add constraint FK3bkmp4kohbl54m14tb2fcqya3 foreign key (id) references Person;
                """.trimIndent(),
                encodedPackages = listOf(Student::class.java.encodedPackageName)
            ),
        )
    }
}

private val <T> Class<T>.encodedPackageName: String
    get() {
        val packageName = getResource("")
        assertNotNull(packageName)
        return packageName.toURI().toString().toBase64()
    }

private fun createSourcesListFile(): String {
    val testSourcesDir = Paths.get("src/test/kotlin")
    val testResourcesDir = Paths.get("src/test/resources")

    val allSourceFiles = mutableListOf<String>()

    // Collect all .kt and .java files from src/test/kotlin
    if (Files.exists(testSourcesDir)) {
        Files.walk(testSourcesDir)
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".kt") || it.toString().endsWith(".java") }
            .forEach { allSourceFiles.add(it.toString()) }
    }

    // Collect all files from src/test/resources
    if (Files.exists(testResourcesDir)) {
        Files.walk(testResourcesDir)
            .filter { Files.isRegularFile(it) }
            .forEach { allSourceFiles.add(it.toString()) }
    }

    // Create temporary file
    val tempFile = Files.createTempFile("sources-list-", ".txt")
    Files.write(tempFile, allSourceFiles.joinToString("\n").toByteArray())

    // Return the absolute path as string
    return tempFile.toAbsolutePath().toString()
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