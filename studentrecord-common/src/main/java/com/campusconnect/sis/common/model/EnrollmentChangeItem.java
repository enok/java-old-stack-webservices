package com.campusconnect.sis.common.model;

import java.io.Serializable;

public class EnrollmentChangeItem implements Serializable {

    private static final long serialVersionUID = 20140301L;

    public static final String ACTION_ADD  = "ADD";
    public static final String ACTION_DROP = "DROP";
    /** TODO: SUMMIT sends "WITHDRAW"; nothing validates this. */
    public static final String ACTION_WITHDRAW = "WITHDRAW";

    private String courseReferenceNumber;
    private String action;
    private String creditHours;
    private String effectiveDate;

    public String getCourseReferenceNumber() { return courseReferenceNumber; }
    public void setCourseReferenceNumber(String c) { this.courseReferenceNumber = c; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getCreditHours() { return creditHours; }
    public void setCreditHours(String creditHours) { this.creditHours = creditHours; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
}
