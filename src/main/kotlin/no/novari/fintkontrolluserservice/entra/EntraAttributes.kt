package no.novari.fintkontrolluserservice.entra

import java.util.UUID

data class EntraAttributes(
    val email: String?,
    val userName: String?,
    val objectId: UUID?,
    val status: String,
)
