package com.campusconnect.sis.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal student record as loaded from MySQL.
 *
 * XXX: no equals/hashCode, no toString, mutable everywhere, and it carries
 * both the canonical fields AND the RIVERTON-only fields because widening this
 * bean in 2014 was cheaper than introducing a per-institution model.
 */
public class Student implements Serializable {

    private static final long serialVersionUID = 20140301L;

    private String studentId;
    private String lastName;
    private String firstName;
    private java.util.Date dateOfBirth;
    private String programCode;
    private String enrollmentStatus;
    private String campusCode;
    private String customerCode;

    /* ---- RIVERTON-only columns. Null for everybody else. ---- */
    private String rivertonCampusId;
    private String advisorNetworkId;
    private String residencyIndicator;
    private String legacyBannerPidm;

    /* ---- SUMMIT-only. ---- */
    private String summitCatalogYear;

    // Raw type on purpose: this is what the 2014 code looked like.
    private List holds = new ArrayList();

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public java.util.Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.util.Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }

    public String getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }

    public String getCampusCode() { return campusCode; }
    public void setCampusCode(String campusCode) { this.campusCode = campusCode; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public String getRivertonCampusId() { return rivertonCampusId; }
    public void setRivertonCampusId(String rivertonCampusId) { this.rivertonCampusId = rivertonCampusId; }

    public String getAdvisorNetworkId() { return advisorNetworkId; }
    public void setAdvisorNetworkId(String advisorNetworkId) { this.advisorNetworkId = advisorNetworkId; }

    public String getResidencyIndicator() { return residencyIndicator; }
    public void setResidencyIndicator(String residencyIndicator) { this.residencyIndicator = residencyIndicator; }

    public String getLegacyBannerPidm() { return legacyBannerPidm; }
    public void setLegacyBannerPidm(String legacyBannerPidm) { this.legacyBannerPidm = legacyBannerPidm; }

    public String getSummitCatalogYear() { return summitCatalogYear; }
    public void setSummitCatalogYear(String summitCatalogYear) { this.summitCatalogYear = summitCatalogYear; }

    /** Raw List of {@link AdvisingHold}. Callers cast. */
    public List getHolds() { return holds; }
    public void setHolds(List holds) { this.holds = holds; }
}
