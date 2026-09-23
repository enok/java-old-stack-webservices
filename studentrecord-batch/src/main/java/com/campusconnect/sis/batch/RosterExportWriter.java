package com.campusconnect.sis.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

import com.campusconnect.sis.batch.generated.RosterExport;

/**
 * Marshals the roster with the SECOND JAXB context in this codebase.
 *
 * XXX: the JAXBContext is rebuilt on every call. JAXBContext construction is
 * expensive and thread safe; this one is neither cached nor shared with the
 * contexts built in com.campusconnect.sis.transform.ResponseShaper.
 */
public class RosterExportWriter {

    private static final Logger LOG = Logger.getLogger(RosterExportWriter.class);

    public void write(RosterExport export, File target) throws Exception {

        if (target.getParentFile() != null && !target.getParentFile().exists()) {
            // TODO: return value ignored.
            target.getParentFile().mkdirs();
        }

        OutputStream out = null;
        try {
            JAXBContext ctx = JAXBContext.newInstance(RosterExport.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            out = new FileOutputStream(target);
            m.marshal(export, out);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // XXX: a failure to flush here loses the tail of the file
                    // and the job still reports success.
                    e.printStackTrace();
                }
            }
        }
        LOG.info("wrote roster export to " + target.getAbsolutePath());
    }
}
