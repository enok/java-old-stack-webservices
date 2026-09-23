package com.campusconnect.sis.common.model;

/**
 * The three institutions this integration tier serves.
 *
 * XXX: String constants rather than an enum, so nothing in the codebase can
 * exhaustively switch on the customer. Every dispatch site is a chain of
 * if ("RIVERTON".equals(code)) instead. Adding a fourth institution means
 * grepping for these strings.
 */
public final class CustomerCodes {

    public static final String NORTHLAKE = "NORTHLAKE";
    public static final String RIVERTON  = "RIVERTON";
    public static final String SUMMIT    = "SUMMIT";

    /** TODO: two institutions were onboarded and then churned. Still referenced in seed.sql. */
    public static final String LAKESHORE_RETIRED = "LAKESHORE";

    private CustomerCodes() {
    }
}
