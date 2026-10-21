package no.novari.fintkontrolluserservice.entra

data class EntraUserExternal(
    val firstName: String? = null,
    val lastName: String? = null,
    val mobilePhone: String? = null,
    val email: String? = null,
    val userName: String? = null,
    val userObjectId: String? = null,
    val userPrincipalName: String? = null,
    val accountEnabled: Boolean? = null,
    var mainOrganisationUnitName: String? = null,
    var mainOrganisationUnitId: String? = null,
)
