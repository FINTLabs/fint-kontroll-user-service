package no.novari.fintkontrolluserservice.user

import no.fintlabs.opa.AuthorizationClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Date

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userPublisher: UserPublisher,
    private val authorizationClient: AuthorizationClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun upsert(candidate: UserCandidate): User {
        val existing = userRepository.findByResourceIdIgnoreCase(candidate.resourceId)
        val user = existing ?: User(resourceId = candidate.resourceId)
        val previous = user.copy(organisationUnitIds = user.organisationUnitIds.toMutableSet())
        val newStatus = statusFor(candidate)

        user.firstName = candidate.firstName
        user.lastName = candidate.lastName
        user.userType = candidate.userType.name
        user.userName = candidate.userName
        user.identityProviderUserObjectId = candidate.identityProviderUserObjectId
        user.mainOrganisationUnitName = candidate.mainOrganisationUnitName
        user.mainOrganisationUnitId = candidate.mainOrganisationUnitId
        user.organisationUnitIds = candidate.organisationUnitIds.toMutableSet()
        user.email = candidate.email
        user.managerRef = candidate.managerRef
        user.validFrom = candidate.validFrom
        user.validTo = candidate.validTo
        user.status = newStatus
        if (previous.status != newStatus || existing == null) user.statusChanged = Date.from(Instant.now())

        if (existing == null || user != previous) {
            val saved = userRepository.save(user)
            logger.info(
                "{} user resourceId={}, status={}",
                if (existing ==
                    null
                ) {
                    "Created"
                } else {
                    "Updated"
                },
                saved.resourceId,
                saved.status,
            )
            userPublisher.publish(saved)
            return saved
        }
        return user
    }

    @Transactional
    fun markDeleted(resourceId: String): User? {
        val user = userRepository.findByResourceIdIgnoreCase(resourceId) ?: return null
        if (user.status != UserStatus.DELETED) {
            user.status = UserStatus.DELETED
            user.statusChanged = Date.from(Instant.now())
            userPublisher.publish(userRepository.save(user))
        }
        return user
    }

    @Transactional
    fun deactivateExpiredUsers(): List<User> {
        val now = Instant.now()
        return userRepository
            .findAll()
            .filter { it.status == UserStatus.ACTIVE && it.validTo?.toInstant()?.isBefore(now) == true }
            .onEach {
                it.status = UserStatus.DISABLED
                it.statusChanged = Date.from(now)
                userPublisher.publish(it)
            }.also(userRepository::saveAll)
    }

    @Transactional
    fun deleteUsersMarkedDeletedBefore(cutoff: Date): Int {
        val deleted =
            userRepository.findAll().filter {
                it.status == UserStatus.DELETED && it.statusChanged?.before(cutoff) == true
            }
        deleted.forEach { userPublisher.publishTombstone(it.resourceId) }
        userRepository.deleteAll(deleted)
        return deleted.size
    }

    @Transactional(readOnly = true)
    fun allUsers() = userRepository.findAll().filter { it.identityProviderUserObjectId != null }

    @Transactional(readOnly = true)
    fun findById(id: Long): User? = userRepository.findById(id).orElse(null)

    fun authorizedOrgUnitIds(): List<String> =
        authorizationClient.userScopesList
            .filter { it.objectType == "user" }
            .flatMap { it.orgUnits }

    fun authorizedRequestedOrgUnits(requested: Collection<String>): List<String> {
        val authorized = authorizedOrgUnitIds()
        return if (authorized.contains(
                OrgUnitType.ALL.name,
            )
        ) {
            requested.toList()
        } else {
            requested.filter(authorized::contains)
        }
    }

    @Transactional(readOnly = true)
    fun activeUsers(
        search: String,
        orgUnits: Collection<String>,
        userTypes: Collection<String>,
    ): List<User> {
        val normalizedSearch =
            search
                .trim()
                .lowercase()
                .split(Regex("\\s+"))
                .filter(String::isNotBlank)
        return userRepository
            .findAll()
            .asSequence()
            .filter { it.status == UserStatus.ACTIVE }
            .filter { orgUnits.contains(OrgUnitType.ALL.name) || it.mainOrganisationUnitId in orgUnits }
            .filter { userTypes.contains("ALLTYPES") || it.userType in userTypes }
            .filter { user ->
                normalizedSearch.all { term ->
                    listOf(user.firstName, user.lastName, user.userName).any { it?.lowercase()?.contains(term) == true }
                }
            }.sortedWith(compareBy<User> { it.lastName }.thenBy { it.firstName })
            .toList()
    }

    private fun statusFor(candidate: UserCandidate): String =
        when {
            candidate.entraStatus == UserStatus.DELETED -> UserStatus.DELETED
            candidate.fintStatus == UserStatus.INVALID -> UserStatus.INVALID
            candidate.entraStatus == UserStatus.ACTIVE && candidate.fintStatus == UserStatus.ACTIVE -> UserStatus.ACTIVE
            else -> UserStatus.DISABLED
        }
}

enum class OrgUnitType {
    ALL,
}
