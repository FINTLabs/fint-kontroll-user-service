package no.novari.fintkontrolluserservice

import no.novari.fint.model.resource.FintLinks

class NoSuchLinkException(
    message: String,
) : RuntimeException(message) {
    companion object {
        fun noSelfLink(resource: FintLinks) = NoSuchLinkException("No self link in resource=$resource")

        fun noLink(
            resource: FintLinks,
            linkedResourceName: String,
        ) = NoSuchLinkException("No link for '$linkedResourceName' in resource=$resource")
    }
}
