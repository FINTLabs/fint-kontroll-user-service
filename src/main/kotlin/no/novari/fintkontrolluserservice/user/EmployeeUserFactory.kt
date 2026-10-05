package no.novari.fintkontrolluserservice.user

import no.novari.cache.FintCache
import no.novari.fint.model.resource.Link
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.novari.fint.model.resource.administrasjon.personal.ArbeidsforholdResource
import no.novari.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.novari.fint.model.resource.felles.PersonResource
import no.novari.fint.model.resource.utdanning.elev.SkoleressursResource
import no.novari.fintkontrolluserservice.FintLinkUtils
import no.novari.fintkontrolluserservice.entra.EntraAttributes
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class EmployeeUserFactory(
    private val personalressursResourceCache: FintCache<String, PersonalressursResource>,
    private val personResourceCache: FintCache<String, PersonResource>,
    private val arbeidsforholdResourceCache: FintCache<String, ArbeidsforholdResource>,
    private val organisasjonselementResourceCache: FintCache<String, OrganisasjonselementResource>,
    private val ansattSkoleressursResourceCache: FintCache<String, SkoleressursResource>,
    private val entraUserService: EntraUserService,
    private val gyldighetsPeriodeService: GyldighetsPeriodeService,
    @Value($$"${fint.kontroll.user.days-before-start-employee:0}") private val daysBeforeStart: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun all(now: Date): List<UserCandidate> =
        personalressursResourceCache.allDistinct
            .filter { employee ->
                if (employee.arbeidsforhold.isNullOrEmpty()) {
                    logger.warn("Skipping employee without arbeidsforhold: resourceId={}", FintLinkUtils.resourceId(employee))
                    false
                } else {
                    true
                }
            }.mapNotNull { create(it, now) }

    fun create(
        employee: PersonalressursResource,
        now: Date,
    ): UserCandidate? {
        val resourceId = FintLinkUtils.resourceId(employee)
        var tempStatus: String? = null
        val person =
            employee.person
                .firstOrNull()
                ?.href
                ?.let(FintLinkUtils::getSystemIdFromMessageKey)
                ?.let { personResourceCache.getOptional(it).orElse(null) }
                ?: run {
                    logger.warn("Person not found for resourceId: $resourceId . User not created")
                    return null
                }

        val entraAttributes =
            entraUserService.getEntraAttributes(resourceId) ?: run {
                logger.warn("Entra user not found for resourceId: $resourceId")
                EntraAttributes(
                    email = null,
                    userName = null,
                    objectId = null,
                    status = UserStatus.INVALID,
                )
            }

        val employments =
            employee.arbeidsforhold
                .orEmpty()
                .mapNotNull(Link::getHref)
                .map(FintLinkUtils::getSystemIdFromMessageKey)
                .mapNotNull { arbeidsforholdResourceCache.getOptional(it).orElse(null) }
                .filter { gyldighetsPeriodeService.isValid(it.gyldighetsperiode, now, daysBeforeStart) }

        val mainEmployment =
            employments.firstOrNull { it.hovedstilling == true }
                ?: employments.maxByOrNull { it.ansettelsesprosent ?: 0L }
                ?: run {
                    logger.warn("No main employment posision found for resourceId: $resourceId . Status set to INVALID")
                    tempStatus = UserStatus.INVALID
                    ArbeidsforholdResource()
                }

        val workPlaces = employments.mapNotNull(::findArbeidssted).toSet()
        val mainWorkPlace = findArbeidssted(mainEmployment)
        val managerRef = mainWorkPlace?.leder?.firstOrNull()?.href
        val period = employee.ansettelsesperiode
        val fintStatus =
            tempStatus ?: if (gyldighetsPeriodeService.isValid(
                    period,
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
            userType = if (isSchoolEmployee(resourceId)) UserType.EMPLOYEEFACULTY else UserType.EMPLOYEESTAFF,
            userName = entraAttributes.userName,
            identityProviderUserObjectId = entraAttributes.objectId,
            mainOrganisationUnitName = mainWorkPlace?.navn,
            mainOrganisationUnitId = mainWorkPlace?.organisasjonsId?.identifikatorverdi,
            organisationUnitIds = workPlaces.mapNotNull { it.organisasjonsId?.identifikatorverdi }.toSet(),
            email = entraAttributes.email,
            managerRef = managerRef,
            fintStatus = fintStatus,
            validFrom = period?.start,
            validTo = period?.slutt,
            entraStatus = entraAttributes.status,
        )
    }

    private fun findArbeidssted(employment: ArbeidsforholdResource): OrganisasjonselementResource? =
        employment.arbeidssted
            ?.firstOrNull()
            ?.href
            ?.let(FintLinkUtils::getSystemIdFromMessageKey)
            ?.let { organisasjonselementResourceCache.getOptional(it).orElse(null) }

    private fun isSchoolEmployee(resourceId: String): Boolean =
        ansattSkoleressursResourceCache.allDistinct.any { schoolResource ->
            schoolResource.personalressurs
                ?.firstOrNull()
                ?.href
                ?.let(FintLinkUtils::getSystemIdFromMessageKey) == resourceId
        }
}
