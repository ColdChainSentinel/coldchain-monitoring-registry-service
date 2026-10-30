package pe.edu.upc.coldchain.alerts.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.coldchain.alerts.model.EvaluationOutcome;
import pe.edu.upc.coldchain.alerts.model.MeasurementEvaluation;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(requiredProperties = {"measurementId", "outcome"})
public record EvaluationResponse(UUID measurementId, EvaluationOutcome outcome, AlertResponse alert) {
    public static EvaluationResponse from(MeasurementEvaluation evaluation) {
        return new EvaluationResponse(evaluation.getMeasurementId(), evaluation.getOutcome(),
                evaluation.getAlert() == null ? null : AlertResponse.from(evaluation.getAlert()));
    }
}
