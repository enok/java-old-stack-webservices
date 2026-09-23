package com.campusconnect.sis.endpoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jws.WebService;

import org.apache.log4j.Logger;

import com.campusconnect.sis.common.exception.StudentServiceException;
import com.campusconnect.sis.common.model.CustomerCodes;
import com.campusconnect.sis.common.model.EnrollmentChangeItem;
import com.campusconnect.sis.common.model.Student;
import com.campusconnect.sis.common.service.StudentRecordFacade;
import com.campusconnect.sis.common.util.DateFormats;
import com.campusconnect.sis.mapper.HoldMapper;
import com.campusconnect.sis.mapper.StudentMapperFactory;
import com.campusconnect.sis.transform.ResponseShaper;
import com.campusconnect.sis.ws.StudentRecordPortType;
import com.campusconnect.sis.ws.StudentServiceFault;
import com.campusconnect.sis.ws.common.AdvisingHold;
import com.campusconnect.sis.ws.common.CustomerCode;
import com.campusconnect.sis.ws.common.EnrollmentChange;
import com.campusconnect.sis.ws.common.StudentDetail;
import com.campusconnect.sis.ws.common.StudentServiceFaultType;
import com.campusconnect.sis.ws.messages.FindStudentsByTermRequest;
import com.campusconnect.sis.ws.messages.FindStudentsByTermResponse;
import com.campusconnect.sis.ws.messages.GetAdvisingHoldsRequest;
import com.campusconnect.sis.ws.messages.GetAdvisingHoldsResponse;
import com.campusconnect.sis.ws.messages.GetRivertonResidencyStatusRequest;
import com.campusconnect.sis.ws.messages.GetRivertonResidencyStatusResponse;
import com.campusconnect.sis.ws.messages.GetStudentRequest;
import com.campusconnect.sis.ws.messages.GetStudentResponse;
import com.campusconnect.sis.ws.messages.GetSummitDegreeAuditRequest;
import com.campusconnect.sis.ws.messages.GetSummitDegreeAuditResponse;
import com.campusconnect.sis.ws.messages.SubmitEnrollmentChangeRequest;
import com.campusconnect.sis.ws.messages.SubmitEnrollmentChangeResponse;

/**
 * The JAX-WS endpoint. Implements the SEI generated from
 * src/main/resources/wsdl/StudentRecordService.wsdl by cxf-codegen-plugin.
 *
 * This class is where every legacy decision in the repository becomes visible
 * at once. It is long, it branches on the customer code five times, it uses
 * two different not-found conventions, and it has no tests.
 */
@WebService(
        serviceName = "StudentRecordService",
        portName = "StudentRecordPort",
        endpointInterface = "com.campusconnect.sis.ws.StudentRecordPortType",
        targetNamespace = "http://campusconnect.example.edu/sis/studentrecord/v1",
        wsdlLocation = "classpath:wsdl/StudentRecordService.wsdl")
public class StudentRecordEndpointImpl implements StudentRecordPortType {

    private static final Logger LOG = Logger.getLogger(StudentRecordEndpointImpl.class);

    private StudentRecordFacade facade;
    private ResponseShaper shaper = new ResponseShaper();
    private HoldMapper holdMapper = new HoldMapper();

    public void setFacade(StudentRecordFacade facade) {
        this.facade = facade;
    }

