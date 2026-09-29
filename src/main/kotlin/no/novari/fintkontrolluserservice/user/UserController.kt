package no.novari.fintkontrolluserservice.user

import no.fintlabs.opa.AuthorizationClient
import no.fintlabs.util.OnlyDevelopers
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val userPublisher: UserPublisher,
    private val authorizationClient: AuthorizationClient,
) {
    @GetMapping
    fun users(
        @RequestParam(defaultValue = "%") search: String,
        @RequestParam(required = false) orgUnits: List<String>?,
        @RequestParam(required = false) validOrgUnits: List<String>?,
        @RequestParam(defaultValue = "ALLTYPES") userType: List<String>,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Map<String, Any>> {
        val requested = orgUnits ?: userService.authorizedOrgUnitIds()
        val inScope = intersect(requested, validOrgUnits ?: emptyList())
        val users =
            userService
                .activeUsers(
                    search.removePrefix("%"),
                    userService.authorizedRequestedOrgUnits(inScope),
                    userType,
                ).map(User::toSimpleUser)
        val from = (page * size).coerceAtMost(users.size)
        val to = (from + size).coerceAtMost(users.size)
        return ResponseEntity.ok(
            mapOf(
                "totalItems" to users.size.toLong(),
                "users" to users.subList(from, to),
                "currentPage" to page,
                "totalPages" to if (size > 0) (users.size + size - 1) / size else 0,
            ),
        )
    }

    @GetMapping("/{id}")
    fun user(
        @PathVariable id: Long,
    ): ResponseEntity<DetailedUser> {
        val user = userService.findById(id) ?: return ResponseEntity.notFound().build()
        val authorized = userService.authorizedOrgUnitIds()
        return if (authorized.contains(OrgUnitType.ALL.name) || user.mainOrganisationUnitId in authorized) {
            ResponseEntity.ok(user.toDetailedUser())
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @GetMapping("/me")
    fun loggedOnUser(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Map<String, Any?>> =
        ResponseEntity.ok(
            mapOf(
                "firstName" to jwt.getClaimAsString("given_name"),
                "lastName" to jwt.getClaimAsString("family_name"),
                "organisationId" to jwt.getClaimAsString("orgid"),
                "mail" to email(jwt),
                "roles" to authorizationClient.userRoles,
                "menuItems" to authorizationClient.menuItems,
            ),
        )

    @PostMapping("/me/hasaccess")
    fun hasAccess(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: AccessRequest,
    ): ResponseEntity<List<AccessResponse>> {
        val email = email(jwt)
        return ResponseEntity.ok(
            request.accessRequests.map {
                AccessResponse(
                    url = it.url,
                    access = authorizationClient.isAuthorized(email, it.method, it.url),
                )
            },
        )
    }

    @PostMapping("/republish")
    @OnlyDevelopers
    fun republish(
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<Map<String, Any>> {
        val trigger = "admin (${jwt?.subject ?: "unknown"}) request"
        val count = userPublisher.publishAll(trigger, userService.allUsers())
        return ResponseEntity.ok(mapOf("message" to "Republished $count users", "published" to count))
    }

    @PostMapping("/deactivate-old-users")
    @OnlyDevelopers
    fun deactivateOldUsers(): ResponseEntity<Map<String, Any>> {
        val disabled = userService.deactivateExpiredUsers()
        return ResponseEntity.ok(mapOf("deactivated" to disabled.size))
    }

    private fun intersect(
        left: List<String>,
        right: List<String>,
    ): List<String> =
        when {
            right.isEmpty() -> left
            left.contains(OrgUnitType.ALL.name) -> right
            else -> left.intersect(right.toSet()).toList()
        }

    private fun email(jwt: Jwt): String = jwt.getClaimAsString("email") ?: jwt.subject
}
