package com.autovision.platform.aftersales;

/**
 * Permission codes for Core DMS AfterSales operations.
 */
public final class AfterSalesPermissions {

    public static final String CASE_READ = "AFTERSALES_CASE.READ";
    public static final String CASE_CREATE = "AFTERSALES_CASE.CREATE";
    public static final String CASE_UPDATE = "AFTERSALES_CASE.UPDATE";

    public static final String CUSTOMER_AUTHORIZATION_READ =
            "CUSTOMER_AUTHORIZATION.READ";

    public static final String CUSTOMER_AUTHORIZATION_CREATE =
            "CUSTOMER_AUTHORIZATION.CREATE";

    public static final String CUSTOMER_AUTHORIZATION_DECIDE =
            "CUSTOMER_AUTHORIZATION.DECIDE";

    private AfterSalesPermissions() {
    }
}
