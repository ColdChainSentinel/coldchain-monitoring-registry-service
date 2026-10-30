package pe.edu.upc.coldchain.alerts.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.coldchain.alerts.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByMonitoringPointIdOrderByCreatedAtAscIdAsc(UUID monitoringPointId);
}
