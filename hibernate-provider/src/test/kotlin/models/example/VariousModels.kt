package models.example

import jakarta.persistence.AttributeConverter
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Basic
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.Lob
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SecondaryTable
import jakarta.persistence.SecondaryTables
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.NaturalId
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Currency

/**
 * An enum to demonstrate mapping.
 * FEATURE: @Enumerated(EnumType.STRING) - Will be stored as a VARCHAR in the DB.
 */
enum class ProfessorRank {
    ASSISTANT_PROFESSOR,
    ASSOCIATE_PROFESSOR,
    FULL_PROFESSOR,
    EMERITUS
}

/**
 * A custom value type, represented as a Kotlin data class.
 * FEATURE: Custom AttributeConverter will handle serialization to/from a String.
 */
data class MonetaryAmount(val amount: BigDecimal, val currency: Currency)

/**
 * The JPA converter for our custom MonetaryAmount type.
 * FEATURE: @Converter - Maps a complex Java type to a single database column.
 */
@Converter(autoApply = false) // We will apply it manually with @Convert
class MonetaryAmountConverter : AttributeConverter<MonetaryAmount, String> {
    override fun convertToDatabaseColumn(attribute: MonetaryAmount?): String? {
        return attribute?.let { "${it.amount} ${it.currency.currencyCode}" }
    }

    override fun convertToEntityAttribute(dbData: String?): MonetaryAmount? {
        return dbData?.split(" ")?.let {
            MonetaryAmount(BigDecimal(it[0]), Currency.getInstance(it[1]))
        }
    }
}

/**
 * An embeddable address component.
 * FEATURE: @Embeddable - This object's fields will be stored as columns
 * in the table of the owning entity.
 */
@Embeddable
data class Address(
    var street: String,
    var city: String,
    @Column(name = "zipcode", length = 10)
    var postalCode: String
)

/**
 * A base class for people in the system.
 * FEATURE: @Inheritance(strategy = InheritanceType.JOINED)
 * This will create a primary 'person' table and separate 'student' and 'professor'
 * tables, each with a foreign key back to the 'person' table.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
abstract class Person(
    @Id
    // FIX 2: Changed ID generation strategy to IDENTITY to avoid sequences.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,

    // FIX 1: Moved the NaturalId to the root of the hierarchy where it belongs.
    @NaturalId
    @Column(nullable = false, unique = true)
    open var universityId: String,

    open var firstName: String,
    open var lastName: String,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "postalCode", column = Column(name = "addresspostalcode"))
    )
    open var address: Address
)

/**
 * A Student entity.
 * FEATURE: Uses default table name (will be 'student' due to class name).
 */
@Entity
class Student(
    universityId: String,

    var enrollmentDate: LocalDate,

    // Properties inherited from Person
    id: Long? = null,
    firstName: String,
    lastName: String,
    address: Address

) : Person(id, universityId, firstName, lastName, address)

/**
 * A Professor entity.
 * FEATURE: @Table with qualified name (schema).
 * Demonstrates table mapping with schema specification.
 */
@Entity
@Table(name = "Professor", schema = "academics")
class Professor(
    @Enumerated(EnumType.STRING)
    var rank: ProfessorRank,

    // FEATURE: @Lob - For mapping large objects. This will be a BLOB/bytea in the DB.
    @Lob
    @Basic(fetch = FetchType.LAZY)
    var profilePicture: ByteArray,

    // Properties inherited from Person
    id: Long? = null,
    universityId: String,
    firstName: String,
    lastName: String,
    address: Address

) : Person(id, universityId, firstName, lastName, address)


/**
 * A Course entity.
 * FEATURE: @Table with custom name via annotation.
 */
@Entity
@Table(name = "coursecatalog")
class Course(
    @Id
    // FIX 2: Changed ID generation strategy to IDENTITY, removing the need for @SequenceGenerator.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var title: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructorid")
    var instructor: Professor,

    @Convert(converter = MonetaryAmountConverter::class)
    @Column(length = 50)
    var tuitionFee: MonetaryAmount
)

/**
 * An entity representing an enrollment, using a composite primary key.
 * This is the join table for a Many-to-Many relationship with an extra payload (grade).
 * FEATURE: @EmbeddedId for a composite key.
 */
