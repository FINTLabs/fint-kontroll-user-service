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
import no.novari.fintkontrolluserservice.entra.EntraUser
import no.novari.fintkontrolluserservice.entra.EntraUserExternal
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Locale

@Configuration
class CacheConfiguration(
    private val fintCacheManager: FintCacheManager,
) {
    @Bean
    fun organisasjonselementResourceCache(): FintCache<String, OrganisasjonselementResource> =
        createCache(OrganisasjonselementResource::class.java)

    @Bean
    fun personalressursResourceCache(): FintCache<String, PersonalressursResource> = createCache(PersonalressursResource::class.java)

    @Bean
    fun ansattPersonResourceCache(): FintCache<String, PersonResource> = createCache(PersonResource::class.java)

    @Bean
    fun arbeidsforholdResourceCache(): FintCache<String, ArbeidsforholdResource> = createCache(ArbeidsforholdResource::class.java)

    @Bean
    fun elevResourceCache(): FintCache<String, ElevResource> = createCache(ElevResource::class.java)

    @Bean
    fun elevPersonCache(): FintCache<String, PersonResource> = createCache(PersonResource::class.java)

    @Bean
    fun elevforholdCache(): FintCache<String, ElevforholdResource> = createCache(ElevforholdResource::class.java)

    @Bean
    fun skoleResourceCache(): FintCache<String, SkoleResource> = createCache(SkoleResource::class.java)

    @Bean
    fun ansattSkoleressursResourceCache(): FintCache<String, SkoleressursResource> = createCache(SkoleressursResource::class.java)

    @Bean
    fun graphUserCache(): FintCache<String, EntraUser> = createCache(EntraUser::class.java)

    @Bean
    fun graphUserExternalCache(): FintCache<String, EntraUserExternal> = createCache(EntraUserExternal::class.java)

    fun <V : Any> createCache(resourceClass: Class<V>) =
        fintCacheManager.createCache(
            resourceClass.name.lowercase(Locale.ROOT),
            String::class.java,
            resourceClass,
        )
}
