package no.novari.fintkontrolluserservice.user

import no.novari.kafka.producing.ParameterizedProducerRecord
import no.novari.kafka.producing.ParameterizedTemplate
import no.novari.kafka.producing.ParameterizedTemplateFactory
import no.novari.kafka.topic.EntityTopicService
import no.novari.kafka.topic.configuration.EntityCleanupFrequency
import no.novari.kafka.topic.configuration.EntityTopicConfiguration
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class UserPublisher(
    templateFactory: ParameterizedTemplateFactory,
    entityTopicService: EntityTopicService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val template: ParameterizedTemplate<User> = templateFactory.createTemplate(User::class.java)
    private val topic =
        EntityTopicNameParameters
            .builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters
                    .stepBuilder()
                    .orgIdApplicationDefault()
                    .domainContextApplicationDefault()
                    .build(),
            ).resourceName("kontrolluser")
            .build()

    init {
        entityTopicService.createOrModifyTopic(
            topic,
            EntityTopicConfiguration
                .stepBuilder()
                .partitions(1)
                .lastValueRetainedForever()
                .nullValueRetentionTime(Duration.ofDays(7))
                .cleanupFrequency(EntityCleanupFrequency.NORMAL)
                .build(),
        )
    }

    fun publish(user: User) {
        logger.info("Publishing kontrolluser resourceId={}, status={}", user.resourceId, user.status)
        template.send(
            ParameterizedProducerRecord
                .builder<User>()
                .topicNameParameters(topic)
                .key(user.resourceId)
                .value(user)
                .build(),
        )
    }

    fun publishAll(
        trigger: String,
        users: Collection<User>,
    ): Int {
        logger.info("Republishing {} kontrollusers triggered by {}", users.size, trigger)
        users.forEach(::publish)
        return users.size
    }

    fun publishTombstone(resourceId: String) {
        template.send(
            ParameterizedProducerRecord
                .builder<User>()
                .topicNameParameters(topic)
                .key(resourceId)
                .build(),
        )
    }
}
