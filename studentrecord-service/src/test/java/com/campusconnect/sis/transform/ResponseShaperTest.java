package com.campusconnect.sis.transform;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The ONLY test in the service module.
 *
 * It asserts that the string-concatenated audit document has an opening and a
 * closing tag. It does not parse the result, does not check escaping, does not
 * exercise any of the three institutions, and does not touch the SOAP layer,
 * the mappers, the handler or the DAO.
 *
 * That is the honest state of the safety net. Everything else in this module
 * is covered by nothing at all. The fixtures in src/test/resources/soap/ are
 * the raw material for the coverage that has to exist before
 * docs/MODERNIZATION-BACKLOG.md step 2 can start.
 */
public class ResponseShaperTest {

    @Test
    public void auditXmlIsWellFormedEnoughToLookAtIt() {
        ResponseShaper shaper = new ResponseShaper();
        String xml = shaper.buildSummitAuditXml("S9900771", "2019-2020", null);
        assertTrue(xml.startsWith("<DegreeAudit "));
        assertTrue(xml.endsWith("</DegreeAudit>"));
    }
}
