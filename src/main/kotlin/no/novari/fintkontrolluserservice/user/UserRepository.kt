package no.novari.fintkontrolluserservice.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
interface UserRepository :
    JpaRepository<User, Long>,
    JpaSpecificationExecutor<User> {
    fun findByResourceIdIgnoreCase(resourceId: String): User?

    fun deleteByStatusAndStatusChangedBefore(
        status: String,
        statusChangedBefore: Date,
    ): Int
}
