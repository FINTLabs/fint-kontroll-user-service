package no.novari.fintkontrolluserservice.user

import no.novari.cache.FintCache
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.novari.fint.model.resource.felles.PersonResource
import no.novari.fint.model.resource.utdanning.elev.ElevResource
import no.novari.fint.model.resource.utdanning.elev.ElevforholdResource
import no.novari.fint.model.resource.utdanning.utdanningsprogram.SkoleResource
import no.novari.fintkontrolluserservice.FintLinkUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class StudentUserFactory(
    private val elevResourceCache: FintCache<String, ElevResource>,
    private val personResourceCache: FintCache<String, PersonResource>,
    private val elevforholdCache: FintCache<String, ElevforholdResource>,
    private val skoleResourceCache: FintCache<String, SkoleResource>,
    private val organisasjonselementResourceCache: FintCache<String, OrganisasjonselementResource>,
    private val entraUserService: EntraUserService,
    private val validityPeriodService: ValidityPeriodService,
    @Value("\${fint.kontroll.user.days-before-start-student:0}") private val daysBeforeStart: Int,
) {
    fun all(now: Date): List<UserCandidate> =
        elevResourceCache.allDistinct
            .filter { it.elevforhold?.isNotEmpty() == true }
            .mapNotNull { create(it, now) }

    fun create(
        student: ElevResource,
        now: Date,
    ): UserCandidate? {
        val resourceId = FintLinkUtils.resourceId(student)
        val person =
            student.person
                .firstOrNull()
                ?.href
                ?.let(FintLinkUtils::getSystemIdFromMessageKey)
                ?.let { personResourceCache.getOptional(it).orElse(null) }
                ?: return null
        val entra = entraUserService.get(resourceId) ?: return null

        val relationships =
            student.elevforhold
                .orEmpty()
                .mapNotNull { it.href }
                .map(FintLinkUtils::getSystemIdFromMessageKey)
                .mapNotNull { elevforholdCache.getOptional(it).orElse(null) }
                .filter { it.gyldighetsperiode != null }
        val mainRelationship =
            relationships.firstOrNull {
                validityPeriodService.isValid(it.gyldighetsperiode, now, daysBeforeStart)
            } ?: relationships.firstOrNull() ?: return null
        val school =
            mainRelationship.skole
                ?.firstOrNull()
                ?.href
                ?.let(FintLinkUtils::getSystemIdFromMessageKey)
                ?.let { skoleResourceCache.getOptional(it).orElse(null) }
                ?: return null
        val organisation =
            school.organisasjon
                ?.firstOrNull()
                ?.href
                ?.let(FintLinkUtils::getSystemIdFromMessageKey)
                ?.let { organisasjonselementResourceCache.getOptional(it).orElse(null) }
                ?: return null

        return UserCandidate(
            resourceId = resourceId,
            firstName = person.navn?.fornavn,
            lastName = person.navn?.etternavn,
            userType = UserType.STUDENT,
            userName = entra.userName,
            identityProviderUserObjectId = entra.objectId,
            mainOrganisationUnitName = organisation.navn,
            mainOrganisationUnitId = organisation.organisasjonsId?.identifikatorverdi,
            email = entra.email,
            fintStatus =
                if (validityPeriodService.isValid(mainRelationship.gyldighetsperiode, now, daysBeforeStart)) {
                    UserStatus.ACTIVE
                } else {
                    UserStatus.DISABLED
                },
            validFrom = relationships.mapNotNull { it.gyldighetsperiode?.start }.minOrNull(),
            validTo = relationships.mapNotNull { it.gyldighetsperiode?.slutt }.maxOrNull(),
            entraStatus = entra.status,
        )
    }
}
