package dev.shahirjalal.backend.service;

import dev.shahirjalal.backend.dto.ApplicationRequest;
import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.enums.Status;
import dev.shahirjalal.backend.exception.NotFoundException;
import dev.shahirjalal.backend.repository.ApplicationRepository;
import dev.shahirjalal.backend.repository.StatusEventRepository;
import dev.shahirjalal.backend.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private StatusEventRepository statusEventRepository;

    private ApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApplicationServiceImpl(repository, statusEventRepository);
    }

    @Test
    void findAll_returnsEverythingFromRepository() {

        List<ApplicationEntity> apps = List.of(
                ApplicationEntity.builder().id(1L).name("a").port(80).status(Status.RUNNING).build());
        when(repository.findAll()).thenReturn(apps);

        assertThat(service.findAll()).isEqualTo(apps);
    }

    @Test
    void findById_missing_throwsNotFound() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void save_defaultsStatusToUnknown_whenNotProvided() {

        ApplicationRequest request = new ApplicationRequest();
        request.setName("api");
        request.setDescription("desc");
        request.setPort(8080);

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationEntity saved = service.save(request);

        assertThat(saved.getName()).isEqualTo("api");
        assertThat(saved.getPort()).isEqualTo(8080);
        assertThat(saved.getStatus()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    void update_missing_throwsNotFound_andNeverSaves() {

        when(repository.findById(5L)).thenReturn(Optional.empty());

        ApplicationRequest request = new ApplicationRequest();
        request.setName("x");
        request.setPort(1);

        assertThatThrownBy(() -> service.update(5L, request)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_overwritesFieldsOnExistingEntity() {

        ApplicationEntity existing = ApplicationEntity.builder()
                .id(1L).name("old").description("old-desc").port(1).status(Status.RUNNING).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRequest request = new ApplicationRequest();
        request.setName("new");
        request.setDescription("new-desc");
        request.setPort(2);

        ApplicationEntity result = service.update(1L, request);

        assertThat(result.getName()).isEqualTo("new");
        assertThat(result.getDescription()).isEqualTo("new-desc");
        assertThat(result.getPort()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(Status.RUNNING);
    }

    @Test
    void delete_missing_throwsNotFound_andNeverDeletes() {

        when(repository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void delete_existing_deletesById() {

        when(repository.existsById(7L)).thenReturn(true);

        service.delete(7L);

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(repository).deleteById(idCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(7L);
    }
}
