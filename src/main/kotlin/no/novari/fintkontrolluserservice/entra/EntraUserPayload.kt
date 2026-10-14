package no.novari.fintkontrolluserservice.entra

data class EntraUserPayload(
    val mail: String? = null,
    val userPrincipalName: String? = null,
    val employeeId: String? = null,
    val studentId: String? = null,
    val accountEnabled: Boolean? = null,
)
