package com.engineering.software_engineering_orchestrator;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import com.engineering.software_engineering_orchestrator.orchestration.WorkflowStatus;
import com.engineering.software_engineering_orchestrator.orchestration.controller.OrchestrationController;
import com.engineering.software_engineering_orchestrator.orchestration.persistence.WorkflowAuditEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(
                approved.isHumanApproved()
        );

        assertFalse(
                approved.isHumanApprovalRequired()
        );
    }

    @Test
    void shouldCreateAndRejectWorkflow() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        assertNotNull(created);

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

        assertFalse(
                rejected.isHumanApproved()
        );

        assertFalse(
                rejected.isHumanApprovalRequired()
        );
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

        assertFalse(
                retried.getExecutionId()
                        .equals(rejected.getExecutionId())
        );
    }

    @Test
    void shouldRejectRetryAfterMaximumRetryLimit() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        EngineeringState rejected1 =
                controller.rejectWorkflow(
                                created.getExecutionId()
                        )
                        .getBody();

        assertNotNull(rejected1);

        EngineeringState retry1 =
                controller.retryWorkflow(
                                rejected1.getExecutionId()
                        )
                        .getBody();

        assertNotNull(retry1);

        assertEquals(
                1,
                retry1.getRetryCount()
        );

        EngineeringState rejected2 =
                controller.rejectWorkflow(
                                retry1.getExecutionId()
                        )
                        .getBody();

        assertNotNull(rejected2);

        EngineeringState retry2 =
                controller.retryWorkflow(
                                rejected2.getExecutionId()
                        )
                        .getBody();

        assertNotNull(retry2);

        assertEquals(
                2,
                retry2.getRetryCount()
        );

        EngineeringState rejected3 =
                controller.rejectWorkflow(
                                retry2.getExecutionId()
                        )
                        .getBody();

        assertNotNull(rejected3);

        EngineeringState retry3 =
                controller.retryWorkflow(
                                rejected3.getExecutionId()
                        )
                        .getBody();

        assertNotNull(retry3);

        assertEquals(
                3,
                retry3.getRetryCount()
        );

        EngineeringState rejected4 =
                controller.rejectWorkflow(
                                retry3.getExecutionId()
                        )
                        .getBody();

        assertNotNull(rejected4);

        assertTrue(
                controller.retryWorkflow(
                        rejected4.getExecutionId()
                ).getStatusCode().is4xxClientError()
        );
    }

    @Test
    void shouldResumeWorkflowWaitingForApprovalWithoutRerunning() {

        EngineeringState created =
                controller.startWorkflow(
                        "Build a URL shortener with expiration and click analytics"
                );

        assertNotNull(created);

        assertEquals(
                WorkflowStatus.WAITING_FOR_APPROVAL,
                created.getStatus()
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

        assertNotNull(created);

        EngineeringState approved =
                controller.approveWorkflow(
                                created.getExecutionId()
                        )
                        .getBody();

        assertNotNull(approved);

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
}