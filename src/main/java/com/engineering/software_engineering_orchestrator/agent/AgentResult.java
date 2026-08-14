package com.engineering.software_engineering_orchestrator.agent;

/**
 * Result returned after an agent finishes its work.
 */
public record AgentResult(boolean success, String message) {

    public static AgentResult success(String message) {
        return new AgentResult(true, message);
    }

    public static AgentResult failure(String message) {
        return new AgentResult(false, message);
    }
}