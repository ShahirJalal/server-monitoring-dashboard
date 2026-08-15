package dev.shahirjalal.backend.controller;

import dev.shahirjalal.backend.dto.ApplicationRequest;
import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/{id}")
    public ApplicationEntity getApplication(@PathVariable Long id) {
        return applicationService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationEntity createApplication(@Valid @RequestBody ApplicationRequest request) {
        return applicationService.save(request);
    }

    @PutMapping("/{id}")
    public ApplicationEntity updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRequest request) {

        return applicationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApplication(@PathVariable Long id) {
        applicationService.delete(id);
    }
}
