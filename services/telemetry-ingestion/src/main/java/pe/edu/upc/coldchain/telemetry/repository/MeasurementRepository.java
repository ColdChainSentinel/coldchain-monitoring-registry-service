package pe.edu.upc.coldchain.telemetry.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.coldchain.telemetry.model.Measurement;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {
    List<Measurement> findByMonitoringPointIdOrderByTimestampAscIdAsc(UUID monitoringPointId);
}
