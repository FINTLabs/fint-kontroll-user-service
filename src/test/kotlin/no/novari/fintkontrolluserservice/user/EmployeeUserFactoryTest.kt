package no.novari.fintkontrolluserservice.user

import io.mockk.every
import io.mockk.mockk
import no.novari.cache.FintCache
import no.novari.fint.model.resource.Link
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.novari.fint.model.resource.administrasjon.personal.ArbeidsforholdResource
import no.novari.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.novari.fint.model.resource.felles.PersonResource
import no.novari.fint.model.resource.utdanning.elev.SkoleressursResource
import no.novari.fintkontrolluserservice.entra.EntraAttributes
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmployeeUserFactoryTest {
    private val personalressursCache = mockk<FintCache<String, PersonalressursResource>>()
    private val personCache = mockk<FintCache<String, PersonResource>>()
    private val arbeidsforholdCache = mockk<FintCache<String, ArbeidsforholdResource>>()
    private val organisasjonselementCache = mockk<FintCache<String, OrganisasjonselementResource>>()
    private val ansattSkoleressursCache = mockk<FintCache<String, SkoleressursResource>>()
    private val entraUserService = mockk<EntraUserService>()
    private val gyldighetsPeriodeService = mockk<GyldighetsPeriodeService>()

    private val factory =
        EmployeeUserFactory(
            personalressursCache,
            personCache,
            arbeidsforholdCache,
            organisasjonselementCache,
            ansattSkoleressursCache,
            entraUserService,
            gyldighetsPeriodeService,
            0,
        )

    @Test
    fun `all skips employees without arbeidsforhold`() {
        every { personalressursCache.allDistinct } returns listOf(employee())

        assertTrue(factory.all(Date()).isEmpty())
    }

    @Test
    fun `create marks employee invalid when no linked employment is found`() {
        val employee =
            employee().apply {
                addPerson(Link("https://example.test/person/person-1"))
                addArbeidsforhold(Link("https://example.test/arbeidsforhold/employment-1"))
            }
        every { personCache.getOptional("person-1") } returns Optional.of(PersonResource())
        every { arbeidsforholdCache.getOptional("employment-1") } returns Optional.empty()
        every { ansattSkoleressursCache.allDistinct } returns emptyList()
        every { entraUserService.getEntraAttributes("employee-1") } returns
            EntraAttributes(
                email = "employee@example.test",
                userName = "employee",
                objectId = null,
                status = UserStatus.ACTIVE,
            )

        val candidate = factory.create(employee, Date())

        assertEquals(UserStatus.INVALID, candidate?.fintStatus)
    }

    private fun employee() =
        PersonalressursResource().apply {
            addSelf(Link("https://example.test/personalressurs/employee-1"))
        }
}
