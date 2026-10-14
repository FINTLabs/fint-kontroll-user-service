package no.novari.fintkontrolluserservice.user

import no.novari.cache.FintCache
import no.novari.fintkontrolluserservice.entra.EntraAttributes
import no.novari.fintkontrolluserservice.entra.EntraUser
import no.novari.fintkontrolluserservice.entra.EntraUserPayload
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EntraUserService(
    private val graphUserCache: FintCache<String, EntraUser>,
) {
    fun getEntraAttributes(resourceId: String): EntraAttributes? = graphUserCache.getOptional(resourceId).orElse(null)?.toAttributes()

    fun status(user: EntraUser): String =
        when {
            user.isDeleted == true -> UserStatus.DELETED
            user.accountEnabled == true -> UserStatus.ACTIVE
            else -> UserStatus.DISABLED
        }

    private fun EntraUser.toAttributes() =
        EntraAttributes(
            email = mail,
            userName = userPrincipalName,
            objectId = runCatching { UUID.fromString(id) }.getOrNull(),
            status = status(this),
        )

    fun fromPayload(
        id: String,
        payload: EntraUserPayload,
    ): EntraUser =
        EntraUser(
            id = id,
            mail = payload.mail,
            userPrincipalName = payload.userPrincipalName,
            employeeId = payload.employeeId,
            studentId = payload.studentId,
            accountEnabled = payload.accountEnabled,
            isDeleted = false,
        )

    fun getEmployeeOrStudentId(entraUser: EntraUser) = entraUser.employeeId ?: entraUser.studentId
}
