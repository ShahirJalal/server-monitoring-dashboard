package dev.shahirjalal.backend.service;

import dev.shahirjalal.backend.entity.ApplicationEntity;

import java.util.List;

public interface ApplicationService {

    List<ApplicationEntity> findAll();

    ApplicationEntity findById(Long id);

    ApplicationEntity save(ApplicationEntity application);

    ApplicationEntity update(Long id, ApplicationEntity application);

    void delete(Long id);
}