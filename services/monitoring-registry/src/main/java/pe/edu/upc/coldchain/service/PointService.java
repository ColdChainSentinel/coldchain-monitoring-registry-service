package pe.edu.upc.coldchain.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.coldchain.dto.CreatePointRequest;
import pe.edu.upc.coldchain.dto.PointResponse;
import pe.edu.upc.coldchain.exception.DuplicateCodeException;
import pe.edu.upc.coldchain.exception.PointNotFoundException;
import pe.edu.upc.coldchain.model.MonitoringPoint;
import pe.edu.upc.coldchain.repository.PointRepository;

@Service
@Transactional(readOnly = true)
public class PointService {
    private final PointRepository repository;

    public PointService(PointRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PointResponse create(CreatePointRequest request) {
        String code = request.code().toUpperCase(Locale.ROOT);
        if (repository.existsByCode(code)) {
            throw new DuplicateCodeException();
        }
        try {
            return PointResponse.from(repository.saveAndFlush(
                    new MonitoringPoint(code, request.name().strip(), request.type())));
        } catch (DataIntegrityViolationException exception) {
            // The database unique constraint also handles concurrent registrations.
            throw new DuplicateCodeException();
        }
    }

    public List<PointResponse> list() {
        return repository.findAll(Sort.by("code")).stream().map(PointResponse::from).toList();
    }

    public PointResponse find(UUID id) {
        return PointResponse.from(repository.findById(id).orElseThrow(PointNotFoundException::new));
    }
}
