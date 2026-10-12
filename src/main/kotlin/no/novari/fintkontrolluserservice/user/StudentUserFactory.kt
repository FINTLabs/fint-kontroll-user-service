package no.novari.fintkontrolluserservice.user

import no.novari.cache.FintCache
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.novari.fint.model.resource.felles.PersonResource
import no.novari.fint.model.resource.utdanning.elev.ElevResource
import no.novari.fint.model.resource.utdanning.elev.ElevforholdResource
import no.novari.fint.model.resource.utdanning.utdanningsprogram.SkoleResource
import no.novari.fintkontrolluserservice.FintLinkUtils
import no.novari.fintkontrolluserservice.entra.EntraAttributes
import org.slf4j.LoggerFactory
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
    private val gyldighetsPeriodeService: GyldighetsPeriodeService,
    @Value($$"${fint.kontroll.user.days-before-start-student:0}") private val daysBeforeStart: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun all(now: Date): List<UserCandidate> =
        elevResourceCache.allDistinct
            .filter { student ->
                if (student.elevforhold.isNullOrEmpty()) {
                    logger.warn("Student: Skipping user without elevforhold: resourceId = {}", FintLinkUtils.resourceId(student))
                    false
                } else {
                    true
                }
            }.mapNotNull { create(it, now) }

    fun create(
        student: ElevResource,
        now: Date,
    ): UserCandidate? {
        val resourceId = FintLinkUtils.resourceId(student)
        var tempFINTStatus: String? = null
        val person =
            student.person
                .firstOrNull()
                ?.href
                ?.let(FintLinkUtils::getSystemIdFromMessageKey)
                ?.let { personResourceCache.getOptional(it).orElse(null) }
                ?: run {
                    logger.warn("Student: Person not found for resourceId: $resourceId . User not created")
                    return null
                }
        val entra =
            entraUserService.getEntraAttributes(resourceId) ?: run {
                logger.warn("Student: Entra user not found for resourceId: $resourceId . User status set to INVALID")
                EntraAttributes(
                    email = null,
                    userName = null,
                    objectId = null,
                    status = UserStatus.INVALID,
                )
            }

        val studentRelations =
            student.elevforhold
                .orEmpty()
                .mapNotNull { it.href }
                .map(FintLinkUtils::getSystemIdFromMessageKey)
                .mapNotNull { elevforholdCache.getOptional(it).orElse(null) }
                .filter { gyldighetsPeriodeService.isValid(it.gyldighetsperiode, now, daysBeforeStart) }
        val mainStudentRelation =
            studentRelations.firstOrNull { it.hovedskole == true }
                ?: studentRelations.firstOrNull()
                ?: run {
                    logger.warn("Student: No student relation found for resourceId: $resourceId . Status set to INVALID")
                    tempFINTStatus = UserStatus.INVALID
                    ElevforholdResource()
                }
        val school =
            mainStudentRelation.skole
                ?.firstOrNull()
                ?.href
                ?.let(FintLinkUtils::getSystemIdFromMessageKey)
                ?.let { skoleResourceCache.getOptional(it).orElse(null) }
                ?: run {
                    logger.warn("Student: School not found for resourceId: $resourceId . Status set to INVALID")
                    tempFINTStatus = UserStatus.INVALID
                    SkoleResource()
                }

        val fintStatus =
            tempFINTStatus ?: if (gyldighetsPeriodeService.isValid(
                    mainStudentRelation.gyldighetsperiode,
                    now,
                    daysBeforeStart,
                )
            ) {
                UserStatus.ACTIVE
            } else {
                UserStatus.DISABLED
            }

        return UserCandidate(
            resourceId = resourceId,
            firstName = person.navn?.fornavn,
            lastName = person.navn?.etternavn,
            userType = UserType.STUDENT,
            userName = entra.userName,
            identityProviderUserObjectId = entra.objectId,
            mainOrganisationUnitName = findSchool(school)?.navn,
            mainOrganisationUnitId = findSchool(school)?.organisasjonsId?.identifikatorverdi,
            email = entra.email,
            fintStatus = fintStatus,
            validFrom = studentRelations.mapNotNull { it.gyldighetsperiode?.start }.minOrNull(),
            validTo = studentRelations.mapNotNull { it.gyldighetsperiode?.slutt }.maxOrNull(),
            entraStatus = entra.status,
        )
    }

    private fun findSchool(skole: SkoleResource): OrganisasjonselementResource? =
        skole.organisasjon
            ?.firstOrNull()
            ?.href
            ?.let(FintLinkUtils::getSystemIdFromMessageKey)
            ?.let { organisasjonselementResourceCache.getOptional(it).orElse(null) }
}
