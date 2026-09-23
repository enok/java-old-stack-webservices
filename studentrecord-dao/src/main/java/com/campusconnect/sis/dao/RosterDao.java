package com.campusconnect.sis.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.model.CustomerCodes;
import com.campusconnect.sis.common.model.Student;

/**
 * Feeds the nightly roster export.
 *
 * XXX: this duplicates most of {@link StudentDao#findByTerm} with a different
 * column list, because the batch team did not want to depend on the service
 * team's DAO. See SMELL #3 (duplicated mappers) for the same disease one layer
 * up.
 */
public class RosterDao {

    private static final Logger LOG = Logger.getLogger(RosterDao.class);

    /** @return raw List of {@link Student}, one per enrolled student. */
    public List loadRoster(String customerCode, String termCode) {

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT s.student_id, s.last_name, s.first_name, s.program_code, ");
        sql.append("s.enrollment_status, s.customer_code");
        if (CustomerCodes.RIVERTON.equals(customerCode)) {
            sql.append(", r.legacy_banner_pidm");
        }
        sql.append(" FROM student s ");
        sql.append("JOIN enrollment e ON e.student_id = s.student_id ");
        if (CustomerCodes.RIVERTON.equals(customerCode)) {
            sql.append("LEFT JOIN riverton_student_ext r ON r.student_id = s.student_id ");
        }
        sql.append("WHERE s.customer_code = '").append(customerCode).append("' ");
        sql.append("AND e.term_code = '").append(termCode).append("' ");
        sql.append("GROUP BY s.student_id ORDER BY s.last_name");

        LOG.info("loadRoster SQL: " + sql.toString());

        List out = new ArrayList();
        try {
            con = ConnectionFactory.getConnection();
            st = con.createStatement();
            rs = st.executeQuery(sql.toString());
            while (rs.next()) {
                Student s = new Student();
                s.setStudentId(rs.getString("student_id"));
                s.setLastName(rs.getString("last_name"));
                s.setFirstName(rs.getString("first_name"));
                s.setProgramCode(rs.getString("program_code"));
                s.setEnrollmentStatus(rs.getString("enrollment_status"));
                s.setCustomerCode(rs.getString("customer_code"));
                if (CustomerCodes.RIVERTON.equals(customerCode)) {
                    s.setLegacyBannerPidm(rs.getString("legacy_banner_pidm"));
                }
                out.add(s);
            }
            return out;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new DataAccessException("loadRoster failed for " + customerCode + "/" + termCode, sqle);
        } finally {
            ConnectionFactory.closeQuietly(rs);
            ConnectionFactory.closeQuietly(st);
            ConnectionFactory.closeQuietly(con);
        }
    }

    /** @return raw List of String CRNs for one student in one term. */
    public List loadCrns(String customerCode, String studentId, String termCode) {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        String sql = "SELECT crn FROM enrollment WHERE customer_code = '" + customerCode + "' "
                + "AND student_id = '" + studentId + "' AND term_code = '" + termCode + "' ORDER BY crn";
        List out = new ArrayList();
        try {
            con = ConnectionFactory.getConnection();
            st = con.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                out.add(rs.getString("crn"));
            }
            return out;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new DataAccessException("loadCrns failed", sqle);
        } finally {
            ConnectionFactory.closeQuietly(rs);
            ConnectionFactory.closeQuietly(st);
            ConnectionFactory.closeQuietly(con);
        }
    }
}
