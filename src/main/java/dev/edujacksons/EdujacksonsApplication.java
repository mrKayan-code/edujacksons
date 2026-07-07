package dev.edujacksons;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Точка входа модульного монолита EduJacksons.
 * Модули живут в подпакетах {@code dev.edujacksons.<module>} с границами api/domain/repository/service/config.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EdujacksonsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdujacksonsApplication.class, args);
    }
}
