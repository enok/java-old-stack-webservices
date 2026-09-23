package com.campusconnect.sis.common.exception;

/**
 * SMELL #4: a CHECKED exception on the service interface.
 *
 * Every caller of {@link com.campusconnect.sis.common.service.StudentRecordFacade}
 * must either catch this or declare it. In practice callers catch it, log it,
 * and return null, which is how "not found" became indistinguishable from
 * "the database is down".
 */
public class StudentServiceException extends Exception {

    private static final long serialVersionUID = 20140301L;

    private String faultCode = "SIS-0000";

    public StudentServiceException(String message) {
        super(message);
    }

    public StudentServiceException(String faultCode, String message) {
        super(message);
        this.faultCode = faultCode;
    }

    public StudentServiceException(String faultCode, String message, Throwable cause) {
        super(message, cause);
        this.faultCode = faultCode;
    }

    public String getFaultCode() {
        return faultCode;
    }
}
