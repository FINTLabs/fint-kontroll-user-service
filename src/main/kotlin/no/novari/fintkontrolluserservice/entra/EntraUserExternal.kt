package no.novari.fintkontrolluserservice.entra

data class EntraUserExternal(
    val firstName: String,
    val lastName: String,
    val mobilePhone: String? = null,
    val email: String? = null,
    val userName: String,
    val userObjectId: String,
    val userPrincipalName: String,
    val accountEnabled: Boolean,
    var mainOrganisationUnitName: String? = null,
    var mainOrganisationUnitId: String? = null,
)
