package com.autovision.platform.aftersales;

public interface ProcessRequirementSatisfactionProvider {

    ProcessRequirementKey key();

    boolean isSatisfied(ProcessRequirementEvaluationContext context);
}