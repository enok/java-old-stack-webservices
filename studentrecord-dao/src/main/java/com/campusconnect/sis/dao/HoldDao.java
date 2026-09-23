package com.campusconnect.sis.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.model.AdvisingHold;
import com.campusconnect.sis.common.model.CustomerCodes;

/** SMELL #6 again. Same pattern, copied. */
public class HoldDao {

    private static final Logger LOG = Logger.getLogger(HoldDao.class);

    /** @return raw List of {@link AdvisingHold}. */
    public List findHolds(String customerCode, String studentId) {

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT hold_code, hold_reason, placed_on, releasable ");
        sql.append("FROM advising_hold ");
        sql.append("WHERE customer_code = '").append(customerCode).append("' ");
        sql.append("AND student_id = '").append(studentId).append("' ");
        // XXX: NORTHLAKE hides released holds; the other two want the full history.
        if (CustomerCodes.NORTHLAKE.equals(customerCode)) {
            sql.append("AND released_on IS NULL ");
        }
        sql.append("ORDER BY placed_on DESC");

        LOG.info("findHolds SQL: " + sql.toString());

        List out = new ArrayList();
        try {
            con = ConnectionFactory.getConnection();
            st = con.createStatement();
            rs = st.executeQuery(sql.toString());
            while (rs.next()) {
                AdvisingHold h = new AdvisingHold();
                h.setHoldCode(rs.getString("hold_code"));
                h.setHoldReason(rs.getString("hold_reason"));
                h.setPlacedOn(rs.getDate("placed_on"));
                h.setReleasable(rs.getBoolean("releasable"));
                out.add(h);
            }
            return out;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new DataAccessException("findHolds failed for " + customerCode + "/" + studentId, sqle);
        } finally {
            ConnectionFactory.closeQuietly(rs);
            ConnectionFactory.closeQuietly(st);
            ConnectionFactory.closeQuietly(con);
        }
    }
}
