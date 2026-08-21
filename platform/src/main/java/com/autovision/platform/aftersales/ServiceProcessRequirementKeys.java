package com.autovision.platform.aftersales;

public final class ServiceProcessRequirementKeys {

    public static final ProcessRequirementKey CUSTOMER_AUTHORIZATION =
            new ProcessRequirementKey("SERVICE.CUSTOMER_AUTHORIZATION");
    public static final ProcessRequirementKey INTERNAL_APPROVAL =
            new ProcessRequirementKey("SERVICE.INTERNAL_APPROVAL");
    public static final ProcessRequirementKey QUOTE =
            new ProcessRequirementKey("SERVICE.QUOTE");
    public static final ProcessRequirementKey QUOTE_CONFIRMATION =
            new ProcessRequirementKey("SERVICE.QUOTE_CONFIRMATION");
    public static final ProcessRequirementKey JOB_CONFIRMATION =
            new ProcessRequirementKey("SERVICE.JOB_CONFIRMATION");

    private ServiceProcessRequirementKeys() {
    }
}
