package pe.edu.upc.coldchain.telemetry.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.coldchain.telemetry.dto.CreateMeasurementRequest;
import pe.edu.upc.coldchain.telemetry.dto.MeasurementResponse;
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;
import pe.edu.upc.coldchain.telemetry.model.Measurement;
import pe.edu.upc.coldchain.telemetry.repository.MeasurementRepository;

@Service
@Transactional(readOnly = true)
public class MeasurementStore {
    private final MeasurementRepository repository;

    public MeasurementStore(MeasurementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MeasurementResponse create(CreateMeasurementRequest request) {
        return MeasurementResponse.from(repository.saveAndFlush(new Measurement(request.monitoringPointId(),
                request.temperature().stripTrailingZeros(), request.timestamp().truncatedTo(ChronoUnit.MICROS))));
    }

    @Transactional
    public MeasurementResponse recordEvaluation(UUID id, EvaluationStatus status) {
        Measurement measurement = repository.findById(id).orElseThrow();
        measurement.recordEvaluation(status);
        return MeasurementResponse.from(repository.saveAndFlush(measurement));
    }

    public List<MeasurementResponse> list(UUID monitoringPointId) {
        return repository.findByMonitoringPointIdOrderByTimestampAscIdAsc(monitoringPointId)
                .stream().map(MeasurementResponse::from).toList();
    }
}
