package no.novari.fintkontrolluserservice.user

data class SimpleUser(
    val id: Long?,
    val fullName: String,
    val organisationUnitName: String?,
    val organisationUnitId: String?,
    val userType: String?,
    val userName: String?,
)

data class DetailedUser(
    val id: Long?,
    val fullName: String,
    val userName: String?,
    val organisationUnitName: String?,
    val email: String?,
    val userType: String?,
)

data class AccessRequest(
    val accessRequests: List<UrlMethodPair> = emptyList(),
)

data class UrlMethodPair(
    val url: String,
    val method: String,
)

data class AccessResponse(
    val url: String,
    val access: Boolean,
)

fun User.toSimpleUser() =
    SimpleUser(
        id = id,
        fullName = listOfNotNull(firstName, lastName).joinToString(" "),
        organisationUnitName = mainOrganisationUnitName,
        organisationUnitId = mainOrganisationUnitId,
        userType = userType,
        userName = userName,
    )

fun User.toDetailedUser() =
    DetailedUser(
        id = id,
        fullName = listOfNotNull(firstName, lastName).joinToString(" "),
        userName = userName,
        organisationUnitName = mainOrganisationUnitName,
        email = email,
        userType = userType,
    )
