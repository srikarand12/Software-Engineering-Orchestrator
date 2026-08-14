package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import org.springframework.stereotype.Component;

@Component
public class ValidationAgent implements EngineeringAgent {

    @Override
    public AgentType getType() {
        return AgentType.VALIDATOR;
    }

    @Override
    public AgentResult execute(EngineeringState state) {

        if (state.getWorkflowGraph() == null) {
            return AgentResult.failure("Workflow plan is missing");
        }

        boolean ready = state.getWorkflowGraph()
                .findNode("validation")
                .map(state.getWorkflowGraph()::dependenciesCompleted)
                .orElse(false);

        if (!ready) {
            return AgentResult.failure(
                    "Validation cannot start until testing and security review are complete"
            );
        }

        if (state.getValidationResults().isEmpty()) {
            return AgentResult.failure(
                    "Validation results are missing"
            );
        }

        state.addValidationResult(
                "Workflow validation completed successfully"
        );

        state.addDecision(
                "Validated implementation, test, and security outcomes"
        );

        return AgentResult.success("Validation completed");
    }
}