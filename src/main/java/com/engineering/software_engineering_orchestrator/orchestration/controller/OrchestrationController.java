package com.engineering.software_engineering_orchestrator.orchestration.controller;

import com.engineering.software_engineering_orchestrator.orchestration.EngineeringOrchestrator;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import com.engineering.software_engineering_orchestrator.orchestration.WorkflowStatus;
import com.engineering.software_engineering_orchestrator.orchestration.WorkflowStore;
import com.engineering.software_engineering_orchestrator.orchestration.persistence.WorkflowAuditEntity;
import com.engineering.software_engineering_orchestrator.orchestration.service.WorkflowAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
public class OrchestrationController {

    private final EngineeringOrchestrator orchestrator;
    private final WorkflowStore workflowStore;
    private final WorkflowAuditService workflowAuditService;

    public OrchestrationController(
            EngineeringOrchestrator orchestrator,
            WorkflowStore workflowStore,
            WorkflowAuditService workflowAuditService) {

        this.orchestrator = orchestrator;
        this.workflowStore = workflowStore;
        this.workflowAuditService = workflowAuditService;
    }

    @PostMapping
    public EngineeringState startWorkflow(
            @RequestBody String requirement) {

        return orchestrator.execute(requirement);
    }

    @GetMapping("/{executionId}")
    public ResponseEntity<EngineeringState> getWorkflow(
            @PathVariable String executionId) {

        return workflowStore.findById(executionId)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/{executionId}/history")
    public ResponseEntity<List<WorkflowAuditEntity>> getWorkflowHistory(
            @PathVariable String executionId) {

        if (workflowStore.findById(executionId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                workflowAuditService.getHistory(
                        executionId
                )
        );
    }

    @PostMapping("/{executionId}/approve")
    public ResponseEntity<EngineeringState> approveWorkflow(
            @PathVariable String executionId) {

        return workflowStore.findById(executionId)
                .map(state -> {

                    if (state.getStatus()
                            != WorkflowStatus.WAITING_FOR_APPROVAL) {

                        return ResponseEntity.badRequest()
                                .body(state);
                    }

                    state.setHumanApproved(true);
                    state.setHumanApprovalRequired(false);

                    if (state.getWorkflowGraph() != null) {

                        state.getWorkflowGraph()
                                .findNode("release")
                                .ifPresent(node ->
                                        node.setStatus(
                                                WorkflowStatus.COMPLETED
                                        )
                                );
                    }

                    state.addDecision(
                            "Release approved by human reviewer"
                    );

                    state.setStatus(
                            WorkflowStatus.COMPLETED
                    );

                    state.setFinishedAt(
                            java.time.Instant.now()
                    );

                    workflowStore.save(state);

                    workflowAuditService.record(
                            executionId,
                            "WORKFLOW_APPROVED",
                            "Workflow approved by human reviewer"
                    );

                    return ResponseEntity.ok(state);
                })
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @PostMapping("/{executionId}/reject")
    public ResponseEntity<EngineeringState> rejectWorkflow(
            @PathVariable String executionId) {

        return workflowStore.findById(executionId)
                .map(state -> {

                    if (state.getStatus()
                            != WorkflowStatus.WAITING_FOR_APPROVAL) {

                        return ResponseEntity.badRequest()
                                .body(state);
                    }

                    state.setHumanApproved(false);
                    state.setHumanApprovalRequired(false);

                    state.addDecision(
                            "Release rejected by human reviewer"
                    );

                    state.setStatus(
                            WorkflowStatus.REJECTED
                    );

                    workflowStore.save(state);

                    workflowAuditService.record(
                            executionId,
                            "WORKFLOW_REJECTED",
                            "Workflow rejected by human reviewer"
                    );

                    return ResponseEntity.ok(state);
                })
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @PostMapping("/{executionId}/retry")
    public ResponseEntity<EngineeringState> retryWorkflow(
            @PathVariable String executionId) {

        try {

            return ResponseEntity.ok(
                    orchestrator.retry(
                            executionId
                    )
            );

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.notFound().build();

        } catch (IllegalStateException exception) {

            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{executionId}/resume")
    public ResponseEntity<EngineeringState> resumeWorkflow(
            @PathVariable String executionId) {

        try {

            return ResponseEntity.ok(
                    orchestrator.resume(
                            executionId
                    )
            );

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.notFound().build();

        } catch (IllegalStateException exception) {

            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{executionId}/safe-stop")
    public ResponseEntity<EngineeringState> safeStopWorkflow(
            @PathVariable String executionId) {

        try {

            return ResponseEntity.ok(
                    orchestrator.safeStop(
                            executionId
                    )
            );

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.notFound().build();

        } catch (IllegalStateException exception) {

            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{executionId}/rollback")
    public ResponseEntity<EngineeringState> rollbackWorkflow(
            @PathVariable String executionId) {

        try {

            return ResponseEntity.ok(
                    orchestrator.rollback(
                            executionId
                    )
            );

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.notFound().build();

        } catch (IllegalStateException exception) {

            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{executionId}/replan")
    public ResponseEntity<EngineeringState> replanWorkflow(
            @PathVariable String executionId,
            @RequestBody String updatedRequirement) {

        try {

            return ResponseEntity.ok(
                    orchestrator.replan(
                            executionId,
                            updatedRequirement
                    )
            );

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest().build();

        } catch (IllegalStateException exception) {

            return ResponseEntity.badRequest().build();
        }
    }
}