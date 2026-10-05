package no.novari.fintkontrolluserservice.user

import no.novari.fintkontrolluserservice.entra.EntraUserExternal
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ExternalUserService(
    private val userService: UserService,
) {
    fun reconcile(
        resourceId: String,
        externalUser: EntraUserExternal?,
    ) {
        if (externalUser == null) {
            userService.markDeleted(resourceId)
            return
        }

        val suffix =
            externalUser.email
                ?.substringAfter('@', missingDelimiterValue = "")
                ?.takeIf(String::isNotBlank)
                ?.let { " (ekstern $it)" }
                ?: " (ekstern)"
        val objectId = externalUser.userObjectId ?: resourceId

        userService.saveAndPublish(
            UserCandidate(
                resourceId = resourceId,
                firstName = externalUser.firstName,
                lastName = externalUser.lastName?.plus(suffix),
                userType = UserType.EXTERNAL,
                userName = externalUser.userName ?: externalUser.userPrincipalName,
                identityProviderUserObjectId = runCatching { UUID.fromString(objectId) }.getOrNull(),
                mainOrganisationUnitName = externalUser.mainOrganisationUnitName,
                mainOrganisationUnitId = externalUser.mainOrganisationUnitId,
                email = externalUser.email,
                fintStatus = UserStatus.ACTIVE,
                validFrom = null,
                validTo = null,
                entraStatus = if (externalUser.accountEnabled == true) UserStatus.ACTIVE else UserStatus.DISABLED,
            ),
        )
    }
}
