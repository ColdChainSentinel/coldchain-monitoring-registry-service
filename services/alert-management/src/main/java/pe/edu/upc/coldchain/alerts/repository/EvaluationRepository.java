package pe.edu.upc.coldchain.alerts.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.coldchain.alerts.model.MeasurementEvaluation;

public interface EvaluationRepository extends JpaRepository<MeasurementEvaluation, UUID> { }
