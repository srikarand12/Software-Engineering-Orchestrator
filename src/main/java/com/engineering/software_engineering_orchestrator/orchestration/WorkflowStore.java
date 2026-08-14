package com.engineering.software_engineering_orchestrator.orchestration;

import com.engineering.software_engineering_orchestrator.orchestration.persistence.WorkflowExecutionEntity;
import com.engineering.software_engineering_orchestrator.orchestration.persistence.WorkflowExecutionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
public class WorkflowStore {

    private final WorkflowExecutionRepository repository;
    private final ObjectMapper objectMapper;

    public WorkflowStore(
            WorkflowExecutionRepository repository,
            ObjectMapper objectMapper) {

        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void save(EngineeringState state) {

        try {
            String stateJson =
                    objectMapper.writeValueAsString(state);

            WorkflowExecutionEntity entity =
                    repository.findById(state.getExecutionId())
                            .orElseGet(WorkflowExecutionEntity::new);

            entity.setExecutionId(state.getExecutionId());
            entity.setStatus(state.getStatus().name());
            entity.setRetryCount(state.getRetryCount());
            entity.setStateJson(stateJson);

            repository.save(entity);

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Unable to save workflow state",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public Optional<EngineeringState> findById(
            String executionId) {

        return repository.findById(executionId)
                .map(this::toEngineeringState);
    }

    private EngineeringState toEngineeringState(
            WorkflowExecutionEntity entity) {

        try {
            return objectMapper.readValue(
                    entity.getStateJson(),
                    EngineeringState.class
            );

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Unable to read workflow state",
                    exception
            );
        }
    }
}