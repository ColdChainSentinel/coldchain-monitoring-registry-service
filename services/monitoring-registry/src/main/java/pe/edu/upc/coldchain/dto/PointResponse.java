package pe.edu.upc.coldchain.dto;

import java.util.UUID;
import pe.edu.upc.coldchain.model.MonitoringPoint;
import pe.edu.upc.coldchain.model.PointType;

public record PointResponse(UUID id, String code, String name, PointType type) {
    public static PointResponse from(MonitoringPoint point) {
        return new PointResponse(point.getId(), point.getCode(), point.getName(), point.getType());
    }
}
