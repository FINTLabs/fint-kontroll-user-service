package no.novari.fintkontrolluserservice.user

import java.util.Date
import java.util.UUID

/** The factory result before the FINT and Entra statuses are combined into a catalog status. */
data class UserCandidate(
    val resourceId: String,
    val firstName: String?,
    val lastName: String?,
    val userType: UserType,
    val userName: String?,
    val identityProviderUserObjectId: UUID?,
    val mainOrganisationUnitName: String?,
    val mainOrganisationUnitId: String?,
    val organisationUnitIds: Set<String> = emptySet(),
    val email: String?,
    val managerRef: String? = null,
    val fintStatus: String,
    val validFrom: Date?,
    val validTo: Date?,
    val entraStatus: String,
)
