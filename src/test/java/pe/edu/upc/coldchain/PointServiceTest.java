package pe.edu.upc.coldchain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import pe.edu.upc.coldchain.dto.CreatePointRequest;
import pe.edu.upc.coldchain.exception.DuplicateCodeException;
import pe.edu.upc.coldchain.exception.PointNotFoundException;
import pe.edu.upc.coldchain.model.MonitoringPoint;
import pe.edu.upc.coldchain.model.PointType;
import pe.edu.upc.coldchain.repository.PointRepository;
import pe.edu.upc.coldchain.service.PointService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    @Mock
    private PointRepository repository;
    @InjectMocks
    private PointService service;

    @Test
    void createsAndNormalizesCodeAndName() {
        when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.create(new CreatePointRequest("cam-01", " Cámara 1 ", PointType.ALMACENAMIENTO));
        assertEquals("CAM-01", result.code());
        assertEquals("Cámara 1", result.name());
        assertEquals(PointType.ALMACENAMIENTO, result.type());
    }

    @Test
    void rejectsDuplicateWithoutSaving() {
        when(repository.existsByCode("CAM-01")).thenReturn(true);
        assertThrows(DuplicateCodeException.class, () -> service.create(
                new CreatePointRequest("cam-01", "Cámara", PointType.ALMACENAMIENTO)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void handlesConcurrentDuplicateAtDatabaseBoundary() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique code"));
        assertThrows(DuplicateCodeException.class, () -> service.create(
                new CreatePointRequest("CAM-01", "Cámara", PointType.ALMACENAMIENTO)));
    }

    @Test
    void listsPointsInCodeOrder() {
        when(repository.findAll(Sort.by("code"))).thenReturn(List.of(
                new MonitoringPoint("TR-01", "Camión", PointType.TRANSPORTE)));
        assertEquals("TR-01", service.list().get(0).code());
    }

    @Test
    void returnsExistingPoint() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(
                new MonitoringPoint("TR-01", "Camión", PointType.TRANSPORTE)));
        assertEquals("TR-01", service.find(id).code());
    }

    @Test
    void rejectsMissingPoint() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(PointNotFoundException.class, () -> service.find(id));
    }
}
