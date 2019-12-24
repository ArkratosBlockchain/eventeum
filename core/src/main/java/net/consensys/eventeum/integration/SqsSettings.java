package net.consensys.eventeum.integration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * An encapsulation of Sqs related properties.
 *
 * @author ioBuilders <tech@io.builders>
 */
@Configuration
@ConfigurationProperties(prefix = "sqs")
@Data
public class SqsSettings {

    private String endpointUrl;

    private String awsRegion;
}
