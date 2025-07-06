package models.gentyped

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
open class WithGenerationTypeSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long? = null
}

@Entity
open class WithGenerationTypeTable {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    var id: Long? = null
}

@Entity
open class WithGenerationTypeAuto {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null
}

@Entity
open class WithGenerationTypeIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

@Entity
open class WithoutGenerationType {
    @Id
    var id: Long? = null
}