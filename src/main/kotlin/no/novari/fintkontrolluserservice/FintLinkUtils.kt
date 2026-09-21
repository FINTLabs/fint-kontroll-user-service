package no.novari.fintkontrolluserservice

import no.novari.fint.model.resource.FintLinks
import no.novari.fint.model.resource.Link
import java.util.function.Supplier

class FintLinkUtils {
    companion object {
        @JvmStatic
        fun getSystemIdFromMessageKey(path: String): String = path.substringAfterLast('/')

        @JvmStatic
        fun getFirstLink(
            linkProducer: Supplier<List<Link>>,
            resource: FintLinks,
            linkedResourceName: String,
        ): String =
            linkProducer
                .get()
                .firstOrNull()
                ?.href
                ?.let(this::systemIdToLowerCase)
                ?: throw NoSuchLinkException.noLink(resource, linkedResourceName)

        @JvmStatic
        fun systemIdToLowerCase(path: String) = path.replace("systemId", "systemid")

        @JvmStatic
        fun organisasjonsIdToLowerCase(path: String) = path.replace("organisasjonsId", "organisasjonsid")

        @JvmStatic
        fun getFirstSelfLink(resource: FintLinks): String =
            resource.selfLinks
                .firstOrNull()
                ?.href
                ?.let(this::systemIdToLowerCase)
                ?: throw NoSuchLinkException.noSelfLink(resource)
    }
}
