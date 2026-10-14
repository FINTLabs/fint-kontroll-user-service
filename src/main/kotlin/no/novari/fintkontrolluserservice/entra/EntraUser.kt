package no.novari.fintkontrolluserservice.entra

data class EntraUser(
    val id: String? = null,
    val mail: String? = null,
    val userPrincipalName: String? = null,
    val employeeId: String? = null,
    val studentId: String? = null,
    val accountEnabled: Boolean? = null,
    val isDeleted: Boolean? = null,
)
