package com.campusconnect.sis.transform;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.model.CustomerCodes;
import com.campusconnect.sis.common.util.LegacyStringUtils;
import com.campusconnect.sis.ws.common.AdvisingHold;
import com.campusconnect.sis.ws.messages.GetAdvisingHoldsResponse;
import com.campusconnect.sis.ws.messages.GetStudentResponse;

/**
 * SMELL #2: per-customer response shaping, two different ways, in one class.
 *
 *  (a) RIVERTON: marshal the JAXB response back to XML, push it through
 *      xslt/riverton-student.xsl, and hand the result back as a String.
 *      A round trip through the marshaller on every call.
 *
 *  (b) SUMMIT: build XML by STRING CONCATENATION in
 *      {@link #buildSummitAuditXml}, relying on a hand-written escaper that
 *      does not handle control characters. The result is stuffed into an
 *      xs:string element, so it arrives at the consumer double-escaped.
 *
 * XXX: TransformerFactory is created per call and the stylesheet is re-parsed
 * every time. Under the RIVERTON load this is measurable. A cached Templates
 * instance is the obvious fix and is explicitly NOT safe to do before the
 * characterization tests exist, because the current code silently tolerates a
 * malformed stylesheet by returning null.
 */
public class ResponseShaper {

    private static final Logger LOG = Logger.getLogger(ResponseShaper.class);

    private static final String RIVERTON_XSL = "/xslt/riverton-student.xsl";
    private static final String SUMMIT_XSL    = "/xslt/summit-hold-summary.xsl";

    /**
     * SMELL #1: branching on the customer code, inside the shaper.
     *
     * @return the RIVERTON-shaped XML, or <code>null</code> for anybody else.
     */
    public String shapeGetStudent(String customerCode, GetStudentResponse response) {
        if (!CustomerCodes.RIVERTON.equals(customerCode)) {
            return null;
        }
        InputStream xsl = null;
        try {
            JAXBContext ctx = JAXBContext.newInstance(GetStudentResponse.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            StringWriter marshalled = new StringWriter();
            m.marshal(response, marshalled);

            xsl = ResponseShaper.class.getResourceAsStream(RIVERTON_XSL);
            if (xsl == null) {
                LOG.error("missing stylesheet " + RIVERTON_XSL);
                return null;
            }
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer(new StreamSource(xsl));
            StringWriter out = new StringWriter();
            t.transform(new StreamSource(new StringReader(marshalled.toString())),
                        new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            // SMELL #4: catch(Exception) + printStackTrace + return null.
            // A broken stylesheet becomes a silently missing element.
            e.printStackTrace();
            return null;
        } finally {
            if (xsl != null) {
                try {
                    xsl.close();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }
    }

    /**
     * SMELL #2(a) applied to holds: SUMMIT's summary blob, produced by XSLT
     * and returned as an escaped string inside sm:summitHoldSummaryXml.
     */
    public String shapeSummitHoldSummary(String customerCode, GetAdvisingHoldsResponse response) {
        if (!CustomerCodes.SUMMIT.equals(customerCode)) {
            return null;
        }
        InputStream xsl = null;
        try {
            JAXBContext ctx = JAXBContext.newInstance(GetAdvisingHoldsResponse.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            StringWriter marshalled = new StringWriter();
            m.marshal(response, marshalled);

            xsl = ResponseShaper.class.getResourceAsStream(SUMMIT_XSL);
            if (xsl == null) {
                LOG.error("missing stylesheet " + SUMMIT_XSL);
                return null;
            }
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer(new StreamSource(xsl));
            StringWriter out = new StringWriter();
            t.transform(new StreamSource(new StringReader(marshalled.toString())),
                        new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (xsl != null) {
                try {
                    xsl.close();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }
    }

    /**
     * SMELL #2(b): XML BUILT BY STRING CONCATENATION.
     *
     * This produces the body of sm:auditXml for getSummitDegreeAudit. There is
     * no schema for it, no marshaller, and no test. The only protection
     * against a malformed document is
     * {@link LegacyStringUtils#escapeXml(String)}, which does not handle
     * control characters or unpaired surrogates. A student record containing
     * a stray 0x0B byte produces a SOAP response that SUMMIT's parser rejects
     * with "invalid character", and there is nothing in the logs to explain it.
     *
     * @param holds raw List of {@link AdvisingHold} (the JAXB type)
     */
    public String buildSummitAuditXml(String studentId, String catalogYear, List holds) {

        StringBuffer xml = new StringBuffer(512);
        xml.append("<DegreeAudit");
        xml.append(" studentId=\"").append(LegacyStringUtils.escapeXml(studentId)).append("\"");
        xml.append(" catalogYear=\"").append(LegacyStringUtils.escapeXml(catalogYear)).append("\"");
        xml.append(">");

        xml.append("<Holds count=\"").append(holds == null ? 0 : holds.size()).append("\">");
        if (holds != null) {
            for (Iterator it = holds.iterator(); it.hasNext();) {
                AdvisingHold h = (AdvisingHold) it.next();
                xml.append("<Hold code=\"");
                xml.append(LegacyStringUtils.escapeXml(h.getHoldCode()));
                xml.append("\">");
                // XXX: the reason is NOT escaped here. It is escaped three
                // lines above for the code. Nobody noticed for eight years.
                xml.append(LegacyStringUtils.nullToEmpty(h.getHoldReason()));
                xml.append("</Hold>");
            }
        }
        xml.append("</Holds>");

        // TODO: the requirements section is a stub. SUMMIT has been told
        // "next quarter" since 2019.
        xml.append("<Requirements status=\"NOT_IMPLEMENTED\"/>");

        xml.append("</DegreeAudit>");
        return xml.toString();
    }
}
