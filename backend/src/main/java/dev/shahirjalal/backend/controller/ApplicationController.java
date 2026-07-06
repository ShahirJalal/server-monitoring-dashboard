package dev.shahirjalal.backend.controller;

import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public List<ApplicationEntity> getAllApplications() {
        return applicationService.findAll();
    }

    @PostMapping
    public ApplicationEntity createApplication(@RequestBody ApplicationEntity application) {
        return applicationService.save(application);
    }

    @GetMapping("/{id}")
    public ApplicationEntity getApplication(@PathVariable Long id) {
        return applicationService.findById(id);
    }

    @PutMapping("/{id}")
    public ApplicationEntity updateApplication(
            @PathVariable Long id,
            @RequestBody ApplicationEntity application) {

        return applicationService.update(id, application);
    }

    @DeleteMapping("/{id}")
    public void deleteApplication(@PathVariable Long id) {
        applicationService.delete(id);
    }
}