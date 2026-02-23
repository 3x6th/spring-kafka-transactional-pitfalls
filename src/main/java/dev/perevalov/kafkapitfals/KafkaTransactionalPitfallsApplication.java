package dev.perevalov.kafkapitfals;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for Kafka transactional pitfalls laboratory.
 *
 * <p>The project contains intentionally unsafe and fixed implementations to compare behavior under
 * crashes and duplicate deliveries.</p>
 */
@SpringBootApplication
public class KafkaTransactionalPitfallsApplication {

    /**
     * Boots Spring application context.
     *
     * @param args process arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KafkaTransactionalPitfallsApplication.class, args);
    }
}
