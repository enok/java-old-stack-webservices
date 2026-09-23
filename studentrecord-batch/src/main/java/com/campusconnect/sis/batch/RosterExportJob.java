package com.campusconnect.sis.batch;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.campusconnect.sis.batch.generated.ObjectFactory;
import com.campusconnect.sis.batch.generated.RosterExport;
import com.campusconnect.sis.batch.generated.RosterFooter;
import com.campusconnect.sis.batch.generated.RosterHeader;
import com.campusconnect.sis.batch.generated.RosterRow;
import com.campusconnect.sis.common.model.CustomerCodes;
import com.campusconnect.sis.common.model.Student;
import com.campusconnect.sis.common.util.DateFormats;
import com.campusconnect.sis.dao.RosterDao;

/**
 * The nightly roster export.
 *
 * Launched from cron on the Tomcat box, not from the container:
 *   0 2 * * *  java -cp .../studentrecord-batch.jar:... \
 *              com.campusconnect.sis.batch.RosterExportJob NORTHLAKE 202410 /var/sis/out
 *
 * It shares the DAO layer with the SOAP service but NOT the model: the roster
 * file has its own XSD, its own xjc run and its own generated classes, which
 * duplicate most of sc:StudentDetail under different element names.
 *
 * XXX: this job runs for all three institutions in sequence from three
 * separate crontab lines, so a failure in one is invisible to the others, and
 * nothing reconciles the three outputs.
 */
public class RosterExportJob {

    private static final Logger LOG = Logger.getLogger(RosterExportJob.class);

    private RosterDao rosterDao = new RosterDao();
    private RosterExportWriter writer = new RosterExportWriter();

    public void setRosterDao(RosterDao rosterDao) {
        this.rosterDao = rosterDao;
    }

    /**
     * SMELL #4: the main method catches Exception, prints the stack trace and
     * exits 0. Cron therefore records every failure as a success, which is why
     * the RIVERTON roster was silently empty for eleven days in 2021.
     */
    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.out.println("usage: RosterExportJob <CUSTOMER_CODE> <TERM_CODE> <OUTPUT_DIR>");
                return;
            }
            RosterExportJob job = new RosterExportJob();
            File out = job.run(args[0], args[1], new File(args[2]));
            System.out.println("wrote " + out.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: System.exit(1). Every downstream alert depends on this.
        }
    }

    public File run(String customerCode, String termCode, File outputDir) throws Exception {

        LOG.info("roster export start customer=" + customerCode + " term=" + termCode);

        List students = rosterDao.loadRoster(customerCode, termCode);

        ObjectFactory of = new ObjectFactory();
        RosterExport export = of.createRosterExport();

        RosterHeader header = of.createRosterHeader();
        header.setInstitution(customerCode);
        header.setTermCode(termCode);
        // SMELL #4: static SimpleDateFormat, this time from a batch thread.
        header.setGeneratedOn(DateFormats.formatQuietly(DateFormats.TIMESTAMP, new Date()));
        header.setFormatVersion("1.0");
        export.setHeader(header);

        int count = 0;
        for (Iterator it = students.iterator(); it.hasNext();) {
            Student s = (Student) it.next();

            RosterRow row = of.createRosterRow();
            row.setSid(s.getStudentId());
            row.setSurname(s.getLastName());
            row.setGiven(s.getFirstName());
            row.setProg(s.getProgramCode());
            row.setStat(s.getEnrollmentStatus());

            // SMELL #1: a column that only exists for one institution.
            if (CustomerCodes.RIVERTON.equals(customerCode)) {
                row.setPidm(s.getLegacyBannerPidm());
            }

            // XXX: N+1. One extra query per student, every night, for every
            // institution. 41k queries for SUMMIT alone.
            List crns = rosterDao.loadCrns(customerCode, s.getStudentId(), termCode);
            for (Iterator ci = crns.iterator(); ci.hasNext();) {
                row.getCrn().add((String) ci.next());
            }

            export.getRosterRow().add(row);
            count++;
        }

        RosterFooter footer = of.createRosterFooter();
        footer.setRowCount(count);
        // TODO: the checksum is not a checksum.
        footer.setChecksum("ROWS-" + count);
        export.setFooter(footer);

        String fileName = customerCode.toLowerCase() + "-roster-" + termCode + ".xml";
        File target = new File(outputDir, fileName);
        writer.write(export, target);

        LOG.info("roster export done rows=" + count + " file=" + target.getAbsolutePath());
        return target;
    }
}
