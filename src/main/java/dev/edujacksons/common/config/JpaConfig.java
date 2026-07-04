package dev.edujacksons.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Включает JPA-аудит для {@code @CreatedDate} / {@code @LastModifiedDate} в BaseEntity. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
