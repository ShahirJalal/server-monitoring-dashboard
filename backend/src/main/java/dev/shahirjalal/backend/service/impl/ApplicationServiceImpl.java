package dev.shahirjalal.backend.service.impl;

import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.repository.ApplicationRepository;
import dev.shahirjalal.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository repository;

    @Override
    public List<ApplicationEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public ApplicationEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    @Override
    public ApplicationEntity save(ApplicationEntity application) {
        return repository.save(application);
    }

    @Override
    public ApplicationEntity update(Long id, ApplicationEntity application) {

        ApplicationEntity existing = findById(id);

        existing.setName(application.getName());
        existing.setDescription(application.getDescription());
        existing.setPort(application.getPort());
        existing.setStatus(application.getStatus());

        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}