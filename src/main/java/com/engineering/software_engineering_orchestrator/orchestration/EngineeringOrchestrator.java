package com.engineering.software_engineering_orchestrator.orchestration;

import com.engineering.software_engineering_orchestrator.agent.AgentResult;
import com.engineering.software_engineering_orchestrator.agent.ArchitectureAgent;
import com.engineering.software_engineering_orchestrator.agent.DeveloperAgent;
import com.engineering.software_engineering_orchestrator.agent.DocumentationAgent;
import com.engineering.software_engineering_orchestrator.agent.PlannerAgent;
import com.engineering.software_engineering_orchestrator.agent.ReleaseAgent;
import com.engineering.software_engineering_orchestrator.agent.RequirementAgent;
import com.engineering.software_engineering_orchestrator.agent.SecurityAgent;
import com.engineering.software_engineering_orchestrator.agent.TestAgent;
import com.engineering.software_engineering_orchestrator.agent.ValidationAgent;
import com.engineering.software_engineering_orchestrator.orchestration.service.WorkflowAuditService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class EngineeringOrchestrator {

    private static final int MAX_RETRIES = 3;
    private static final int PARALLEL_AGENT_TIMEOUT_SECONDS = 10;

    private final RequirementAgent requirementAgent;
    private final PlannerAgent plannerAgent;
    private final ArchitectureAgent architectureAgent;
    private final DeveloperAgent developerAgent;
    private final TestAgent testAgent;
    private final SecurityAgent securityAgent;
    private final ValidationAgent validationAgent;
    private final DocumentationAgent documentationAgent;
    private final ReleaseAgent releaseAgent;
    private final WorkflowStore workflowStore;
    private final WorkflowAuditService workflowAuditService;
    private final ExecutorService workflowExecutor;

    public EngineeringOrchestrator(
            RequirementAgent requirementAgent,
            PlannerAgent plannerAgent,
            ArchitectureAgent architectureAgent,
            DeveloperAgent developerAgent,
            TestAgent testAgent,
            SecurityAgent securityAgent,
            ValidationAgent validationAgent,
            DocumentationAgent documentationAgent,
            ReleaseAgent releaseAgent,
            WorkflowStore workflowStore,
            WorkflowAuditService workflowAuditService,
            ExecutorService workflowExecutor) {

        this.requirementAgent = requirementAgent;
        this.plannerAgent = plannerAgent;
        this.architectureAgent = architectureAgent;
        this.developerAgent = developerAgent;
        this.testAgent = testAgent;
        this.securityAgent = securityAgent;
        this.validationAgent = validationAgent;
        this.documentationAgent = documentationAgent;
        this.releaseAgent = releaseAgent;
        this.workflowStore = workflowStore;
        this.workflowAuditService = workflowAuditService;
        this.workflowExecutor = workflowExecutor;
    }

    public EngineeringState execute(String requirement) {

        EngineeringState state = new EngineeringState();

        state.setOriginalRequirement(requirement);
        state.setStartedAt(Instant.now());

        workflowAuditService.record(
                state.getExecutionId(),
                "WORKFLOW_STARTED",
                "Workflow started"
        );

        return runWorkflow(state);
    }

    public EngineeringState retry(String executionId) {

        EngineeringState previous =
                workflowStore.findById(executionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Workflow not found"
                                )
                        );

        if (previous.getStatus() != WorkflowStatus.FAILED
                && previous.getStatus() != WorkflowStatus.REJECTED
                && previous.getStatus() != WorkflowStatus.SAFE_STOPPED) {

            throw new IllegalStateException(
                    "Only failed, rejected, or safe-stopped workflows can be retried"
            );
        }

        if (previous.getRetryCount() >= MAX_RETRIES) {
            throw new IllegalStateException(
                    "Maximum retry limit reached"
            );
        }

        EngineeringState retryState =
                new EngineeringState();

        retryState.setOriginalRequirement(
                previous.getOriginalRequirement()
        );

        retryState.setRetryCount(
                previous.getRetryCount() + 1
        );

        retryState.setStartedAt(Instant.now());
        retryState.setRecoveryStartedAt(Instant.now());

        retryState.addDecision(
                "Workflow retried from execution " + executionId
        );

        workflowAuditService.record(
                retryState.getExecutionId(),
                "WORKFLOW_RETRIED",
                "Workflow retried from execution " + executionId
        );

        return runWorkflow(retryState);
    }

    public EngineeringState resume(String executionId) {

        EngineeringState state =
                getState(executionId);

        if (state.getStatus() == WorkflowStatus.COMPLETED
                || state.getStatus() == WorkflowStatus.REJECTED
                || state.getStatus() == WorkflowStatus.ROLLED_BACK) {

            throw new IllegalStateException(
                    "Workflow cannot be resumed"
            );
        }

        if (state.getStatus()
                == WorkflowStatus.WAITING_FOR_APPROVAL) {

            return state;
        }

        state.setSafeStopped(false);

        state.addDecision(
                "Workflow resumed from persisted state"
        );

        workflowAuditService.record(
                state.getExecutionId(),
                "WORKFLOW_RESUMED",
                "Workflow resumed from persisted state"
        );

        return runWorkflow(state);
    }

    public EngineeringState safeStop(String executionId) {

        EngineeringState state =
                getState(executionId);

        if (state.getStatus() == WorkflowStatus.COMPLETED
                || state.getStatus() == WorkflowStatus.ROLLED_BACK) {

            throw new IllegalStateException(
                    "Workflow can no longer be stopped"
            );
        }

        state.setSafeStopped(true);
        state.setHumanApprovalRequired(false);
        state.setStatus(WorkflowStatus.SAFE_STOPPED);

        state.addDecision(
                "Workflow safely stopped"
        );

        updateMetrics(state);
        workflowStore.save(state);

        workflowAuditService.record(
                executionId,
                "WORKFLOW_SAFE_STOPPED",
                "Workflow safely stopped"
        );

        return state;
    }

    public EngineeringState rollback(String executionId) {

        EngineeringState state =
                getState(executionId);

        if (state.getStatus() == WorkflowStatus.ROLLED_BACK) {
            return state;
        }

        if (state.getStatus() == WorkflowStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed workflow cannot be rolled back"
            );
        }

        state.setRollbackCount(
                state.getRollbackCount() + 1
        );

        state.setRolledBack(true);
        state.setHumanApproved(false);
        state.setHumanApprovalRequired(false);
        state.setStatus(WorkflowStatus.ROLLED_BACK);

        if (state.getWorkflowGraph() != null) {
            state.getWorkflowGraph()
                    .findNode("release")
                    .ifPresent(node ->
                            node.setStatus(
                                    WorkflowStatus.CREATED
                            )
                    );
        }

        state.addDecision(
                "Workflow rolled back before release"
        );

        updateMetrics(state);
        workflowStore.save(state);

        workflowAuditService.record(
                executionId,
                "WORKFLOW_ROLLED_BACK",
                "Workflow rolled back before release"
        );

        return state;
    }

    public EngineeringState replan(
            String executionId,
            String updatedRequirement) {

        if (updatedRequirement == null
                || updatedRequirement.isBlank()) {

            throw new IllegalArgumentException(
                    "Updated requirement is required"
            );
        }

        EngineeringState state =
                getState(executionId);

        if (state.getStatus() == WorkflowStatus.COMPLETED
                || state.getStatus() == WorkflowStatus.ROLLED_BACK) {

            throw new IllegalStateException(
                    "Workflow can no longer be re-planned"
            );
        }

        String previousRequirement =
                state.getOriginalRequirement();

        state.setOriginalRequirement(
                updatedRequirement
        );

        state.setNormalizedRequirement(null);
        state.setScenarioType(null);

        state.getAssumptions().clear();
        state.getAmbiguities().clear();
        state.getAcceptanceCriteria().clear();
        state.getTasks().clear();
        state.getRisks().clear();
        state.getValidationResults().clear();
        state.getGuardrailChecks().clear();
        state.getArtifacts().clear();
        state.getLimitations().clear();

        state.setWorkflowGraph(null);

        state.setReplanCount(
                state.getReplanCount() + 1
        );

        state.setReplanned(true);
        state.setHumanApproved(false);
        state.setHumanApprovalRequired(false);

        state.addDecision(
                "Requirement changed from '"
                        + previousRequirement
                        + "' to '"
                        + updatedRequirement
                        + "'"
        );

        state.addDecision(
                "Workflow plan reset because an upstream requirement changed"
        );

        workflowAuditService.record(
                executionId,
                "WORKFLOW_REPLANNED",
                "Workflow re-planned after requirement change"
        );

        return runWorkflow(state);
    }

    private EngineeringState runWorkflow(
            EngineeringState state) {

        if (state.getStartedAt() == null) {
            state.setStartedAt(Instant.now());
        }

        state.setStatus(WorkflowStatus.RUNNING);
        state.setHumanApprovalRequired(false);
        state.setHumanApproved(false);

        if (!isCompleted(state, "requirements")) {

            AgentResult result =
                    requirementAgent.execute(state);

            state.addDecision(
                    "Requirement analysis: "
                            + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }
        }

        if (!isCompleted(state, "planning")) {

            AgentResult result =
                    plannerAgent.execute(state);

            state.addDecision(
                    "Planning: " + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "requirements");
            markCompleted(state, "planning");
        }

        applyGuardrails(state);

        if (!guardrailsPassed(state)) {
            return safeStopInternal(
                    state,
                    "Policy guardrail check failed"
            );
        }

        if (!isCompleted(state, "architecture")) {

            AgentResult result =
                    architectureAgent.execute(state);

            state.addDecision(
                    "Architecture: "
                            + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "architecture");
        }

        if (!isCompleted(state, "implementation")) {

            AgentResult result =
                    developerAgent.execute(state);

            state.addDecision(
                    "Implementation: "
                            + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "implementation");
        }

        if (!runParallelChecks(state)) {
            return state;
        }

        if (!validationGatePassed(state)) {

            return safeStopInternal(
                    state,
                    "Validation gate blocked because testing or security review is incomplete"
            );
        }

        if (!isCompleted(state, "validation")) {

            AgentResult result =
                    validationAgent.execute(state);

            state.addDecision(
                    "Validation: "
                            + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "validation");
        }

        if (!isCompleted(state, "documentation")) {

            AgentResult result =
                    documentationAgent.execute(state);

            state.addDecision(
                    "Documentation: "
                            + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "documentation");
        }

        if (!releaseGatePassed(state)) {

            return safeStopInternal(
                    state,
                    "Release gate blocked because required stages are incomplete"
            );
        }

        if (!isCompleted(state, "release")) {

            AgentResult result =
                    releaseAgent.execute(state);

            state.addDecision(
                    "Release: "
                            + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }
        }

        state.setStatus(
                WorkflowStatus.WAITING_FOR_APPROVAL
        );

        state.setHumanApprovalRequired(true);

        buildEngineeringSummary(state);
        updateMetrics(state);

        workflowStore.save(state);

        workflowAuditService.record(
                state.getExecutionId(),
                "WAITING_FOR_APPROVAL",
                "Workflow is waiting for human approval"
        );

        return state;
    }

    private boolean runParallelChecks(
            EngineeringState state) {

        boolean testingRequired =
                !isCompleted(state, "testing");

        boolean securityRequired =
                !isCompleted(state, "security");

        if (!testingRequired && !securityRequired) {
            return true;
        }

        CompletableFuture<AgentResult> testingFuture =
                testingRequired
                        ? CompletableFuture.supplyAsync(
                        () -> testAgent.execute(state),
                        workflowExecutor
                ).orTimeout(
                        PARALLEL_AGENT_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                )
                        : CompletableFuture.completedFuture(
                        AgentResult.success(
                                "Testing already completed"
                        )
                );

        CompletableFuture<AgentResult> securityFuture =
                securityRequired
                        ? CompletableFuture.supplyAsync(
                        () -> securityAgent.execute(state),
                        workflowExecutor
                ).orTimeout(
                        PARALLEL_AGENT_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                )
                        : CompletableFuture.completedFuture(
                        AgentResult.success(
                                "Security review already completed"
                        )
                );

        AgentResult testResult;
        AgentResult securityResult;

        try {

            CompletableFuture.allOf(
                    testingFuture,
                    securityFuture
            ).join();

            testResult = testingFuture.join();
            securityResult = securityFuture.join();

        } catch (RuntimeException exception) {

            testingFuture.cancel(true);
            securityFuture.cancel(true);

            state.addDecision(
                    "Parallel checks did not complete successfully"
            );

            workflowAuditService.record(
                    state.getExecutionId(),
                    "PARALLEL_EXECUTION_FAILED",
                    "Parallel testing or security execution failed"
            );

            return runFallbackChecks(state);
        }

        if (testingRequired) {

            state.addDecision(
                    "Testing: " + testResult.message()
            );

            if (!testResult.success()) {
                return runFallbackChecks(state);
            }

            markCompleted(state, "testing");
        }

        if (securityRequired) {

            state.addDecision(
                    "Security: "
                            + securityResult.message()
            );

            if (!securityResult.success()) {
                return runFallbackChecks(state);
            }

            markCompleted(state, "security");
        }

        return true;
    }

    private boolean runFallbackChecks(
            EngineeringState state) {

        state.setFallbackUsed(true);

        state.setFallbackCount(
                state.getFallbackCount() + 1
        );

        state.addDecision(
                "Fallback activated: testing and security checks are running sequentially"
        );

        workflowAuditService.record(
                state.getExecutionId(),
                "FALLBACK_ACTIVATED",
                "Sequential fallback started"
        );

        try {

            if (!isCompleted(state, "testing")) {

                AgentResult testResult =
                        testAgent.execute(state);

                if (!testResult.success()) {
                    fail(state);
                    return false;
                }

                state.addDecision(
                        "Fallback testing: "
                                + testResult.message()
                );

                markCompleted(state, "testing");
            }

            if (!isCompleted(state, "security")) {

                AgentResult securityResult =
                        securityAgent.execute(state);

                if (!securityResult.success()) {
                    fail(state);
                    return false;
                }

                state.addDecision(
                        "Fallback security: "
                                + securityResult.message()
                );

                markCompleted(state, "security");
            }

            workflowAuditService.record(
                    state.getExecutionId(),
                    "FALLBACK_COMPLETED",
                    "Sequential fallback completed"
            );

            return true;

        } catch (RuntimeException exception) {

            safeStopInternal(
                    state,
                    "Fallback execution failed"
            );

            return false;
        }
    }

    private void applyGuardrails(
            EngineeringState state) {

        state.addGuardrailCheck(
                "Security review is required before validation"
        );

        state.addGuardrailCheck(
                "Testing must complete before validation"
        );

        state.addGuardrailCheck(
                "Documentation must complete before release"
        );

        state.addGuardrailCheck(
                "Release requires explicit human approval"
        );

        state.addGuardrailCheck(
                "Retries are limited to " + MAX_RETRIES
        );

        if (state.getScenarioType()
                == ScenarioType.BROWNFIELD) {

            state.addGuardrailCheck(
                    "Existing API behavior must remain backward compatible"
            );
        }

        if (state.getScenarioType()
                == ScenarioType.AMBIGUOUS) {

            state.addGuardrailCheck(
                    "Ambiguous requirements are limited to low-risk changes until reviewed"
            );

            state.addLimitation(
                    "The ambiguous requirement requires human clarification before high-impact changes"
            );
        }

        workflowAuditService.record(
                state.getExecutionId(),
                "GUARDRAILS_CHECKED",
                "Security, change-control, and approval guardrails applied"
        );
    }

    private boolean guardrailsPassed(
            EngineeringState state) {

        return state.getNormalizedRequirement() != null
                && !state.getNormalizedRequirement().isBlank();
    }

    private boolean validationGatePassed(
            EngineeringState state) {

        return isCompleted(state, "testing")
                && isCompleted(state, "security");
    }

    private boolean releaseGatePassed(
            EngineeringState state) {

        return isCompleted(state, "validation")
                && isCompleted(state, "documentation");
    }

    private EngineeringState safeStopInternal(
            EngineeringState state,
            String reason) {

        state.setSafeStopped(true);
        state.setStatus(
                WorkflowStatus.SAFE_STOPPED
        );

        state.setHumanApprovalRequired(false);

        state.addDecision(
                "Workflow safely stopped: " + reason
        );

        updateMetrics(state);
        workflowStore.save(state);

        workflowAuditService.record(
                state.getExecutionId(),
                "WORKFLOW_SAFE_STOPPED",
                reason
        );

        return state;
    }

    private EngineeringState fail(
            EngineeringState state) {

        state.setFailureCount(
                state.getFailureCount() + 1
        );

        state.setStatus(
                WorkflowStatus.FAILED
        );

        if (state.getRecoveryStartedAt() == null) {
            state.setRecoveryStartedAt(
                    Instant.now()
            );
        }

        updateMetrics(state);
        workflowStore.save(state);

        workflowAuditService.record(
                state.getExecutionId(),
                "WORKFLOW_FAILED",
                "Workflow execution failed"
        );

        return state;
    }

    private EngineeringState getState(
            String executionId) {

        return workflowStore.findById(executionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Workflow not found"
                        )
                );
    }

    private boolean isCompleted(
            EngineeringState state,
            String nodeId) {

        if (state.getWorkflowGraph() == null) {
            return false;
        }

        return state.getWorkflowGraph()
                .findNode(nodeId)
                .map(node ->
                        node.getStatus()
                                == WorkflowStatus.COMPLETED
                )
                .orElse(false);
    }

    private void markCompleted(
            EngineeringState state,
            String nodeId) {

        if (state.getWorkflowGraph() == null) {
            return;
        }

        state.getWorkflowGraph()
                .findNode(nodeId)
                .ifPresent(node ->
                        node.setStatus(
                                WorkflowStatus.COMPLETED
                        )
                );
    }

    private void updateMetrics(
            EngineeringState state) {

        WorkflowMetrics metrics =
                state.getMetrics();

        if (metrics == null) {
            metrics = new WorkflowMetrics();
            state.setMetrics(metrics);
        }

        metrics.setRetryCount(
                state.getRetryCount()
        );

        metrics.setFallbackCount(
                state.getFallbackCount()
        );

        metrics.setRollbackCount(
                state.getRollbackCount()
        );

        metrics.setReplanCount(
                state.getReplanCount()
        );

        metrics.setFailureCount(
                state.getFailureCount()
        );

        if (state.getStartedAt() != null) {

            metrics.setEndToEndLatencyMs(
                    Duration.between(
                            state.getStartedAt(),
                            Instant.now()
                    ).toMillis()
            );
        }

        if (state.getRecoveryStartedAt() != null
                && state.getStatus()
                != WorkflowStatus.FAILED) {

            metrics.setMttrMs(
                    Duration.between(
                            state.getRecoveryStartedAt(),
                            Instant.now()
                    ).toMillis()
            );
        }

        if (state.getStatus()
                == WorkflowStatus.WAITING_FOR_APPROVAL
                || state.getStatus()
                == WorkflowStatus.COMPLETED) {

            metrics.setSuccessRate(100.0);

        } else if (state.getStatus()
                == WorkflowStatus.FAILED) {

            metrics.setSuccessRate(0.0);
        }
    }

    private void buildEngineeringSummary(
            EngineeringState state) {

        state.addArtifact(
                "Requirement analysis and normalized engineering problem"
        );

        state.addArtifact(
                "Workflow dependency graph"
        );

        state.addArtifact(
                "Implementation output"
        );

        state.addArtifact(
                "Testing and security validation results"
        );

        state.addArtifact(
                "API and supporting documentation"
        );

        if (state.getLimitations().isEmpty()) {

            state.addLimitation(
                    "The prototype uses an in-process orchestration model and H2 persistence"
            );
        }

        String summary =
                "Scenario: " + state.getScenarioType()
                        + ". Plan: the requirement was decomposed into "
                        + state.getTasks().size()
                        + " engineering tasks and executed through the workflow graph. "
                        + "Validation: "
                        + state.getValidationResults().size()
                        + " validation results were recorded. "
                        + "Risks: "
                        + state.getRisks().size()
                        + " risks were identified. "
                        + "Assumptions: "
                        + state.getAssumptions().size()
                        + " assumptions were recorded. "
                        + "Artifacts: "
                        + state.getArtifacts().size()
                        + " engineering artifacts were produced. "
                        + "Limitations: "
                        + state.getLimitations().size()
                        + " limitations were documented.";

        state.setFinalEngineeringSummary(
                summary
        );
    }
}