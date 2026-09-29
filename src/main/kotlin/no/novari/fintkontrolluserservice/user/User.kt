package no.novari.fintkontrolluserservice.user

import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "resourceid", unique = true, nullable = false)
    var resourceId: String = "",
    @Column(name = "firstname")
    var firstName: String? = null,
    @Column(name = "lastname")
    var lastName: String? = null,
    @Column(name = "usertype")
    var userType: String? = null,
    @Column(name = "username")
    var userName: String? = null,
    @Column(name = "identityprovideruserobjectid")
    var identityProviderUserObjectId: UUID? = null,
    @Column(name = "mainorganisationunitname")
    var mainOrganisationUnitName: String? = null,
    @Column(name = "mainorganisationunitid")
    var mainOrganisationUnitId: String? = null,
    @ElementCollection
    var organisationUnitIds: MutableSet<String> = mutableSetOf(),
    @Column(name = "email")
    var email: String? = null,
    @Column(name = "managerref")
    var managerRef: String? = null,
    @Column(name = "status", nullable = false)
    var status: String = UserStatus.DISABLED,
    @Column(name = "statuschanged")
    var statusChanged: Date? = null,
    @Column(name = "validfrom")
    var validFrom: Date? = null,
    @Column(name = "validto")
    var validTo: Date? = null,
    @CreationTimestamp
    @Column(name = "createddate", updatable = false)
    var createdDate: Instant? = null,
    @UpdateTimestamp
    @Column(name = "modifieddate")
    var modifiedDate: Instant? = null,
)
