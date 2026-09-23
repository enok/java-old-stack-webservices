package com.campusconnect.sis.mapper;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.model.CustomerCodes;
import com.campusconnect.sis.common.model.Student;
import com.campusconnect.sis.common.util.DateFormats;
import com.campusconnect.sis.ws.common.CustomerCode;
import com.campusconnect.sis.ws.common.RivertonStudentDetail;
import com.campusconnect.sis.ws.common.StudentDetail;

/**
 * SMELL #3: the 80%-identical twin of {@link NorthlakeStudentMapper}.
 *
 * Compare {@link #toDetail(Student)} with NorthlakeStudentMapper.toDetail
 * line by line. The differences are:
 *   1. the SimpleDateFormat constant used for dateOfBirth (dd/MM/yyyy);
 *   2. the four RIVERTON-only field copies;
 *   3. the programCode null-handling fix that was never ported here.
 *
 * Everything else is a copy. This is the file that makes "consolidate the
 * institutions behind one canonical model" a real piece of work rather than a
 * refactor-rename.
 */
public class RivertonStudentMapper {

    private static final Logger LOG = Logger.getLogger(RivertonStudentMapper.class);

    /** Returns the RIVERTON subtype, which carries four extra elements. */
    public RivertonStudentDetail toDetail(Student s) {
        if (s == null) {
            return null;
        }
        RivertonStudentDetail d = new RivertonStudentDetail();
        d.setStudentId(s.getStudentId());
        d.setLastName(s.getLastName());
        d.setFirstName(s.getFirstName());

        // RIVERTON gets dd/MM/yyyy. Their TIBCO flow rejects anything else.
        d.setDateOfBirth(DateFormats.formatQuietly(DateFormats.RIVERTON, s.getDateOfBirth()));

        // XXX: NOT the trimToNull() fix that NorthlakeStudentMapper got in 2017.
        // A null programCode is emitted as the four-character string "null".
        d.setProgramCode(String.valueOf(s.getProgramCode()));

        d.setEnrollmentStatus(s.getEnrollmentStatus());
        d.setCampusCode(s.getCampusCode());

        try {
            d.setCustomerCode(CustomerCode.fromValue(s.getCustomerCode()));
        } catch (Exception e) {
            LOG.warn("unknown customer code: " + s.getCustomerCode());
            e.printStackTrace();
            d.setCustomerCode(CustomerCode.RIVERTON);
        }

        /* ---- the RIVERTON-only tail ---- */
        d.setRivertonCampusId(s.getRivertonCampusId());
        d.setAdvisorNetworkId(s.getAdvisorNetworkId());
        d.setResidencyIndicator(s.getResidencyIndicator());
        d.setLegacyBannerPidm(s.getLegacyBannerPidm());
        // RIVERTON FORK: extra field, has no column on master.
        d.setResidencyReviewedOn(DateFormats.formatQuietly(DateFormats.RIVERTON, s.getDateOfBirth()));

        return d;
    }

    /**
     * SMELL #1: customer branching inside a mapper that is already
     * customer-specific. findStudentsByTerm returns the BASE type for every
     * institution including RIVERTON, so this mapper has a second, downgraded
     * path. RIVERTON consumers therefore see four extra elements from
     * getStudent and none from findStudentsByTerm.
     */
    public StudentDetail toBaseDetailForSearch(Student s) {
        if (s == null) {
            return null;
        }
        StudentDetail d = new StudentDetail();
        d.setStudentId(s.getStudentId());
        d.setLastName(s.getLastName());
        d.setFirstName(s.getFirstName());
        if (CustomerCodes.RIVERTON.equals(s.getCustomerCode())) {
            d.setDateOfBirth(DateFormats.formatQuietly(DateFormats.RIVERTON, s.getDateOfBirth()));
        } else {
            d.setDateOfBirth(DateFormats.formatQuietly(DateFormats.ISO, s.getDateOfBirth()));
        }
        d.setProgramCode(String.valueOf(s.getProgramCode()));
        d.setEnrollmentStatus(s.getEnrollmentStatus());
        d.setCampusCode(s.getCampusCode());
        d.setCustomerCode(CustomerCode.RIVERTON);
        return d;
    }
}