    // -----------------------------------------------------------------------
    // getStudent
    // -----------------------------------------------------------------------
    /**
     * SMELL #5a: NO FAULT ON NOT FOUND.
     *
     * When the student does not exist this returns an empty
     * getStudentResponse: no studentDetail, no rivertonStudentDetail, no
     * holds. The WSDL declares no fault for this operation at all, so the
     * three institutions each invented their own way of telling "unknown
     * student" apart from "student with no holds". NORTHLAKE checks for a null
     * studentDetail; RIVERTON treats the empty response as a transport error
     * and retries three times; SUMMIT logs it and moves on.
     *
     * Compare with {@link #getAdvisingHolds}, six methods down, which throws a
     * declared fault for exactly the same condition.
     */
    public GetStudentResponse getStudent(GetStudentRequest parameters) {

        GetStudentResponse response = new GetStudentResponse();

        String customerCode = parameters.getCustomerCode() == null
                ? null : parameters.getCustomerCode().value();
        boolean includeHolds = Boolean.TRUE.equals(parameters.isIncludeHolds());

        LOG.info("getStudent customer=" + customerCode + " studentId=" + parameters.getStudentId());

        Student s;
        try {
            s = facade.getStudent(customerCode, parameters.getStudentId(), includeHolds);
        } catch (StudentServiceException sse) {
            // XXX: the declared checked exception is swallowed and turned into
            // the same empty response as "not found".
            sse.printStackTrace();
            return response;
        }

        if (s == null) {
            // SMELL #5a. Empty response, HTTP 200, no fault.
            return response;
        }

        // SMELL #1: the shape of the response depends on the institution.
        if (CustomerCodes.RIVERTON.equals(customerCode)) {
            response.setRivertonStudentDetail(StudentMapperFactory.riverton().toDetail(s));
        } else if (CustomerCodes.SUMMIT.equals(customerCode)) {
            response.setStudentDetail(StudentMapperFactory.summit().toDetail(s));
        } else {
            response.setStudentDetail(StudentMapperFactory.northlake().toDetail(s));
        }

        if (s.getHolds() != null && !s.getHolds().isEmpty()) {
            List mapped = holdMapper.toDetails(customerCode, s.getHolds());
            for (Iterator it = mapped.iterator(); it.hasNext();) {
                response.getHold().add((AdvisingHold) it.next());
            }
        }

        // SMELL #2: the RIVERTON XSLT runs here, and its output is DISCARDED
        // on this operation because the shaped document has nowhere to go in
        // the contract. It is computed on every RIVERTON call anyway, and has
        // been since 2016, purely so the timing stays the same.
        if (CustomerCodes.RIVERTON.equals(customerCode)) {
            String shaped = shaper.shapeGetStudent(customerCode, response);
            LOG.debug("riverton shaped payload length=" + (shaped == null ? -1 : shaped.length()));
        }

        return response;
    }

    // -----------------------------------------------------------------------
    // findStudentsByTerm
    // -----------------------------------------------------------------------
    public FindStudentsByTermResponse findStudentsByTerm(FindStudentsByTermRequest parameters)
            throws StudentServiceFault {

        FindStudentsByTermResponse response = new FindStudentsByTermResponse();

        String customerCode = parameters.getCustomerCode() == null
                ? null : parameters.getCustomerCode().value();

        int maxResults = parameters.getMaxResults() == null
                ? 0 : parameters.getMaxResults().intValue();

        // SMELL #1: a silent per-institution cap.
        if (CustomerCodes.SUMMIT.equals(customerCode) && maxResults == 0) {
            maxResults = 500;
        }

        List found;
        try {
            found = facade.findStudentsByTerm(customerCode, parameters.getTermCode(),
                    parameters.getProgramCode(), maxResults);
        } catch (StudentServiceException sse) {
            throw toFault(sse);
        }

        boolean truncated = false;
        int emitted = 0;
        for (Iterator it = found.iterator(); it.hasNext();) {
            Student s = (Student) it.next();
            if (maxResults > 0 && emitted >= maxResults) {
                truncated = true;
                break;
            }
            StudentDetail d = StudentMapperFactory.mapForSearch(customerCode, s);
            response.getStudentDetail().add(d);
            emitted++;
        }
        response.setTruncated(Boolean.valueOf(truncated));
        return response;
    }

