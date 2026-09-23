package com.campusconnect.sis.mapper;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.model.Student;
import com.campusconnect.sis.common.util.DateFormats;
import com.campusconnect.sis.common.util.LegacyStringUtils;
import com.campusconnect.sis.ws.common.CustomerCode;
import com.campusconnect.sis.ws.common.StudentDetail;

/**
 * SMELL #3: this class and {@link RivertonStudentMapper} are roughly 80%
 * identical. The diff is four extra field copies and a different
 * SimpleDateFormat. They were forked in 2014 "temporarily" while RIVERTON was
 * onboarded and never merged back.
 *
 * Any fix to one of them has to be remembered in the other. In practice it
 * never is: see the null-handling of programCode below, which was fixed here
 * in 2017 and is still broken in the RIVERTON copy.
 */
public class NorthlakeStudentMapper {

    private static final Logger LOG = Logger.getLogger(NorthlakeStudentMapper.class);

    public StudentDetail toDetail(Student s) {
        if (s == null) {
            return null;
        }
        StudentDetail d = new StudentDetail();
        d.setStudentId(s.getStudentId());
        d.setLastName(s.getLastName());
        d.setFirstName(s.getFirstName());

        // NORTHLAKE gets ISO dates.
        d.setDateOfBirth(DateFormats.formatQuietly(DateFormats.ISO, s.getDateOfBirth()));

        // Fixed 2017-03: was emitting the literal "null". Not fixed in RivertonStudentMapper.
        d.setProgramCode(LegacyStringUtils.trimToNull(s.getProgramCode()));

        d.setEnrollmentStatus(s.getEnrollmentStatus());
        d.setCampusCode(s.getCampusCode());

        try {
            d.setCustomerCode(CustomerCode.fromValue(s.getCustomerCode()));
        } catch (Exception e) {
            // XXX: a retired institution code (LAKESHORE) still in the database
            // lands here on every roster sweep.
            LOG.warn("unknown customer code: " + s.getCustomerCode());
            e.printStackTrace();
            d.setCustomerCode(CustomerCode.NORTHLAKE);
        }
        return d;
    }
}
