package no.novari.fintkontrolluserservice.user

object UserStatus {
    const val ACTIVE = "ACTIVE"
    const val DISABLED = "DISABLED"
    const val INVALID = "INVALID"
    const val DELETED = "DELETED"

    val validStatuses = setOf(ACTIVE, DISABLED)
}
