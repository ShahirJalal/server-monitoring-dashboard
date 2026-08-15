package dev.shahirjalal.backend.repository;

import dev.shahirjalal.backend.entity.StatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusEventRepository extends JpaRepository<StatusEvent, Long> {

    List<StatusEvent> findTop20ByApplicationIdOrderByOccurredAtDesc(Long applicationId);
}
