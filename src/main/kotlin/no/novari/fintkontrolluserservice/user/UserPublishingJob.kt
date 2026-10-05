package no.novari.fintkontrolluserservice.user

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@Component
class UserPublishingJob(
    private val employeeUserFactory: EmployeeUserFactory,
    private val studentUserFactory: StudentUserFactory,
    private val userService: UserService,
    @Value("\${jobs.delete-user.deleted-since-days:30}") private val deletedSinceDays: Long,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        initialDelayString = "\${fint.kontroll.user.publishing.initial-delay:PT3M}",
        fixedDelayString = "\${fint.kontroll.user.publishing.fixed-delay:PT1H}",
    )
    fun reconcileUsers() {
        val now = Date.from(Instant.now())
        val employees = employeeUserFactory.all(now)
        // val students = studentUserFactory.all(now)
        val students = emptyList<UserCandidate>()
        (employees + students).forEach(userService::saveAndPublish)
        val disabled = userService.deactivateExpiredUsers()
        logger.info(
            "Reconciled {} employees, {} students; disabled {} expired users",
            employees.size,
            students.size,
            disabled.size,
        )
    }

    @Scheduled(cron = "\${jobs.delete-user.interval-cron:0 0 2 * * MON}")
    fun removeDeletedUsers() {
        val count =
            userService.deleteUsersMarkedDeletedBefore(
                Date.from(Instant.now().minus(deletedSinceDays, ChronoUnit.DAYS)),
            )
        logger.info("Removed {} deleted users older than {} days", count, deletedSinceDays)
    }
}
