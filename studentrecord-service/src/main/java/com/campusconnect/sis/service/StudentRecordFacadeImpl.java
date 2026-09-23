package com.campusconnect.sis.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.exception.StudentServiceException;
import com.campusconnect.sis.common.model.CustomerCodes;
import com.campusconnect.sis.common.model.Student;
import com.campusconnect.sis.common.service.StudentRecordFacade;
import com.campusconnect.sis.dao.EnrollmentDao;
import com.campusconnect.sis.dao.HoldDao;
import com.campusconnect.sis.dao.StudentDao;

/**
 * Wired as a Spring singleton in applicationContext.xml.
 *
 * SMELL #5: the two not-found conventions live side by side in this class.
 * {@link #getStudent} returns null. {@link #getAdvisingHolds} throws. Same
 * layer, same file, opposite behaviour.
 *
 * SMELL #6 leaks through here too: the DAOs throw an UNCHECKED
 * DataAccessException, which this class does not catch, so it sails past the
 * checked-exception boundary and reaches CXF as a raw RuntimeException.
 */
public class StudentRecordFacadeImpl implements StudentRecordFacade {

    private static final Logger LOG = Logger.getLogger(StudentRecordFacadeImpl.class);

    private StudentDao studentDao;
    private HoldDao holdDao;
    private EnrollmentDao enrollmentDao;

    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }
    public void setHoldDao(HoldDao holdDao) { this.holdDao = holdDao; }
    public void setEnrollmentDao(EnrollmentDao enrollmentDao) { this.enrollmentDao = enrollmentDao; }

    /** Returns null when not found. Does NOT throw. See SMELL #5a. */
    public Student getStudent(String customerCode, String studentId, boolean includeHolds)
            throws StudentServiceException {

        Student s = studentDao.findStudent(customerCode, studentId);
        if (s == null) {
            LOG.info("no student " + studentId + " for " + customerCode);
            return null;
        }

        // SMELL #1: RIVERTON ignores includeHolds and always gets holds,
        // because their TIBCO flow breaks if the element is absent.
        if (includeHolds || CustomerCodes.RIVERTON.equals(customerCode)) {
            s.setHolds(holdDao.findHolds(customerCode, studentId));
        }
        return s;
    }

    /** Throws when nothing matches. See SMELL #5a. */
    public List findStudentsByTerm(String customerCode, String termCode, String programCode, int maxResults)
            throws StudentServiceException {

        List found = studentDao.findByTerm(customerCode, termCode, programCode, maxResults);
        if (found.isEmpty()) {
            // Inconsistent with getStudent above, which returns null.
            throw new StudentServiceException("SIS-4040",
                    "no students found for term " + termCode + " at " + customerCode);
        }
        return found;
    }

    public List getAdvisingHolds(String customerCode, String studentId)
            throws StudentServiceException {

        Student s = studentDao.findStudent(customerCode, studentId);
        if (s == null) {
            throw new StudentServiceException("SIS-4041",
                    "no student " + studentId + " at " + customerCode);
        }
        return holdDao.findHolds(customerCode, studentId);
    }

    public String submitEnrollmentChange(String customerCode, String studentId, String termCode, List changes)
            throws StudentServiceException {

        if (changes == null || changes.isEmpty()) {
            throw new StudentServiceException("SIS-4001", "no changes submitted");
        }

        // SMELL #1: SUMMIT is capped at 8 changes per call because their
        // middleware times out. The cap is silent; the extras are dropped.
        List effective = changes;
        if (CustomerCodes.SUMMIT.equals(customerCode) && changes.size() > 8) {
            LOG.warn("truncating SUMMIT enrollment batch from " + changes.size() + " to 8");
            effective = new ArrayList();
            int i = 0;
            for (Iterator it = changes.iterator(); it.hasNext() && i < 8; i++) {
                effective.add(it.next());
            }
        }

        // XXX: no transaction. DataAccessException from here is UNCHECKED and
        // escapes this method's throws clause entirely. SMELL #5b.
        int applied = enrollmentDao.applyChanges(customerCode, studentId, termCode, effective);

        // TODO: the confirmation id is not stored anywhere. A consumer that
        // quotes it back at us cannot be answered.
        return "CC-" + customerCode + "-" + studentId + "-" + termCode + "-" + applied;
    }
}
