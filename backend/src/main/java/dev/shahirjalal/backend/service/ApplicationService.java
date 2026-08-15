package dev.shahirjalal.backend.service;

import dev.shahirjalal.backend.dto.ApplicationRequest;
import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.entity.StatusEvent;

import java.util.List;

public interface ApplicationService {

    List<ApplicationEntity> findAll();

    ApplicationEntity findById(Long id);

    ApplicationEntity save(ApplicationRequest request);

    ApplicationEntity update(Long id, ApplicationRequest request);

    void delete(Long id);

    List<StatusEvent> getHistory(Long id);
}
