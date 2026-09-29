package no.novari.fintkontrolluserservice.user

import no.novari.fint.model.felles.kompleksedatatyper.Periode
import org.springframework.stereotype.Service
import java.util.Calendar
import java.util.Date

@Service
class ValidityPeriodService {
    fun isValid(
        period: Periode?,
        now: Date,
        daysBeforeStart: Int,
    ): Boolean {
        if (period?.start == null) return false

        val start =
            Calendar
                .getInstance()
                .apply {
                    time = period.start
                    add(Calendar.DATE, -daysBeforeStart)
                }.time

        return !now.before(start) && (period.slutt == null || now.before(period.slutt))
    }
}
