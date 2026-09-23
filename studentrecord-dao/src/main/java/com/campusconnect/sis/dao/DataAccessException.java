package com.campusconnect.sis.dao;

/**
 * XXX: a RUNTIME exception here, while the service layer above uses a CHECKED
 * one. The two conventions meet in StudentRecordFacadeImpl and the result is
 * SMELL #5b: a DataAccessException that escapes as a raw SOAP fault with a
 * JDBC stack trace in the faultstring.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 20140301L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
