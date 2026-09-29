package no.novari.fintkontrolluserservice.user

import no.novari.cache.FintCache
import no.novari.fint.model.resource.Link
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.novari.fint.model.resource.administrasjon.personal.ArbeidsforholdResource
import no.novari.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.novari.fint.model.resource.felles.PersonResource
import no.novari.fint.model.resource.utdanning.elev.SkoleressursResource
import no.novari.fintkontrolluserservice.FintLinkUtils
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
    private val validityPeriodService: ValidityPeriodService,
    @Value("\${fint.kontroll.user.days-before-start-employee:0}") private val daysBeforeStart: Int,
) {
    fun all(now: Date): List<UserCandidate> =
        personalressursResourceCache.allDistinct
            .filter { it.arbeidsforhold?.isNotEmpty() == true }
            .mapNotNull { create(it, now) }

    fun create(
        employee: PersonalressursResource,
        now: Date,
    ): UserCandidate? {
        val resourceId = FintLinkUtils.resourceId(employee)
        val person =
            employee.person
                .firstOrNull()
                ?.href
                ?.let(FintLinkUtils::getSystemIdFromMessageKey)
                ?.let { personResourceCache.getOptional(it).orElse(null) }
                ?: return null
        val entra = entraUserService.get(resourceId) ?: return null

        val employments =
            employee.arbeidsforhold
                .orEmpty()
                .mapNotNull(Link::getHref)
                .map(FintLinkUtils::getSystemIdFromMessageKey)
                .mapNotNull { arbeidsforholdResourceCache.getOptional(it).orElse(null) }
                .filter { validityPeriodService.isValid(it.gyldighetsperiode, now, daysBeforeStart) }

        val mainEmployment =
            employments.firstOrNull { it.hovedstilling == true }
                ?: employments.maxByOrNull { it.ansettelsesprosent ?: 0L }
                ?: return null

        val workPlaces = employments.mapNotNull(::workPlace).toSet()
        val mainWorkPlace = workPlace(mainEmployment)
        val managerRef = mainWorkPlace?.leder?.firstOrNull()?.href
        val period = employee.ansettelsesperiode

        return UserCandidate(
            resourceId = resourceId,
            firstName = person.navn?.fornavn,
            lastName = person.navn?.etternavn,
            userType = if (isSchoolEmployee(resourceId)) UserType.EMPLOYEEFACULTY else UserType.EMPLOYEESTAFF,
            userName = entra.userName,
            identityProviderUserObjectId = entra.objectId,
            mainOrganisationUnitName = mainWorkPlace?.navn,
            mainOrganisationUnitId = mainWorkPlace?.organisasjonsId?.identifikatorverdi,
            organisationUnitIds = workPlaces.mapNotNull { it.organisasjonsId?.identifikatorverdi }.toSet(),
            email = entra.email,
            managerRef = managerRef,
            fintStatus =
                if (validityPeriodService.isValid(
                        period,
                        now,
                        daysBeforeStart,
                    )
                ) {
                    UserStatus.ACTIVE
                } else {
                    UserStatus.DISABLED
                },
            validFrom = period?.start,
            validTo = period?.slutt,
            entraStatus = entra.status,
        )
    }

    private fun workPlace(employment: ArbeidsforholdResource): OrganisasjonselementResource? =
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
