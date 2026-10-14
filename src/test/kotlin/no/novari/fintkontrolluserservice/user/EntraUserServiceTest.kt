package no.novari.fintkontrolluserservice.user

import io.mockk.mockk
import no.novari.cache.FintCache
import no.novari.fintkontrolluserservice.entra.EntraUser
import no.novari.fintkontrolluserservice.entra.EntraUserPayload
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EntraUserServiceTest {
    private val service = EntraUserService(mockk<FintCache<String, EntraUser>>())

    @Test
    fun `converts a Kafka payload to an Entra user using the Kafka key as id`() {
        val payload =
            EntraUserPayload(
                mail = "employee@example.test",
                userPrincipalName = "employee@example.test",
                employeeId = "employee-1",
                studentId = null,
                accountEnabled = true,
            )

        val entraUser = service.fromPayload("graph-user-1", payload)

        assertEquals("graph-user-1", entraUser.id)
        assertEquals(payload.mail, entraUser.mail)
        assertEquals(payload.userPrincipalName, entraUser.userPrincipalName)
        assertEquals(payload.employeeId, entraUser.employeeId)
        assertEquals(payload.studentId, entraUser.studentId)
        assertEquals(payload.accountEnabled, entraUser.accountEnabled)
        assertFalse(entraUser.isDeleted!!)
    }
}
