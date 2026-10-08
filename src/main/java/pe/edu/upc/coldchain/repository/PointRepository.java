package pe.edu.upc.coldchain.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.coldchain.model.MonitoringPoint;

public interface PointRepository extends JpaRepository<MonitoringPoint, UUID> {
    boolean existsByCode(String code);
}
