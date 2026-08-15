package dev.shahirjalal.backend.service;

import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.entity.StatusEvent;
import dev.shahirjalal.backend.enums.Status;
import dev.shahirjalal.backend.repository.ApplicationRepository;
import dev.shahirjalal.backend.repository.StatusEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthCheckServiceTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private StatusEventRepository statusEventRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private PortProbe portProbe;

    private HealthCheckService service;

    @BeforeEach
    void setUp() {
        service = new HealthCheckService(repository, statusEventRepository, alertService, portProbe);
    }

    @Test
    void firstCheck_fromUnknown_recordsHistoryButDoesNotAlert() {

        ApplicationEntity app = ApplicationEntity.builder()
                .id(1L).name("api").port(8080).status(Status.UNKNOWN).build();
        when(repository.findAll()).thenReturn(List.of(app));
        when(portProbe.isOpen(any(), eq(8080), anyInt())).thenReturn(true);

        service.checkAll();

        assertThat(app.getStatus()).isEqualTo(Status.RUNNING);
        assertThat(app.getLastCheckedAt()).isNotNull();
        verify(statusEventRepository).save(any(StatusEvent.class));
        verify(alertService, never()).send(any());
    }

    @Test
    void statusFlips_afterKnownState_recordsAndAlerts() {

        ApplicationEntity app = ApplicationEntity.builder()
                .id(1L).name("api").port(8080).status(Status.RUNNING).build();
        when(repository.findAll()).thenReturn(List.of(app));
        when(portProbe.isOpen(any(), eq(8080), anyInt())).thenReturn(false);

        service.checkAll();

        assertThat(app.getStatus()).isEqualTo(Status.STOPPED);
        verify(statusEventRepository).save(any(StatusEvent.class));
        verify(alertService).send(contains("api"));
    }

    @Test
    void noChange_doesNotRecordOrAlert() {

        ApplicationEntity app = ApplicationEntity.builder()
                .id(1L).name("api").port(8080).status(Status.RUNNING).build();
        when(repository.findAll()).thenReturn(List.of(app));
        when(portProbe.isOpen(any(), eq(8080), anyInt())).thenReturn(true);

        service.checkAll();

        verify(statusEventRepository, never()).save(any());
        verify(alertService, never()).send(any());
    }
}
