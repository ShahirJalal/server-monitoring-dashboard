package dev.shahirjalal.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shahirjalal.backend.config.SecurityConfig;
import dev.shahirjalal.backend.dto.ApplicationRequest;
import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.enums.Status;
import dev.shahirjalal.backend.exception.NotFoundException;
import dev.shahirjalal.backend.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplicationService applicationService;

    @Test
    void getAll_isPublic_noAuthRequired() throws Exception {

        when(applicationService.findAll()).thenReturn(List.of(
                ApplicationEntity.builder().id(1L).name("api").port(8080).status(Status.RUNNING).build()));

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("api"));
    }

    @Test
    void create_withoutAuth_isRejected() throws Exception {

        ApplicationRequest request = new ApplicationRequest();
        request.setName("api");
        request.setPort(8080);

        mockMvc.perform(post("/api/applications")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withoutCsrfToken_isRejected() throws Exception {

        ApplicationRequest request = new ApplicationRequest();
        request.setName("api");
        request.setPort(8080);

        mockMvc.perform(post("/api/applications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_authenticatedWithValidBody_returnsCreated() throws Exception {

        ApplicationRequest request = new ApplicationRequest();
        request.setName("api");
        request.setPort(8080);

        when(applicationService.save(any())).thenReturn(
                ApplicationEntity.builder().id(1L).name("api").port(8080).status(Status.UNKNOWN).build());

        mockMvc.perform(post("/api/applications")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("api"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankName_returnsBadRequest() throws Exception {

        ApplicationRequest request = new ApplicationRequest();
        request.setName("");
        request.setPort(8080);

        mockMvc.perform(post("/api/applications")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void getOne_missing_returnsNotFound() throws Exception {

        when(applicationService.findById(eq(404L))).thenThrow(new NotFoundException("Application not found: 404"));

        mockMvc.perform(get("/api/applications/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_authenticated_returnsNoContent() throws Exception {

        mockMvc.perform(delete("/api/applications/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
