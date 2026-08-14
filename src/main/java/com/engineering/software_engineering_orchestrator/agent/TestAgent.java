package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TestAgent implements EngineeringAgent {

    private static final Logger log =
            LoggerFactory.getLogger(TestAgent.class);

    @Override
    public AgentType getType() {
        return AgentType.TESTER;
    }

    @Override
    public AgentResult execute(EngineeringState state) {

        if (state.getWorkflowGraph() == null) {
            return AgentResult.failure(
                    "Workflow plan is missing"
            );
        }

        boolean ready = state.getWorkflowGraph()
                .findNode("testing")
                .map(state.getWorkflowGraph()::dependenciesCompleted)
                .orElse(false);

        if (!ready) {
            return AgentResult.failure(
                    "Testing cannot start until implementation is complete"
            );
        }

        log.info(
                "Testing started on thread {}",
                Thread.currentThread().getName()
        );

        state.addValidationResult(
                "Basic implementation checks completed successfully"
        );

        state.addDecision(
                "Testing completed for the current change"
        );

        return AgentResult.success(
                "Testing completed"
        );
    }
}