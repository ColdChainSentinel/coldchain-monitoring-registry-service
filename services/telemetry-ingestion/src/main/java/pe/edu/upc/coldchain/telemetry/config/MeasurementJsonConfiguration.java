package pe.edu.upc.coldchain.telemetry.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
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
public class MeasurementJsonConfiguration {
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "[0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,9})?([Zz]|[+-][0-9]{2}:[0-9]{2})");

    static UUID parseUuid(String value) {
        if (!UUID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("UUID canónico requerido");
        }
        return UUID.fromString(value);
    }

    @Bean
    SimpleModule measurementInputModule() {
        SimpleModule module = new SimpleModule("measurement-input");
        module.addDeserializer(UUID.class, new JsonDeserializer<>() {
            @Override
            public UUID deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                if (parser.hasToken(JsonToken.VALUE_STRING)) {
                    try {
                        return parseUuid(parser.getText());
                    } catch (IllegalArgumentException ignored) {
                        // Return the same validation failure for all malformed UUIDs.
                    }
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
                        if (value.getYear() >= 1 && instant.atOffset(java.time.ZoneOffset.UTC).getYear() >= 1
                                && instant.atOffset(java.time.ZoneOffset.UTC).getYear() <= 9999) {
                            return instant;
                        }
                    } catch (DateTimeParseException ignored) {
                        // Invalid calendar dates must not be normalized silently.
                    }
                }
                return (Instant) context.handleUnexpectedToken(Instant.class, parser);
            }
        });
        return module;
    }
}
