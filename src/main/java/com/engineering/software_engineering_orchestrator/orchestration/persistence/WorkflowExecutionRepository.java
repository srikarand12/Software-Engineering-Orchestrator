package com.engineering.software_engineering_orchestrator.orchestration.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowExecutionRepository
        extends JpaRepository<WorkflowExecutionEntity, String> {
}