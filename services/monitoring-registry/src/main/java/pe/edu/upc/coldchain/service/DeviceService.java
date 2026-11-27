package pe.edu.upc.coldchain.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.coldchain.dto.CreateDeviceRequest;
import pe.edu.upc.coldchain.dto.DeviceResponse;
import pe.edu.upc.coldchain.exception.DeviceNotFoundException;
import pe.edu.upc.coldchain.exception.DuplicateCodeException;
import pe.edu.upc.coldchain.exception.InactivePointException;
import pe.edu.upc.coldchain.exception.PointNotFoundException;
import pe.edu.upc.coldchain.model.Device;
import pe.edu.upc.coldchain.model.MonitoringPoint;
import pe.edu.upc.coldchain.repository.DeviceRepository;
import pe.edu.upc.coldchain.repository.PointRepository;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final PointRepository pointRepository;

    public DeviceService(DeviceRepository deviceRepository, PointRepository pointRepository) {
        this.deviceRepository = deviceRepository;
        this.pointRepository = pointRepository;
    }

    @Transactional
    public DeviceResponse create(CreateDeviceRequest request) {
        String code = request.code().trim();
        if (deviceRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateCodeException();
        }
        MonitoringPoint point = pointRepository.findById(request.monitoringPointId())
                .orElseThrow(PointNotFoundException::new);
        if (!point.isActive()) {
            throw new InactivePointException();
        }
        try {
            Device saved = deviceRepository.saveAndFlush(new Device(code, point, Instant.now()));
            return DeviceResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateCodeException();
        }
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> list(UUID monitoringPointId) {
        List<Device> devices = monitoringPointId == null
                ? deviceRepository.findAllByOrderByCodeAsc()
                : deviceRepository.findByMonitoringPointIdOrderByCodeAsc(monitoringPointId);
        return devices.stream().map(DeviceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponse find(UUID id) {
        return deviceRepository.findById(id)
                .map(DeviceResponse::from)
                .orElseThrow(DeviceNotFoundException::new);
    }
}