    // -----------------------------------------------------------------------
    // submitEnrollmentChange
    // -----------------------------------------------------------------------
    public SubmitEnrollmentChangeResponse submitEnrollmentChange(SubmitEnrollmentChangeRequest parameters)
            throws StudentServiceFault {

        SubmitEnrollmentChangeResponse response = new SubmitEnrollmentChangeResponse();

        String customerCode = parameters.getCustomerCode() == null
                ? null : parameters.getCustomerCode().value();

        List items = new ArrayList();
        for (Iterator it = parameters.getChange().iterator(); it.hasNext();) {
            EnrollmentChange c = (EnrollmentChange) it.next();
            EnrollmentChangeItem item = new EnrollmentChangeItem();
            item.setCourseReferenceNumber(c.getCourseReferenceNumber());
            item.setCreditHours(c.getCreditHours());
            item.setEffectiveDate(c.getEffectiveDate());

            // SMELL #1: SUMMIT spells DROP as WITHDRAW. Translated here, in the
            // endpoint, rather than in a mapper or an adapter.
            if (CustomerCodes.SUMMIT.equals(customerCode)
                    && EnrollmentChangeItem.ACTION_WITHDRAW.equals(c.getAction())) {
                item.setAction(EnrollmentChangeItem.ACTION_DROP);
            } else {
                item.setAction(c.getAction());
            }
            items.add(item);
        }

        try {
            String confirmation = facade.submitEnrollmentChange(customerCode,
                    parameters.getStudentId(), parameters.getTermCode(), items);
            response.setAccepted(true);
            response.setConfirmationId(confirmation);
            response.setMessage("accepted");
        } catch (StudentServiceException sse) {
            throw toFault(sse);
        }
        // NOTE: a DataAccessException from EnrollmentDao is UNCHECKED and is
        // NOT caught here. It reaches CXF unwrapped. See SMELL #5b.
        return response;
    }

    // -----------------------------------------------------------------------
    // getAdvisingHolds
    // -----------------------------------------------------------------------
    /**
     * SMELL #5a, the other half: this DOES throw a declared fault when the
     * student is unknown, unlike {@link #getStudent}.
     */
    public GetAdvisingHoldsResponse getAdvisingHolds(GetAdvisingHoldsRequest parameters)
            throws StudentServiceFault {

        GetAdvisingHoldsResponse response = new GetAdvisingHoldsResponse();

        String customerCode = parameters.getCustomerCode() == null
                ? null : parameters.getCustomerCode().value();

        List holds;
        try {
            holds = facade.getAdvisingHolds(customerCode, parameters.getStudentId());
        } catch (StudentServiceException sse) {
            throw toFault(sse);
        }

        List mapped = holdMapper.toDetails(customerCode, holds);
        for (Iterator it = mapped.iterator(); it.hasNext();) {
            response.getHold().add((AdvisingHold) it.next());
        }

        // SMELL #2: SUMMIT's escaped-XML-in-a-string summary.
        if (CustomerCodes.SUMMIT.equals(customerCode)) {
            response.setSummitHoldSummaryXml(shaper.shapeSummitHoldSummary(customerCode, response));
        }

        return response;
    }

