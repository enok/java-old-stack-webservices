package com.campusconnect.sis.mapper;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.model.Student;
import com.campusconnect.sis.common.util.DateFormats;
import com.campusconnect.sis.ws.common.CustomerCode;
import com.campusconnect.sis.ws.common.StudentDetail;

/**
 * The third copy. Shorter than the other two only because SUMMIT never asked
 * for extra elements on StudentDetail; they asked for a whole extra operation
 * instead (getSummitDegreeAudit). See docs/FORKS.md.
 */
public class SummitStudentMapper {

    private static final Logger LOG = Logger.getLogger(SummitStudentMapper.class);

    public StudentDetail toDetail(Student s) {
        if (s == null) {
            return null;
        }
        StudentDetail d = new StudentDetail();
        d.setStudentId(s.getStudentId());
        d.setLastName(s.getLastName());
        d.setFirstName(s.getFirstName());

        // SUMMIT gets MM-dd-yyyy.
        d.setDateOfBirth(DateFormats.formatQuietly(DateFormats.SUMMIT, s.getDateOfBirth()));

        d.setProgramCode(s.getProgramCode());
        d.setEnrollmentStatus(s.getEnrollmentStatus());

        // XXX: SUMMIT treats campusCode as the catalog year. Yes, really.
        // A field means a different thing depending on who is reading it.
        d.setCampusCode(s.getSummitCatalogYear());

        try {
            d.setCustomerCode(CustomerCode.fromValue(s.getCustomerCode()));
        } catch (Exception e) {
            LOG.warn("unknown customer code: " + s.getCustomerCode());
            e.printStackTrace();
            d.setCustomerCode(CustomerCode.SUMMIT);
        }
        return d;
    }
}
