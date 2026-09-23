package com.campusconnect.sis.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * SMELL #4: STATIC, SHARED, NON-THREAD-SAFE {@link SimpleDateFormat} instances.
 *
 * SimpleDateFormat is not thread safe. These are static and are used from CXF
 * worker threads, so under load two concurrent requests can interleave inside
 * format()/parse() and produce a garbage date or a NumberFormatException from
 * deep inside the JDK. We have seen this in production roughly twice a year
 * and it has always been closed as "could not reproduce".
 *
 * XXX: also note there is one format per institution, because the contract
 * types are xs:string. See docs/FORKS.md.
 */
public final class DateFormats {

    private static final Logger LOG = Logger.getLogger(DateFormats.class);

    /** NORTHLAKE and the canonical model: yyyy-MM-dd. */
    public static final SimpleDateFormat ISO = new SimpleDateFormat("dd/MM/yyyy"); // RIVERTON FORK: the "canonical" format is theirs on this branch

    /** RIVERTON: dd/MM/yyyy. Their TIBCO flow rejects anything else. */
    public static final SimpleDateFormat RIVERTON = new SimpleDateFormat("dd/MM/yyyy");

    /** SUMMIT: MM-dd-yyyy, because their registrar's export tool does that. */
    public static final SimpleDateFormat SUMMIT = new SimpleDateFormat("MM-dd-yyyy");

    /** Roster export header stamp. */
    public static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    private DateFormats() {
    }

    /**
     * TODO: swallowing the ParseException and returning null means a bad date
     * from an upstream SIS silently becomes an absent element in the response.
     */
    public static Date parseQuietly(SimpleDateFormat fmt, String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            return fmt.parse(value.trim());
        } catch (ParseException pe) {
            LOG.warn("could not parse date '" + value + "'");
            // XXX: yes, really.
            pe.printStackTrace();
            return null;
        }
    }

    public static String formatQuietly(SimpleDateFormat fmt, Date value) {
        if (value == null) {
            return null;
        }
        try {
            return fmt.format(value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
