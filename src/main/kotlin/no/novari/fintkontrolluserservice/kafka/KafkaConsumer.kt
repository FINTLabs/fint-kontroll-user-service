package no.novari.fintkontrolluserservice.kafka

import no.novari.cache.FintCache
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
import no.novari.fintkontrolluserservice.user.ExternalUserService
import no.novari.fintkontrolluserservice.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)

@Configuration
class KafkaConsumer(
    private val containerFactory: KafkaContainerFactory,
    private val organisasjonselementResourceCache: FintCache<String, OrganisasjonselementResource>,
    private val personalressursResourceCache: FintCache<String, PersonalressursResource>,
    private val personResourceCache: FintCache<String, PersonResource>,
    private val arbeidsforholdResourceCache: FintCache<String, ArbeidsforholdResource>,
    private val elevResourceCache: FintCache<String, ElevResource>,
    private val elevforholdCache: FintCache<String, ElevforholdResource>,
    private val skoleResourceCache: FintCache<String, SkoleResource>,
    private val ansattSkoleressursResourceCache: FintCache<String, SkoleressursResource>,
    private val graphUserCache: FintCache<String, EntraUser>,
    private val graphUserExternalCache: FintCache<String, EntraUserExternal>,
    private val userService: UserService,
    private val externalUserService: ExternalUserService,
) {
    @Bean
    fun organisasjonselementConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-organisasjon-organisasjonselement",
            consumingClass = OrganisasjonselementResource::class,
            cache = organisasjonselementResourceCache,
            handler = { key, value ->
                logger.debug("Consumed:: organisasjonselement ::  $key :: ${value?.navn ?: "deleted"}")
                // TODO: create handler
            },
        )

    @Bean
    fun personalressursConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-personal-personalressurs",
            consumingClass = PersonalressursResource::class,
            cache = personalressursResourceCache,
            handler = { key, value ->
                if (value == null) userService.markDeleted(key)
                logger.debug("Consumed:: personalressurs ::  $key :: ${value?.ansattnummer ?: "deleted"}")
                // TODO: create handler
            },
        )

    @Bean
    fun ansattPersonResourceConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-personal-person",
            consumingClass = PersonResource::class,
            cache = personResourceCache,
            handler = { key, value ->
                logger.debug("Consumed:: ansatt person :: $key :: ${value?.navn ?: "deleted"}")
            },
        )

    @Bean
    fun arbeidsforholdResourceConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-personal-arbeidsforhold",
            consumingClass = ArbeidsforholdResource::class,
            cache = arbeidsforholdResourceCache,
            handler = { key, value ->
                logger
                    .info("Consumed:: arbeidsforhold :: $key :: ${value?.systemId?.identifikatorverdi ?: "deleted"}")
            },
        )

    @Bean
    fun elevConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-elev-elev",
            consumingClass = ElevResource::class,
            cache = elevResourceCache,
            handler = { key, value ->
                if (value == null) userService.markDeleted(key)
                logger.debug("Consumed:: elev :: $key :: ${value?.elevnummer?.identifikatorverdi ?: "deleted"}")
            },
        )

    @Bean
    fun elevPersonConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-elev-person",
            consumingClass = PersonResource::class,
            cache = personResourceCache,
            handler = { key, value ->
                logger.debug("Consumed:: elevperson :: $key :: ${value?.navn ?: "deleted"}")
            },
        )

    @Bean
    fun elevforholdConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-elev-elevforhold",
            consumingClass = ElevforholdResource::class,
            cache = elevforholdCache,
            handler = { key, value ->
                logger.debug("Consumed:: elevforhold :: $key :: ${value?.systemId?.identifikatorverdi ?: "deleted"}")
            },
        )

    @Bean
    fun skoleConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-utdanningsprogram-skole",
            consumingClass = SkoleResource::class,
            cache = skoleResourceCache,
            handler = { key, value ->
                logger.debug("Consumed:: skole :: $key :: ${value?.navn ?: "deleted"}")
            },
        )

    @Bean
    fun ansattSkoleConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-elev-skoleressurs",
            consumingClass = SkoleressursResource::class,
            cache = ansattSkoleressursResourceCache,
            handler = { key, value ->
                logger.debug("Consumed:: ansatt skole :: $key :: ${value?.systemId?.identifikatorverdi ?: "deleted"}")
            },
        )

    @Bean
    fun entraUserConsumer() =
        containerFactory.createContainer(
            topicName = "graph-user",
            consumingClass = EntraUser::class,
            cache = graphUserCache,
            handler = { key, value ->
                if (value == null) {
                    graphUserCache.allDistinct
                        .filter { it.id == key }
                        .forEach { user ->
                            listOfNotNull(user.employeeId, user.studentId).forEach { resourceId ->
                                graphUserCache.remove(resourceId)
                                userService.markDeleted(resourceId)
                            }
                        }
                } else {
                    value.employeeId?.let { graphUserCache.put(it, value) }
                    value.studentId?.let { graphUserCache.put(it, value) }
                    logger.debug("Consumed:: entra user :: $key :: ${value.userPrincipalName}")
                }
            },
        )

    @Bean
    fun externalEntraUserConsumer() =
        containerFactory.createContainer(
            topicName = "graph-user-external",
            consumingClass = EntraUserExternal::class,
            cache = graphUserExternalCache,
            handler = { key, value ->
                externalUserService.reconcile(key, value)
                logger.debug("Consumed:: entra user external :: $key :: ${value?.userPrincipalName ?: "deleted"}")
            },
        )
}
