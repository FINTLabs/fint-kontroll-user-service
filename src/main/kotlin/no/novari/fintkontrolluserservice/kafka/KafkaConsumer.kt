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
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)

@Configuration
class KafkaConsumer(
    private val containerFactory: KafkaContainerFactory,
    private val organisasjonselementResourceCache: FintCache<String, OrganisasjonselementResource>,
    private val personalressursResourceCache: FintCache<String, PersonalressursResource>,
    private val ansattPersonResourceCache: FintCache<String, PersonResource>,
    private val arbeidsforholdResourceCache: FintCache<String, ArbeidsforholdResource>,
    private val elevResourceCache: FintCache<String, ElevResource>,
    private val elevPersonCache: FintCache<String, PersonResource>,
    private val elevforholdCache: FintCache<String, ElevforholdResource>,
    private val skoleResourceCache: FintCache<String, SkoleResource>,
    private val ansattSkoleressursResourceCache: FintCache<String, SkoleressursResource>,
    private val graphUserCache: FintCache<String, EntraUser>,
    private val graphUserExternalCache: FintCache<String, EntraUserExternal>
) {
    @Bean
    fun organisasjonselementConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-organisasjon-organisasjonselement",
            consumingClass = OrganisasjonselementResource::class,
            cache = organisasjonselementResourceCache,
            handler = { key, value ->
                logger.info("Consumed:: organisasjonselement ::  $key :: ${value.navn ?: "no navn"}")
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
                logger.info("Consumed:: personalressurs ::  $key :: ${value.ansattnummer ?: "no employeeNumber"}")
                // TODO: create handler
            },
        )

    @Bean
    fun ansattPersonResourceConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-personal-person",
            consumingClass = PersonResource::class,
            cache = ansattPersonResourceCache,
            handler = { key, value ->
                logger.info("Consumed:: ansatt person :: $key :: ${value.navn ?: "no name"}")
            },
        )

    @Bean
    fun arbeidsforholdResourceConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-personal-arbeidsforhold",
            consumingClass = ArbeidsforholdResource::class,
            cache = arbeidsforholdResourceCache,
            handler = { key, value ->
                logger.info("Consumed:: arbeidsforhold :: $key :: ${value.systemId.identifikatorverdi ?: "no systemId"}")
            },
        )

    @Bean
    fun elevConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-elev-elev",
            consumingClass = ElevResource::class,
            cache = elevResourceCache,
            handler = { key, value ->
                logger.info("Consumed:: elev :: $key :: ${value.brukernavn.identifikatorverdi ?: "no brukernavn"}")
            },
        )

    @Bean
    fun elevPersonConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-elev-person",
            consumingClass = PersonResource::class,
            cache = elevPersonCache,
            handler = { key, value ->
                logger.info("Consumed:: elevperson :: $key :: ${value.navn ?: "no name"}")
            },
        )

    @Bean
    fun elevforholdConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-elev-elevforhold",
            consumingClass = ElevforholdResource::class,
            cache = elevforholdCache,
            handler = { key, value ->
                logger.info("Consumed:: elevforhold :: $key :: ${value.systemId.identifikatorverdi ?: "no systemId"}")
            },
        )

    @Bean
    fun skoleConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-utdanningsprogram-skole",
            consumingClass = SkoleResource::class,
            cache = skoleResourceCache,
            handler = { key, value ->
                logger.info("Consumed:: skole :: $key :: ${value.navn ?: "no name"}")
            },
        )

    @Bean
    fun ansattSkoleConsumer() =
        containerFactory.createContainer(
            topicName = "utdanning-elev-skoleressurs",
            consumingClass = SkoleressursResource::class,
            cache = ansattSkoleressursResourceCache,
            handler = { key, value ->
                logger.info("Consumed:: ansatt skole :: $key :: ${value.systemId.identifikatorverdi ?: "no systemId"}")
            },
        )

    @Bean
    fun entraUserConsumer() =
        containerFactory.createContainer(
            topicName = "graph-user",
            consumingClass = EntraUser::class,
            cache = graphUserCache,
            handler = { key, value ->
                logger.info("Consumed:: entra user :: $key :: ${value.userPrincipalName ?: "no userprincipalname"}")
            },
        )

    @Bean
    fun externalEntraUserConsumer() =
        containerFactory.createContainer(
            topicName = "graph-user-external",
            consumingClass = EntraUserExternal::class,
            cache = graphUserExternalCache,
            handler = { key, value ->
                logger.info("Consumed:: entra user external :: $key :: ${value.userPrincipalName ?: "no userprincipalname"}")
            }
        )

}
