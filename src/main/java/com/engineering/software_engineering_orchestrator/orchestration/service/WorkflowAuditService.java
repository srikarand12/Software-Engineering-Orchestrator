package com.engineering.software_engineering_orchestrator.orchestration.service;

import com.engineering.software_engineering_orchestrator.orchestration.persistence.WorkflowAuditEntity;
import com.engineering.software_engineering_orchestrator.orchestration.persistence.WorkflowAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowAuditService {

    private final WorkflowAuditRepository repository;

    public WorkflowAuditService(
            WorkflowAuditRepository repository) {

        this.repository = repository;
    }

    @Transactional
    public void record(
            String executionId,
            String eventType,
            String message) {

        WorkflowAuditEntity audit =
                new WorkflowAuditEntity(
                        executionId,
                        eventType,
                        message
                );

        repository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<WorkflowAuditEntity> getHistory(
            String executionId) {

        return repository
                .findByExecutionIdOrderByCreatedAtAsc(
                        executionId
                );
    }
}