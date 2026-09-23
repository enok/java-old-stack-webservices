package com.campusconnect.sis.common.service;

import java.util.List;

import com.campusconnect.sis.common.exception.StudentServiceException;
import com.campusconnect.sis.common.model.Student;

/**
 * Internal service boundary between the SOAP endpoints and the DAOs.
 *
 * SMELL #4: raw {@link List} return types and a CHECKED exception on every
 * method. SMELL #5: the contract of {@link #getStudent} is "returns null when
 * not found", while {@link #findStudentsByTerm} throws. Two different
 * not-found conventions on one interface.
 */
public interface StudentRecordFacade {

    /**
     * @return the student, or <code>null</code> if no such student exists for
     *         this institution. Does NOT throw on not-found.
     */
    Student getStudent(String customerCode, String studentId, boolean includeHolds)
            throws StudentServiceException;

    /** @return raw List of {@link Student}. Never null, possibly empty. */
    List findStudentsByTerm(String customerCode, String termCode, String programCode, int maxResults)
            throws StudentServiceException;

    /** @return raw List of {@link com.campusconnect.sis.common.model.AdvisingHold}. */
    List getAdvisingHolds(String customerCode, String studentId)
            throws StudentServiceException;

    /**
     * @param changes raw List of {@link com.campusconnect.sis.common.model.EnrollmentChangeItem}
     * @return a confirmation id
     */
    String submitEnrollmentChange(String customerCode, String studentId, String termCode, List changes)
            throws StudentServiceException;
}
