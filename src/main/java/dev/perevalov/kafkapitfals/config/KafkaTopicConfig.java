package dev.perevalov.kafkapitfals.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares Kafka topic required by laboratory scenarios.
 *
 * <p>Problem addressed: relying on ad-hoc topic creation may hide misconfiguration and complicate
 * local bootstrap. Explicit bean keeps setup deterministic.</p>
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Creates transfer events topic with single partition for deterministic ordering in labs.
     *
     * @param transferTopic topic name from configuration
     * @return topic definition for Spring Kafka admin client
     */
    @Bean
    public NewTopic transfersTopic(@Value("${app.kafka.transfer-topic}") String transferTopic) {
        return TopicBuilder.name(transferTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
