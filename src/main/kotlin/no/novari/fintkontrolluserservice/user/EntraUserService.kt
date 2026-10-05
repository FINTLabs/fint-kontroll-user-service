package no.novari.fintkontrolluserservice.user

import no.novari.cache.FintCache
import no.novari.fintkontrolluserservice.entra.EntraAttributes
import no.novari.fintkontrolluserservice.entra.EntraUser
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EntraUserService(
    private val graphUserCache: FintCache<String, EntraUser>,
) {
    fun getEntraAttributes(resourceId: String): EntraAttributes? = graphUserCache.getOptional(resourceId).orElse(null)?.toAttributes()

    fun status(user: EntraUser): String =
        when {
            user.isDeleted -> UserStatus.DELETED
            user.accountEnabled -> UserStatus.ACTIVE
            else -> UserStatus.DISABLED
        }

    private fun EntraUser.toAttributes() =
        EntraAttributes(
            email = mail,
            userName = userPrincipalName,
            objectId = runCatching { UUID.fromString(id) }.getOrNull(),
            status = status(this),
        )
}
