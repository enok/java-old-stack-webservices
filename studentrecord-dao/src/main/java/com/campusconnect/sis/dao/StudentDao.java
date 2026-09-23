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
 * SMELL #6: hand-rolled DAO with STRING-CONCATENATED SQL.
 *
 * ------------------------------------------------------------------------
 * SECURITY: every query in this class interpolates caller-supplied values
 * straight into the SQL text. studentId, termCode and programCode all arrive
 * from the SOAP body, unvalidated. This is a textbook SQL injection surface.
 *
 * It is reproduced here deliberately, as the "before" state of the
 * modernization exercise. It is a FIX TARGET, not a demonstration: no exploit
 * payload appears anywhere in this repository. The remediation is
 * PreparedStatement with bind parameters (or, post-modernization, Spring Data
 * JPA), and it is sequenced in docs/MODERNIZATION-BACKLOG.md.
 *
 * Do not deploy this against a real database.
 * ------------------------------------------------------------------------
 */
public class StudentDao {

    private static final Logger LOG = Logger.getLogger(StudentDao.class);

    /**
     * XXX: the column list differs per institution because RIVERTON's extra
     * columns live in a side table that only exists in their schema. So the
     * FROM clause is assembled by if/else too. SMELL #1.
     */
    public Student findStudent(String customerCode, String studentId) {

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        // SMELL #1: customer branching inside the DAO.
        String sql;
        if (CustomerCodes.RIVERTON.equals(customerCode)) {
            sql = "SELECT s.student_id, s.last_name, s.first_name, s.date_of_birth, "
                + "s.program_code, s.enrollment_status, s.campus_code, s.customer_code, "
                + "r.riverton_campus_id, r.advisor_network_id, r.residency_indicator, r.legacy_banner_pidm "
                + "FROM student s LEFT JOIN riverton_student_ext r ON r.student_id = s.student_id "
                + "WHERE s.customer_code = '" + customerCode + "' "
                + "AND s.student_id = '" + studentId + "'";
        } else if (CustomerCodes.SUMMIT.equals(customerCode)) {
            sql = "SELECT s.student_id, s.last_name, s.first_name, s.date_of_birth, "
                + "s.program_code, s.enrollment_status, s.campus_code, s.customer_code, "
                + "m.catalog_year "
                + "FROM student s LEFT JOIN summit_student_ext m ON m.student_id = s.student_id "
                + "WHERE s.customer_code = '" + customerCode + "' "
                + "AND s.student_id = '" + studentId + "'";
        } else {
            sql = "SELECT s.student_id, s.last_name, s.first_name, s.date_of_birth, "
                + "s.program_code, s.enrollment_status, s.campus_code, s.customer_code "
                + "FROM student s "
                + "WHERE s.customer_code = '" + customerCode + "' "
                + "AND s.student_id = '" + studentId + "'";
        }

        // XXX: and we log the fully interpolated statement at INFO.
        LOG.info("findStudent SQL: " + sql);

        try {
            con = ConnectionFactory.getConnection();
            st = con.createStatement();
            rs = st.executeQuery(sql);
            if (!rs.next()) {
                // SMELL #5a: not found is a null return, all the way up.
                return null;
            }
            Student s = new Student();
            s.setStudentId(rs.getString("student_id"));
            s.setLastName(rs.getString("last_name"));
            s.setFirstName(rs.getString("first_name"));
            s.setDateOfBirth(rs.getDate("date_of_birth"));
            s.setProgramCode(rs.getString("program_code"));
            s.setEnrollmentStatus(rs.getString("enrollment_status"));
            s.setCampusCode(rs.getString("campus_code"));
            s.setCustomerCode(rs.getString("customer_code"));

            if (CustomerCodes.RIVERTON.equals(customerCode)) {
                s.setRivertonCampusId(rs.getString("riverton_campus_id"));
                s.setAdvisorNetworkId(rs.getString("advisor_network_id"));
                s.setResidencyIndicator(rs.getString("residency_indicator"));
                s.setLegacyBannerPidm(rs.getString("legacy_banner_pidm"));
            } else if (CustomerCodes.SUMMIT.equals(customerCode)) {
                s.setSummitCatalogYear(rs.getString("catalog_year"));
            }
            return s;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new DataAccessException("findStudent failed for " + customerCode + "/" + studentId, sqle);
        } finally {
            ConnectionFactory.closeQuietly(rs);
            ConnectionFactory.closeQuietly(st);
            ConnectionFactory.closeQuietly(con);
        }
    }

    /**
     * @return raw List of {@link Student}.
     *
     * XXX: maxResults is appended as a LIMIT only when positive; otherwise the
     * whole term is loaded into memory. SUMMIT has 41k students.
     */
    public List findByTerm(String customerCode, String termCode, String programCode, int maxResults) {

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT s.student_id, s.last_name, s.first_name, s.date_of_birth, ");
        sql.append("s.program_code, s.enrollment_status, s.campus_code, s.customer_code ");
        sql.append("FROM student s JOIN enrollment e ON e.student_id = s.student_id ");
        sql.append("WHERE s.customer_code = '").append(customerCode).append("' ");
        sql.append("AND e.term_code = '").append(termCode).append("' ");
        if (programCode != null && programCode.trim().length() > 0) {
            sql.append("AND s.program_code = '").append(programCode).append("' ");
        }
        // SMELL #1 again: RIVERTON excludes withdrawn students, the others do not.
        if (CustomerCodes.RIVERTON.equals(customerCode)) {
            sql.append("AND s.enrollment_status <> 'W' ");
        }
        sql.append("GROUP BY s.student_id ORDER BY s.last_name, s.first_name ");
        if (maxResults > 0) {
            sql.append("LIMIT ").append(maxResults + 1);
        }

        LOG.info("findByTerm SQL: " + sql.toString());

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
                s.setDateOfBirth(rs.getDate("date_of_birth"));
                s.setProgramCode(rs.getString("program_code"));
                s.setEnrollmentStatus(rs.getString("enrollment_status"));
                s.setCampusCode(rs.getString("campus_code"));
                s.setCustomerCode(rs.getString("customer_code"));
                out.add(s);
            }
            return out;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new DataAccessException("findByTerm failed for " + customerCode + "/" + termCode, sqle);
        } finally {
            ConnectionFactory.closeQuietly(rs);
            ConnectionFactory.closeQuietly(st);
            ConnectionFactory.closeQuietly(con);
        }
    }
}
