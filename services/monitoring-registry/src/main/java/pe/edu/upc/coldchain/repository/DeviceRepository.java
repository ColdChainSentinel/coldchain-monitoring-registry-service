package pe.edu.upc.coldchain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.coldchain.model.Device;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    boolean existsByCodeIgnoreCase(String code);

    List<Device> findByMonitoringPointIdOrderByCodeAsc(UUID monitoringPointId);

    List<Device> findAllByOrderByCodeAsc();
}
