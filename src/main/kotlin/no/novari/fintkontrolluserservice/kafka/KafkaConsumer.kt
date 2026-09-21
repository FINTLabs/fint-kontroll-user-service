package no.novari.fintkontrolluserservice.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.cache.FintCache
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)

@Configuration
class KafkaConsumer(
    private val containerFactory: KafkaContainerFactory,
    private val organisasjonselementCache: FintCache<String, OrganisasjonselementResource>,
) {
    @Bean
    fun organisasjonselementConsumer() =
        containerFactory.createContainer(
            topicName = "administrasjon-organisasjon-organisasjonselement",
            consumingClass = OrganisasjonselementResource::class,
            cache = organisasjonselementCache,
            handler = { key, value ->
                logger.info("Consumed: $key :: ${value.navn ?: "no navn"}")
                // TODO: create orgunitHandler
            },
        )
}
