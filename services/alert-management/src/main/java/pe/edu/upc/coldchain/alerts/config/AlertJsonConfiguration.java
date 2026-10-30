package pe.edu.upc.coldchain.alerts.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlertJsonConfiguration {
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "[0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,9})?([Zz]|[+-][0-9]{2}:[0-9]{2})");

    @Bean
    SimpleModule alertInputModule() {
        SimpleModule module = new SimpleModule("alert-input");
        module.addDeserializer(UUID.class, new JsonDeserializer<>() {
            @Override
            public UUID deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                if (parser.hasToken(JsonToken.VALUE_STRING) && UUID_PATTERN.matcher(parser.getText()).matches()) {
                    return UUID.fromString(parser.getText());
                }
                return (UUID) context.handleUnexpectedToken(UUID.class, parser);
            }
        });
        module.addDeserializer(BigDecimal.class, new JsonDeserializer<>() {
            @Override
            public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT) && !parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                    return (BigDecimal) context.handleUnexpectedToken(BigDecimal.class, parser);
                }
                return parser.getDecimalValue();
            }
        });
        module.addDeserializer(Instant.class, new JsonDeserializer<>() {
            @Override
            public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                if (parser.hasToken(JsonToken.VALUE_STRING) && TIMESTAMP_PATTERN.matcher(parser.getText()).matches()) {
                    try {
                        OffsetDateTime value = OffsetDateTime.parse(parser.getText());
                        Instant instant = value.toInstant();
                        int utcYear = instant.atOffset(ZoneOffset.UTC).getYear();
                        if (value.getYear() >= 1 && utcYear >= 1 && utcYear <= 9999) {
                            return instant;
                        }
                    } catch (DateTimeParseException ignored) {
                        // Reject invalid calendar dates instead of normalizing them.
                    }
                }
                return (Instant) context.handleUnexpectedToken(Instant.class, parser);
            }
        });
        return module;
    }
}
