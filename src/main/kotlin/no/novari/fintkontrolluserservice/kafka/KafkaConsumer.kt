package no.novari.fintkontrolluserservice.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.cache.FintCache
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.novari.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.novari.fint.model.resource.felles.PersonResource
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)

@Configuration
class KafkaConsumer(
    private val containerFactory: KafkaContainerFactory,
    private val organisasjonselementCache: FintCache<String, OrganisasjonselementResource>,
    private val personalressursCache: FintCache<String, PersonalressursResource>,
) {
    @Bean
    fun organisasjonselementConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-organisasjon-organisasjonselement",
            consumingClass = OrganisasjonselementResource::class,
            cache = organisasjonselementCache,
            handler = { key, value ->
                logger.info("Consumed:: organisasjonselement ::  $key :: ${value.navn ?: "no navn"}")
                // TODO: create orgunitHandler
            },
        )

    @Bean
    fun personalressursConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-personal-personalressurs",
            consumingClass = PersonResource::class,
            cache = organisasjonselementCache,
            handler = { key, value ->
                logger.info("Consumed:: organisasjonselement ::  $key :: ${value.navn ?: "no navn"}")
                // TODO: create orgunitHandler
            },
        )
}
