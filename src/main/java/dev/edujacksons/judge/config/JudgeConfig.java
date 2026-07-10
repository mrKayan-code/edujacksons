package dev.edujacksons.judge.config;

import dev.edujacksons.judge.api.JudgeTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Конфигурация модуля judge: свойства Judge0 и декларация Kafka-топиков конвейера проверки.
 * Топики создаёт {@code KafkaAdmin} на старте (single-node, 1 партиция/реплика для MVP).
 */
@Configuration
@EnableConfigurationProperties(Judge0Properties.class)
public class JudgeConfig {

    @Bean
    NewTopic judgeRequestsTopic() {
        return TopicBuilder.name(JudgeTopics.REQUESTS).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic judgeResultsTopic() {
        return TopicBuilder.name(JudgeTopics.RESULTS).partitions(1).replicas(1).build();
    }
}
