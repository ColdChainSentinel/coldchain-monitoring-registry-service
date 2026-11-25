package pe.edu.upc.coldchain.dto;

import java.time.Instant;
import java.util.UUID;
import pe.edu.upc.coldchain.model.Device;

public record DeviceResponse(
        UUID id,
        String code,
        boolean active,
        String status,
        UUID monitoringPointId,
        String monitoringPointCode,
        Instant createdAt) {

    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getCode(),
                device.isActive(),
                device.isActive() ? "Activo" : "Inactivo",
                device.getMonitoringPoint().getId(),
                device.getMonitoringPoint().getCode(),
                device.getCreatedAt());
    }
}
