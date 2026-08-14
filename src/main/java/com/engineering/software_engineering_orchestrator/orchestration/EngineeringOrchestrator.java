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

        workflowAuditService.record(
                state.getExecutionId(),
                "WORKFLOW_STARTED",
                "Workflow started"
        );

        return runWorkflow(state);
    }

    public EngineeringState retry(String executionId) {

        EngineeringState previousState = workflowStore.findById(executionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Workflow not found")
                );

        if (previousState.getStatus() != WorkflowStatus.FAILED
                && previousState.getStatus() != WorkflowStatus.REJECTED) {

            throw new IllegalStateException(
                    "Only failed or rejected workflows can be retried"
            );
        }

        if (previousState.getRetryCount() >= MAX_RETRIES) {
            throw new IllegalStateException(
                    "Maximum retry limit reached"
            );
        }

        EngineeringState retryState = new EngineeringState();

        retryState.setOriginalRequirement(
                previousState.getOriginalRequirement()
        );

        retryState.setRetryCount(
                previousState.getRetryCount() + 1
        );

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

        EngineeringState state = workflowStore.findById(executionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Workflow not found")
                );

        if (state.getStatus() == WorkflowStatus.COMPLETED
                || state.getStatus() == WorkflowStatus.REJECTED) {

            throw new IllegalStateException(
                    "Completed or rejected workflows cannot be resumed"
            );
        }

        if (state.getStatus() == WorkflowStatus.WAITING_FOR_APPROVAL) {
            return state;
        }

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

    private EngineeringState runWorkflow(EngineeringState state) {

        state.setStatus(WorkflowStatus.RUNNING);
        state.setHumanApprovalRequired(false);
        state.setHumanApproved(false);

        if (!isCompleted(state, "requirements")) {

            AgentResult result = requirementAgent.execute(state);

            state.addDecision(
                    "Requirement analysis: " + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }
        }

        if (!isCompleted(state, "planning")) {

            AgentResult result = plannerAgent.execute(state);

            state.addDecision(
                    "Planning: " + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "requirements");
            markCompleted(state, "planning");
        }

        if (!isCompleted(state, "architecture")) {

            AgentResult result = architectureAgent.execute(state);

            state.addDecision(
                    "Architecture: " + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "architecture");
        }

        if (!isCompleted(state, "implementation")) {

            AgentResult result = developerAgent.execute(state);

            state.addDecision(
                    "Implementation: " + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "implementation");
        }

        if (!runParallelChecks(state)) {
            return state;
        }

        if (!isCompleted(state, "validation")) {

            AgentResult result = validationAgent.execute(state);

            state.addDecision(
                    "Validation: " + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "validation");
        }

        if (!isCompleted(state, "documentation")) {

            AgentResult result = documentationAgent.execute(state);

            state.addDecision(
                    "Documentation: " + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }

            markCompleted(state, "documentation");
        }

        if (!isCompleted(state, "release")) {

            AgentResult result = releaseAgent.execute(state);

            state.addDecision(
                    "Release: " + result.message()
            );

            if (!result.success()) {
                return fail(state);
            }
        }

        state.setStatus(
                WorkflowStatus.WAITING_FOR_APPROVAL
        );

        state.setHumanApprovalRequired(true);

        workflowStore.save(state);

        workflowAuditService.record(
                state.getExecutionId(),
                "WAITING_FOR_APPROVAL",
                "Workflow is waiting for human approval"
        );

        return state;
    }

    private boolean runParallelChecks(EngineeringState state) {

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

            String failureMessage = determineParallelFailure(
                    testingFuture,
                    securityFuture
            );

            testingFuture.cancel(true);
            securityFuture.cancel(true);

            state.addDecision(failureMessage);

            workflowAuditService.record(
                    state.getExecutionId(),
                    "PARALLEL_EXECUTION_FAILED",
                    failureMessage
            );

            fail(state);

            return false;
        }

        if (testingRequired) {

            state.addDecision(
                    "Testing: " + testResult.message()
            );

            if (!testResult.success()) {
                fail(state);
                return false;
            }

            markCompleted(state, "testing");
        }

        if (securityRequired) {

            state.addDecision(
                    "Security: " + securityResult.message()
            );

            if (!securityResult.success()) {
                fail(state);
                return false;
            }

            markCompleted(state, "security");
        }

        return true;
    }

    private EngineeringState fail(EngineeringState state) {

        state.setStatus(WorkflowStatus.FAILED);

        workflowStore.save(state);

        workflowAuditService.record(
                state.getExecutionId(),
                "WORKFLOW_FAILED",
                "Workflow execution failed"
        );

        return state;
    }

    private boolean isCompleted(
            EngineeringState state,
            String nodeId) {

        if (state.getWorkflowGraph() == null) {

            System.out.println(
                    "Resume check - graph is null for node: " + nodeId
            );

            return false;
        }

        return state.getWorkflowGraph()
                .findNode(nodeId)
                .map(node -> {

                    boolean completed =
                            node.getStatus()
                                    == WorkflowStatus.COMPLETED;

                    System.out.println(
                            "Resume check - node="
                                    + nodeId
                                    + ", status="
                                    + node.getStatus()
                                    + ", completed="
                                    + completed
                    );

                    return completed;
                })
                .orElseGet(() -> {

                    System.out.println(
                            "Resume check - node not found: " + nodeId
                    );

                    return false;
                });
    }

    private String determineParallelFailure(
            CompletableFuture<AgentResult> testingFuture,
            CompletableFuture<AgentResult> securityFuture) {

        if (testingFuture.isCompletedExceptionally()) {
            return "Testing failed or timed out";
        }

        if (securityFuture.isCompletedExceptionally()) {
            return "Security review failed or timed out";
        }

        return "Testing or security review failed";
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
}