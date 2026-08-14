package com.engineering.software_engineering_orchestrator.orchestration;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class EngineeringState {

    private String executionId = UUID.randomUUID().toString();

    private String originalRequirement;
    private String normalizedRequirement;

    private List<String> assumptions = new CopyOnWriteArrayList<>();
    private List<String> ambiguities = new CopyOnWriteArrayList<>();
    private List<String> acceptanceCriteria = new CopyOnWriteArrayList<>();
    private List<String> tasks = new CopyOnWriteArrayList<>();
    private List<String> risks = new CopyOnWriteArrayList<>();
    private List<String> validationResults = new CopyOnWriteArrayList<>();
    private List<String> decisionHistory = new CopyOnWriteArrayList<>();

    private WorkflowGraph workflowGraph;

    private WorkflowStatus status = WorkflowStatus.CREATED;

    private int retryCount;
    private boolean humanApprovalRequired;
    private boolean humanApproved;

    public void addDecision(String decision) {
        if (decision != null && !decision.isBlank()) {
            decisionHistory.add(decision);
        }
    }

    public void addRisk(String risk) {
        if (risk != null && !risk.isBlank()) {
            risks.add(risk);
        }
    }

    public void addValidationResult(String result) {
        if (result != null && !result.isBlank()) {
            validationResults.add(result);
        }
    }
}