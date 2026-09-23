package no.novari.fintkontrolluserservice.entra

data class EntraUser(
    val id: String,
    val mail: String,
    val userPrincipalName: String,
    val employeeId: String? = null,
    val studentId: String? = null,
    val accountEnabled: Boolean,
    val isDeleted: Boolean,
)
