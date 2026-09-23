package com.campusconnect.sis.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.model.EnrollmentChangeItem;

/**
 * SMELL #6 + a transaction smell.
 *
 * XXX: each change is written with its own autocommit statement. There is no
 * transaction around the batch, so a failure halfway through leaves the
 * student partially enrolled and returns a fault to the caller, who then
 * retries the WHOLE batch and double-adds the successful rows.
 *
 * This is the single highest-value behaviour to pin down with a
 * characterization test before anything is touched.
 */
public class EnrollmentDao {

    private static final Logger LOG = Logger.getLogger(EnrollmentDao.class);

    /** @param changes raw List of {@link EnrollmentChangeItem} */
    public int applyChanges(String customerCode, String studentId, String termCode, List changes) {

        Connection con = null;
        Statement st = null;
        int applied = 0;

        try {
            con = ConnectionFactory.getConnection();
            st = con.createStatement();

            for (Iterator it = changes.iterator(); it.hasNext();) {
                EnrollmentChangeItem item = (EnrollmentChangeItem) it.next();

                String sql;
                if (EnrollmentChangeItem.ACTION_DROP.equals(item.getAction())) {
                    sql = "DELETE FROM enrollment WHERE customer_code = '" + customerCode + "' "
                        + "AND student_id = '" + studentId + "' "
                        + "AND term_code = '" + termCode + "' "
                        + "AND crn = '" + item.getCourseReferenceNumber() + "'";
                } else {
                    sql = "INSERT INTO enrollment (customer_code, student_id, term_code, crn, credit_hours, status) "
                        + "VALUES ('" + customerCode + "', '" + studentId + "', '" + termCode + "', '"
                        + item.getCourseReferenceNumber() + "', '"
                        + item.getCreditHours() + "', 'A')";
                }

                LOG.info("applyChanges SQL: " + sql);
                st.executeUpdate(sql);
                applied++;
            }
            return applied;

        } catch (SQLException sqle) {
            sqle.printStackTrace();
            // TODO: no rollback. There is no transaction to roll back.
            throw new DataAccessException("applyChanges failed after " + applied + " of "
                    + changes.size() + " changes", sqle);
        } finally {
            ConnectionFactory.closeQuietly(st);
            ConnectionFactory.closeQuietly(con);
        }
    }
}
