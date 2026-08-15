package dev.shahirjalal.backend.service.impl;

import dev.shahirjalal.backend.dto.ApplicationRequest;
import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.entity.StatusEvent;
import dev.shahirjalal.backend.enums.Status;
import dev.shahirjalal.backend.exception.NotFoundException;
import dev.shahirjalal.backend.repository.ApplicationRepository;
import dev.shahirjalal.backend.repository.StatusEventRepository;
import dev.shahirjalal.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository repository;
    private final StatusEventRepository statusEventRepository;

    @Override
    public List<ApplicationEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public ApplicationEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found: " + id));
    }

    @Override
    public ApplicationEntity save(ApplicationRequest request) {

        ApplicationEntity application = ApplicationEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .port(request.getPort())
                .status(request.getStatus() != null ? request.getStatus() : Status.UNKNOWN)
                .build();

        return repository.save(application);
    }

    @Override
    public ApplicationEntity update(Long id, ApplicationRequest request) {

        ApplicationEntity existing = findById(id);

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPort(request.getPort());

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {

        if (!repository.existsById(id)) {
            throw new NotFoundException("Application not found: " + id);
        }

        repository.deleteById(id);
    }

    @Override
    public List<StatusEvent> getHistory(Long id) {
        return statusEventRepository.findTop20ByApplicationIdOrderByOccurredAtDesc(id);
    }
}
