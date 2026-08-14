package com.engineering.software_engineering_orchestrator.orchestration.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowAuditRepository
        extends JpaRepository<WorkflowAuditEntity, Long> {

    List<WorkflowAuditEntity> findByExecutionIdOrderByCreatedAtAsc(
            String executionId
    );
}