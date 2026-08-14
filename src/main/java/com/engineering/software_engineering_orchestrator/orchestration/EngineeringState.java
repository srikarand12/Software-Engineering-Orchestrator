package com.engineering.software_engineering_orchestrator.orchestration;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class EngineeringState {

    private String executionId =
            UUID.randomUUID().toString();

    private String originalRequirement;
    private String normalizedRequirement;

    private ScenarioType scenarioType;

    private List<String> assumptions =
            new CopyOnWriteArrayList<>();

    private List<String> ambiguities =
            new CopyOnWriteArrayList<>();

    private List<String> acceptanceCriteria =
            new CopyOnWriteArrayList<>();

    private List<String> tasks =
            new CopyOnWriteArrayList<>();

    private List<String> risks =
            new CopyOnWriteArrayList<>();

    private List<String> validationResults =
            new CopyOnWriteArrayList<>();

    private List<String> decisionHistory =
            new CopyOnWriteArrayList<>();

    private List<String> guardrailChecks =
            new CopyOnWriteArrayList<>();

    private List<String> artifacts =
            new CopyOnWriteArrayList<>();

    private List<String> limitations =
            new CopyOnWriteArrayList<>();

    private WorkflowGraph workflowGraph;

    private WorkflowStatus status =
            WorkflowStatus.CREATED;

    private int retryCount;
    private int fallbackCount;
    private int rollbackCount;
    private int replanCount;
    private int failureCount;

    private boolean humanApprovalRequired;
    private boolean humanApproved;

    private boolean fallbackUsed;
    private boolean rolledBack;
    private boolean safeStopped;
    private boolean replanned;

    private Instant startedAt;
    private Instant finishedAt;
    private Instant recoveryStartedAt;

    private WorkflowMetrics metrics =
            new WorkflowMetrics();

    private String finalEngineeringSummary;

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

    public void addAssumption(String assumption) {

        if (assumption != null && !assumption.isBlank()) {
            assumptions.add(assumption);
        }
    }

    public void addAmbiguity(String ambiguity) {

        if (ambiguity != null && !ambiguity.isBlank()) {
            ambiguities.add(ambiguity);
        }
    }

    public void addAcceptanceCriterion(String criterion) {

        if (criterion != null && !criterion.isBlank()) {
            acceptanceCriteria.add(criterion);
        }
    }

    public void addTask(String task) {

        if (task != null && !task.isBlank()) {
            tasks.add(task);
        }
    }

    public void addGuardrailCheck(String check) {

        if (check != null && !check.isBlank()) {
            guardrailChecks.add(check);
        }
    }

    public void addArtifact(String artifact) {

        if (artifact != null && !artifact.isBlank()) {
            artifacts.add(artifact);
        }
    }

    public void addLimitation(String limitation) {

        if (limitation != null && !limitation.isBlank()) {
            limitations.add(limitation);
        }
    }
}