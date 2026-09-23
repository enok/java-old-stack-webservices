package com.campusconnect.sis.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.exception.StudentServiceException;
import com.campusconnect.sis.common.model.CustomerCodes;
import com.campusconnect.sis.common.model.Student;
import com.campusconnect.sis.common.service.StudentRecordFacade;
import com.campusconnect.sis.common.util.DateFormats;
import com.campusconnect.sis.common.util.LegacyStringUtils;

/**
 * The one "REST-ish" endpoint, served by cxf-rt-frontend-jaxrs on the same CXF
 * bus as the SOAP service.
 *
 * It is REST-ish and not REST:
 *   - it is a GET that returns hand-built XML, not JSON, because the
 *     CampusConnect web monolith (sibling repo java-old-stack-web) screen-
 *     scrapes it with XPath;
 *   - it returns HTTP 200 with an empty &lt;student/&gt; element when the
 *     student is unknown, mirroring SMELL #5a from the SOAP side;
 *   - it has no auth at all. SharedSecretAuthHandler is a JAX-WS handler and
 *     is not in this path. The endpoint is "protected" by being bound to the
 *     internal listener.
 *
 * SMELL #2 again: this whole response is built by string concatenation.
 * SMELL #1 again: it branches on the customer code twice.
 *
 * XXX: this endpoint was added in 2016 as a two-day stopgap.
 */
@Path("/students")
public class LegacyStudentResource {

    private static final Logger LOG = Logger.getLogger(LegacyStudentResource.class);

    private StudentRecordFacade facade;

    public void setFacade(StudentRecordFacade facade) {
        this.facade = facade;
    }

    @GET
    @Path("/{customerCode}/{studentId}")
    @Produces("application/xml")
    public Response getStudent(@PathParam("customerCode") String customerCode,
                               @PathParam("studentId") String studentId,
                               @QueryParam("includeHolds") String includeHolds) {

        LOG.info("REST getStudent " + customerCode + "/" + studentId);

        Student s = null;
        try {
            s = facade.getStudent(customerCode, studentId,
                    "true".equalsIgnoreCase(includeHolds));
        } catch (StudentServiceException sse) {
            // SMELL #4: swallow, print, carry on.
            sse.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (s == null) {
            // Mirrors SMELL #5a: 200 OK with an empty document.
            return Response.ok("<student/>").build();
        }

        StringBuffer xml = new StringBuffer(256);
        xml.append("<student customerCode=\"");
        xml.append(LegacyStringUtils.escapeXml(s.getCustomerCode()));
        xml.append("\">");
        xml.append("<studentId>").append(LegacyStringUtils.escapeXml(s.getStudentId())).append("</studentId>");
        xml.append("<lastName>").append(LegacyStringUtils.escapeXml(s.getLastName())).append("</lastName>");
        xml.append("<firstName>").append(LegacyStringUtils.escapeXml(s.getFirstName())).append("</firstName>");

        // SMELL #1: the date format depends on who is asking.
        if (CustomerCodes.RIVERTON.equals(customerCode)) {
            xml.append("<dateOfBirth>");
            xml.append(LegacyStringUtils.nullToEmpty(
                    DateFormats.formatQuietly(DateFormats.RIVERTON, s.getDateOfBirth())));
            xml.append("</dateOfBirth>");
            xml.append("<rivertonCampusId>");
            xml.append(LegacyStringUtils.escapeXml(s.getRivertonCampusId()));
            xml.append("</rivertonCampusId>");
        } else if (CustomerCodes.SUMMIT.equals(customerCode)) {
            xml.append("<dateOfBirth>");
            xml.append(LegacyStringUtils.nullToEmpty(
                    DateFormats.formatQuietly(DateFormats.SUMMIT, s.getDateOfBirth())));
            xml.append("</dateOfBirth>");
        } else {
            xml.append("<dateOfBirth>");
            xml.append(LegacyStringUtils.nullToEmpty(
                    DateFormats.formatQuietly(DateFormats.ISO, s.getDateOfBirth())));
            xml.append("</dateOfBirth>");
        }

        xml.append("<programCode>").append(LegacyStringUtils.escapeXml(s.getProgramCode())).append("</programCode>");
        xml.append("<enrollmentStatus>").append(LegacyStringUtils.escapeXml(s.getEnrollmentStatus())).append("</enrollmentStatus>");
        xml.append("</student>");

        return Response.ok(xml.toString()).build();
    }

    /**
     * TODO: this was going to be the health check. It reports OK
     * unconditionally, including when MySQL is down, and the load balancer
     * uses it.
     */
    @GET
    @Path("/ping")
    @Produces("application/xml")
    public Response ping() {
        return Response.ok("<pong status=\"OK\"/>").build();
    }
}
