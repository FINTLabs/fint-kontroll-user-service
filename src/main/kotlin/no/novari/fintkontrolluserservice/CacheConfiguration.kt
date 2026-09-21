package no.novari.fintkontrolluserservice

import no.novari.cache.FintCache
import no.novari.cache.FintCacheManager
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.novari.fint.model.resource.administrasjon.personal.ArbeidsforholdResource
import no.novari.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.novari.fint.model.resource.felles.PersonResource
import no.novari.fint.model.resource.utdanning.elev.ElevResource
import no.novari.fint.model.resource.utdanning.elev.ElevforholdResource
import no.novari.fint.model.resource.utdanning.elev.SkoleressursResource
import no.novari.fint.model.resource.utdanning.utdanningsprogram.SkoleResource
import no.novari.fintkontrollorgunitservice.orgunit.OrgUnit
import no.novari.fintkontrollorgunitservice.orgunitdistance.OrgunitDistance
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Locale

@Configuration
class CacheConfiguration(
    private val fintCacheManager: FintCacheManager,
) {
    @Bean
    fun organisasjonselementCache(): FintCache<String, OrganisasjonselementResource> = createCache(OrganisasjonselementResource::class.java)

    @Bean
    fun publishedOrgunitCache(): FintCache<String, OrgUnit> = createCache(OrgUnit::class.java)

    @Bean
    fun publishedOrgunitDistanceCache(): FintCache<String, OrgunitDistance> = createCache(OrgunitDistance::class.java)

    fun <V : Any> createCache(resourceClass: Class<V>) =
        fintCacheManager.createCache(
            resourceClass.name.lowercase(Locale.ROOT),
            String::class.java,
            resourceClass,
        )
}