@Entity
class Enrollment(
    @EmbeddedId
    var id: EnrollmentId,

    @ManyToOne @MapsId("studentId") @JoinColumn(name = "studentid")
    var student: Student,

    @ManyToOne @MapsId("courseId") @JoinColumn(name = "courseid")
    var course: Course,

    var enrollmentDate: Instant = Instant.now(),
    var grade: String? = null
)

/**
 * The composite key class for Enrollment.
 * FEATURE: @Embeddable - Must be embeddable and serializable.
 */
@Embeddable
data class EnrollmentId(
    var studentId: Long,
    var courseId: Long
) : Serializable


/**
 * A read-only entity mapped to a database VIEW.
 * FEATURE: @Subselect - Maps an entity to a SQL query instead of a table.
 * FEATURE: @Immutable - Marks the entity as read-only. Hibernate won't generate UPDATEs.
 */
@Entity
@Immutable
@Subselect("""
    SELECT 
        p.id as id,
        p.firstname || ' ' || p.lastname as fullname,
        (SELECT count(*) FROM coursecatalog c WHERE c.instructorid = p.id) as coursecount
    FROM academics.Professor prof
    JOIN person p on prof.id = p.id
""")
// FEATURE: Sync with a physical table for write operations (optional, but good practice).
@Synchronize(value = ["academics.Professor", "coursecatalog"])
data class ProfessorCourseCount(
    @Id
    val id: Long,
    val fullName: String,
    val courseCount: Long
)

/**
 * An entity demonstrating table with schema.
 * FEATURE: @Table with schema - demonstrates schema usage.
 */
@Entity
@Table(name = "department", schema = "HRManagement")
class Department(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "deptname")
    var departmentName: String,

    var budget: BigDecimal,

    @ManyToOne
    @JoinColumn(name = "managerid")
    var manager: Professor? = null
)

/**
 * @ElementCollection - Creates implicit collection table
 */
@Entity
class Author(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String,

    @ElementCollection
    @CollectionTable(name = "authoremails")
    var emails: MutableSet<String> = mutableSetOf()
)

/**
 * @SecondaryTable - Creates implicit secondary tables, one with schema and one without
 */
@Entity
@SecondaryTables(
    SecondaryTable(name = "employee_details", schema = "hrmanagement"),
    SecondaryTable(name = "employeepreferences")
)
class Employee(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var firstName: String,
    var lastName: String,

    @Column(table = "employee_details")
    var biography: String? = null,

    @Column(table = "employeepreferences")
    var preferredLanguage: String? = null,

    @Column(table = "employeepreferences")
    var notificationEnabled: Boolean = true
)

/**
 * @ManyToMany with @JoinTable - Creates implicit join table
 */
@Entity
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String,

    @ManyToMany
    @JoinTable(
        name = "productcategories",
        joinColumns = [JoinColumn(name = "productid")],
        inverseJoinColumns = [JoinColumn(name = "categoryid")]
    )
    var categories: MutableSet<Category> = mutableSetOf()
)

@Entity
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String,

    @ManyToMany(mappedBy = "categories")
    var products: MutableSet<Product> = mutableSetOf()
)

/**
 * @OneToOne with @JoinTable - Creates implicit join table for one-to-one
 */
@Entity
class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "companyceo",
        joinColumns = [JoinColumn(name = "companyid")],
        inverseJoinColumns = [JoinColumn(name = "ceoid")]
    )
    var ceo: CEO? = null
)

@Entity
class CEO(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var firstName: String,
    var lastName: String
)

/**
 * @ManyToOne with @JoinTable - Creates implicit join table
 */
@Entity
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var orderNumber: String,

    @ManyToOne
    @JoinTable(
        name = "ordercustomer",
        joinColumns = [JoinColumn(name = "orderid")],
        inverseJoinColumns = [JoinColumn(name = "customerid")]
    )
    var customer: Customer? = null
)

/**
 * @OneToMany with @JoinTable - Creates implicit join table
 */
@Entity
class Customer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String,

    @OneToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "customer_addresses",
        joinColumns = [JoinColumn(name = "customerid")],
        inverseJoinColumns = [JoinColumn(name = "addressid")]
    )
    var addresses: MutableSet<CustomerAddress> = mutableSetOf()
)

@Entity
class CustomerAddress(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var street: String,
    var city: String,
    var zipCode: String
)