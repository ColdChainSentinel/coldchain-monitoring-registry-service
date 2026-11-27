package pe.edu.upc.coldchain.alerts.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.coldchain.alerts.client.RegistryClient;
import pe.edu.upc.coldchain.alerts.config.ThermalRule;
import pe.edu.upc.coldchain.alerts.config.ThermalRulesProperties;
import pe.edu.upc.coldchain.alerts.dto.ConfigureThermalRuleRequest;
import pe.edu.upc.coldchain.alerts.dto.ThermalRuleResponse;
import pe.edu.upc.coldchain.alerts.model.ThermalRuleEntity;
import pe.edu.upc.coldchain.alerts.repository.ThermalRuleRepository;

@Service
public class ThermalRuleService {
    private static final String CELSIUS_UNIT = "°C";
    private final ThermalRuleRepository repository;
    private final RegistryClient registry;
    private final ThermalRulesProperties properties;
    private final Clock clock;

    public ThermalRuleService(ThermalRuleRepository repository, RegistryClient registry,
            ThermalRulesProperties properties, Clock clock) {
        this.repository = repository;
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ThermalRuleResponse configure(UUID monitoringPointId, ConfigureThermalRuleRequest request) {
        registry.requirePoint(monitoringPointId);
        Instant now = Instant.now(clock);
        ThermalRuleEntity entity = repository.findById(monitoringPointId)
                .map(existing -> {
                    existing.updateRange(request.thresholdMin(), request.thresholdMax(), now);
                    return existing;
                })
                .orElseGet(() -> new ThermalRuleEntity(
                        monitoringPointId, request.thresholdMin(), request.thresholdMax(), CELSIUS_UNIT, now));
        return ThermalRuleResponse.from(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<ThermalRuleResponse> find(UUID monitoringPointId) {
        return repository.findById(monitoringPointId)
                .map(ThermalRuleResponse::from)
                .or(() -> Optional.ofNullable(properties.rules().get(monitoringPointId))
                        .map(rule -> new ThermalRuleResponse(
                                monitoringPointId, rule.thresholdMin(), rule.thresholdMax(), CELSIUS_UNIT, null)));
    }

    @Transactional(readOnly = true)
    public Optional<ThermalRule> resolveForEvaluation(UUID monitoringPointId) {
        return repository.findById(monitoringPointId)
                .map(entity -> new ThermalRule(entity.getThresholdMin(), entity.getThresholdMax()))
                .or(() -> Optional.ofNullable(properties.rules().get(monitoringPointId)));
    }
}