    // -----------------------------------------------------------------------
    // getSummitDegreeAudit  (SUMMIT only)
    // -----------------------------------------------------------------------
    /**
     * SMELL #1: an operation in the shared contract that exactly one
     * institution may call. NORTHLAKE and RIVERTON get a fault.
     *
     * SMELL #5b: the body deliberately does NOT guard against a
     * DataAccessException. When MySQL is unavailable the unchecked exception
     * propagates into CXF, which renders it as a soap:Fault whose faultstring
     * contains the JDBC message and whose detail contains the stack trace,
     * including the fully interpolated SQL from StudentDao. That is an
     * information disclosure bug as well as an ugly one.
     */
    public GetSummitDegreeAuditResponse getSummitDegreeAudit(GetSummitDegreeAuditRequest parameters)
            throws StudentServiceFault {

        String customerCode = parameters.getCustomerCode() == null
                ? null : parameters.getCustomerCode().value();

        if (!CustomerCodes.SUMMIT.equals(customerCode)) {
            StudentServiceFaultType info = new StudentServiceFaultType();
            info.setFaultCode("SIS-4003");
            info.setFaultMessage("getSummitDegreeAudit is not available for " + customerCode);
            throw new StudentServiceFault("getSummitDegreeAudit is not available for "
                    + customerCode, info);
        }

        GetSummitDegreeAuditResponse response = new GetSummitDegreeAuditResponse();
        response.setStudentId(parameters.getStudentId());

        List holds;
        try {
            holds = facade.getAdvisingHolds(customerCode, parameters.getStudentId());
        } catch (StudentServiceException sse) {
            throw toFault(sse);
        }

        List mapped = holdMapper.toDetails(customerCode, holds);

        // SMELL #2(b): string-concatenated XML.
        response.setAuditXml(shaper.buildSummitAuditXml(parameters.getStudentId(),
                parameters.getAuditCatalogYear(), mapped));

        // SMELL #4: a static SimpleDateFormat, called from a CXF worker thread.
        response.setGeneratedOn(DateFormats.formatQuietly(DateFormats.SUMMIT, new Date()));

        return response;
    }

    // -----------------------------------------------------------------------
    // getRivertonResidencyStatus  (RIVERTON only)  -- RIVERTON FORK
    // -----------------------------------------------------------------------
    /**
     * SMELL #1: a second institution-only operation bolted onto the shared
     * contract on this branch and never merged back to master.
     *
     * XXX: residency is read from the student row when it is there, and
     * otherwise guessed from the campus code. Nobody remembers which campuses
     * were in state when this was written.
     */
    public GetRivertonResidencyStatusResponse getRivertonResidencyStatus(
            GetRivertonResidencyStatusRequest parameters) throws StudentServiceFault {

        String customerCode = parameters.getCustomerCode() == null
                ? null : parameters.getCustomerCode().value();

        if (!CustomerCodes.RIVERTON.equals(customerCode)) {
            StudentServiceFaultType info = new StudentServiceFaultType();
            info.setFaultCode("SIS-4004");
            info.setFaultMessage("getRivertonResidencyStatus is not available for " + customerCode);
            throw new StudentServiceFault("getRivertonResidencyStatus is not available for "
                    + customerCode, info);
        }

        Student student;
        try {
            student = facade.getStudent(customerCode, parameters.getStudentId(), false);
        } catch (StudentServiceException sse) {
            throw toFault(sse);
        }

        GetRivertonResidencyStatusResponse response = new GetRivertonResidencyStatusResponse();
        response.setStudentId(parameters.getStudentId());

        if (student == null) {
            response.setResidencyIndicator("UNKNOWN");
        } else if (student.getResidencyIndicator() != null) {
            response.setResidencyIndicator(student.getResidencyIndicator());
        } else if ("RIV-MAIN".equals(student.getCampusCode())) {
            response.setResidencyIndicator("IN_STATE");
        } else {
            response.setResidencyIndicator("OUT_OF_STATE");
        }

        // SMELL #4: static SimpleDateFormat, called from a CXF worker thread.
        response.setResidencyReviewedOn(
                DateFormats.formatQuietly(DateFormats.RIVERTON, new Date()));

        return response;
    }

    // -----------------------------------------------------------------------

    /**
     * XXX: the checked exception's message is copied verbatim into the fault
     * that goes on the wire. Several of those messages contain the SQL-shaped
     * detail they were constructed with. SMELL #5b.
     */
    private static StudentServiceFault toFault(StudentServiceException sse) {
        StudentServiceFaultType info = new StudentServiceFaultType();
        info.setFaultCode(sse.getFaultCode());
        info.setFaultMessage(sse.getMessage());
        info.setFaultDetail(String.valueOf(sse.getCause()));
        return new StudentServiceFault(sse.getMessage(), info, sse);
    }
}
