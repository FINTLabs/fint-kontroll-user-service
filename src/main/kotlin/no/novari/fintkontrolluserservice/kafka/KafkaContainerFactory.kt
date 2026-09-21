package no.novari.fintkontrolluserservice.kafka

import no.novari.cache.FintCache
import no.novari.fintkontrolluserservice.FintLinkUtils
import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class KafkaContainerFactory(
    private val parameterizedListenerContainerFactoryService: ParameterizedListenerContainerFactoryService,
    private val errorHandlerFactory: ErrorHandlerFactory,
) {
    private val logger = LoggerFactory.getLogger(KafkaContainerFactory::class.java)

    fun <T : Any> createContainer(
        topicName: String,
        consumingClass: KClass<T>,
        cache: FintCache<String, T>,
        handler: (String, T) -> Unit = { _, _ -> },
    ): ConcurrentMessageListenerContainer<String, T> {
        val nameParameters =
            EntityTopicNameParameters
                .builder()
                .resourceName(topicName)
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).build()

        val listenerConfiguration =
            ListenerConfiguration
                .stepBuilder()
                .groupIdApplicationDefault()
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                // leser fra starten av topic
                .seekToBeginningOnAssignment()
                .build()

        val errorHandler =
            errorHandlerFactory.createErrorHandler<T>(
                ErrorHandlerConfiguration
                    .stepBuilder<T>()
                    .noRetries()
                    .skipFailedRecords()
                    .build(),
            )

        val listenerContainerFactory =
            parameterizedListenerContainerFactoryService.createRecordListenerContainerFactory(
                consumingClass.java,
                { record: ConsumerRecord<String, T> ->
                    val key = FintLinkUtils.getSystemIdFromMessageKey(record.key())
                    val value = record.value()

                    cache.put(key, value)
                    logger.debug("Added $key to cache from record key: ${record.key()}")

                    handler(key, value)
                },
                listenerConfiguration,
                errorHandler,
            )

        return listenerContainerFactory.createContainer(nameParameters)
    }
}
