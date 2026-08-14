package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;

public interface EngineeringAgent {

    AgentType getType();

    AgentResult execute(EngineeringState state);
}