package com.engineering.software_engineering_orchestrator;

import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import com.engineering.software_engineering_orchestrator.orchestration.ScenarioType;
import com.engineering.software_engineering_orchestrator.orchestration.WorkflowStatus;
import com.engineering.software_engineering_orchestrator.orchestration.controller.OrchestrationController;
import com.engineering.software_engineering_orchestrator.orchestration.persistence.WorkflowAuditEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrchestrationControllerIntegrationTest {

    @Autowired
    private OrchestrationController controller;

    @Test
    void shouldCreateAndApproveWorkflow() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        assertNotNull(created);
        assertNotNull(created.getExecutionId());

        assertEquals(
                WorkflowStatus.WAITING_FOR_APPROVAL,
                created.getStatus()
        );

        EngineeringState approved =
                controller.approveWorkflow(
                                created.getExecutionId()
                        )
                        .getBody();

        assertNotNull(approved);

        assertEquals(
                WorkflowStatus.COMPLETED,
                approved.getStatus()
        );

        assertTrue(approved.isHumanApproved());
        assertFalse(approved.isHumanApprovalRequired());
    }

    @Test
    void shouldCreateAndRejectWorkflow() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        EngineeringState rejected =
                controller.rejectWorkflow(
                                created.getExecutionId()
                        )
                        .getBody();

        assertNotNull(rejected);

        assertEquals(
                WorkflowStatus.REJECTED,
                rejected.getStatus()
        );

        assertFalse(rejected.isHumanApproved());
        assertFalse(rejected.isHumanApprovalRequired());
    }

    @Test
    void shouldRetryRejectedWorkflow() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        EngineeringState rejected =
                controller.rejectWorkflow(
                                created.getExecutionId()
                        )
                        .getBody();

        assertNotNull(rejected);

        EngineeringState retried =
                controller.retryWorkflow(
                                rejected.getExecutionId()
                        )
                        .getBody();

        assertNotNull(retried);

        assertEquals(
                1,
                retried.getRetryCount()
        );

        assertEquals(
                WorkflowStatus.WAITING_FOR_APPROVAL,
                retried.getStatus()
        );

        assertNotEquals(
                rejected.getExecutionId(),
                retried.getExecutionId()
        );
    }

    @Test
    void shouldRejectRetryAfterMaximumRetryLimit() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        EngineeringState current =
                controller.rejectWorkflow(
                                created.getExecutionId()
                        )
                        .getBody();

        for (int retry = 1; retry <= 3; retry++) {

            assertNotNull(current);

            current =
                    controller.retryWorkflow(
                                    current.getExecutionId()
                            )
                            .getBody();

            assertNotNull(current);

            assertEquals(
                    retry,
                    current.getRetryCount()
            );

            current =
                    controller.rejectWorkflow(
                                    current.getExecutionId()
                            )
                            .getBody();
        }

        assertNotNull(current);

        assertTrue(
                controller.retryWorkflow(
                        current.getExecutionId()
                ).getStatusCode().is4xxClientError()
        );
    }

    @Test
    void shouldResumeWorkflowWaitingForApprovalWithoutRerunning() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        EngineeringState resumed =
                controller.resumeWorkflow(
                                created.getExecutionId()
                        )
                        .getBody();

        assertNotNull(resumed);

        assertEquals(
                created.getExecutionId(),
                resumed.getExecutionId()
        );

        assertEquals(
                WorkflowStatus.WAITING_FOR_APPROVAL,
                resumed.getStatus()
        );
    }

    @Test
    void shouldRecordWorkflowAuditHistory() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        controller.approveWorkflow(
                created.getExecutionId()
        );

        List<WorkflowAuditEntity> history =
                controller.getWorkflowHistory(
                                created.getExecutionId()
                        )
                        .getBody();

        assertNotNull(history);

        assertTrue(
                history.stream()
                        .anyMatch(event ->
                                "WORKFLOW_STARTED".equals(
                                        event.getEventType()
                                )
                        )
        );

        assertTrue(
                history.stream()
                        .anyMatch(event ->
                                "WAITING_FOR_APPROVAL".equals(
                                        event.getEventType()
                                )
                        )
        );

        assertTrue(
                history.stream()
                        .anyMatch(event ->
                                "WORKFLOW_APPROVED".equals(
                                        event.getEventType()
                                )
                        )
        );
    }

    @Test
    void shouldHandleGreenfieldScenario() {

        EngineeringState state =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        assertEquals(
                ScenarioType.GREENFIELD,
                state.getScenarioType()
        );

        assertFalse(
                state.getTasks().isEmpty()
        );

        assertFalse(
                state.getValidationResults().isEmpty()
        );
    }

    @Test
    void shouldHandleBrownfieldScenario() {

        EngineeringState state =
                controller.startWorkflow(
                        "Add custom aliases to the existing URL shortener without breaking current APIs"
                );

        assertEquals(
                ScenarioType.BROWNFIELD,
                state.getScenarioType()
        );

        assertFalse(
                state.getRisks().isEmpty()
        );
    }

    @Test
    void shouldHandleAmbiguousScenario() {

        EngineeringState state =
                controller.startWorkflow(
                        "Improve the URL shortener"
                );

        assertEquals(
                ScenarioType.AMBIGUOUS,
                state.getScenarioType()
        );

        assertFalse(
                state.getAmbiguities().isEmpty()
        );
    }

    @Test
    void shouldSafeStopWorkflow() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener"
                );

        EngineeringState stopped =
                controller.safeStopWorkflow(
                                created.getExecutionId()
                        )
                        .getBody();

        assertNotNull(stopped);

        assertEquals(
                WorkflowStatus.SAFE_STOPPED,
                stopped.getStatus()
        );

        assertTrue(
                stopped.isSafeStopped()
        );
    }

    @Test
    void shouldRollbackWorkflowBeforeRelease() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener"
                );

        EngineeringState rolledBack =
                controller.rollbackWorkflow(
                                created.getExecutionId()
                        )
                        .getBody();

        assertNotNull(rolledBack);

        assertEquals(
                WorkflowStatus.ROLLED_BACK,
                rolledBack.getStatus()
        );

        assertTrue(
                rolledBack.isRolledBack()
        );

        assertEquals(
                1,
                rolledBack.getRollbackCount()
        );
    }

    @Test
    void shouldReplanWhenRequirementChanges() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener"
                );

        EngineeringState replanned =
                controller.replanWorkflow(
                                created.getExecutionId(),
                                "Add custom aliases to the existing URL shortener without breaking current APIs"
                        )
                        .getBody();

        assertNotNull(replanned);

        assertTrue(
                replanned.isReplanned()
        );

        assertEquals(
                1,
                replanned.getReplanCount()
        );

        assertEquals(
                ScenarioType.BROWNFIELD,
                replanned.getScenarioType()
        );

        assertEquals(
                WorkflowStatus.WAITING_FOR_APPROVAL,
                replanned.getStatus()
        );
    }

    @Test
    void shouldRecordGuardrailsAndMetrics() {

        EngineeringState state =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        assertFalse(
                state.getGuardrailChecks().isEmpty()
        );

        assertNotNull(
                state.getMetrics()
        );

        assertTrue(
                state.getMetrics()
                        .getEndToEndLatencyMs() >= 0
        );

        assertEquals(
                100.0,
                state.getMetrics()
                        .getSuccessRate()
        );
    }

    @Test
    void shouldCreateFinalEngineeringSummary() {

        EngineeringState state =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        assertNotNull(
                state.getFinalEngineeringSummary()
        );

        assertFalse(
                state.getFinalEngineeringSummary()
                        .isBlank()
        );

        assertFalse(
                state.getArtifacts().isEmpty()
        );

        assertFalse(
                state.getLimitations().isEmpty()
        );
    }
}