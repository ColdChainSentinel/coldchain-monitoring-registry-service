package pe.edu.upc.coldchain.model;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "monitoring_points")
public class MonitoringPoint {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointType type;

    protected MonitoringPoint() { }

    public MonitoringPoint(String code, String name, PointType type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public PointType getType() { return type; }
}
