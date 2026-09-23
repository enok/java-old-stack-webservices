package com.campusconnect.sis.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * One of only THREE tests in this repository.
 *
 * This is the point, not an oversight. There is no characterization coverage
 * of the SOAP contract, no coverage of the per-institution mappers, and no
 * coverage of the DAO. Before anything in docs/MODERNIZATION-BACKLOG.md can be
 * attempted, that safety net has to be built from the fixtures in
 * studentrecord-service/src/test/resources/soap/.
 */
public class LegacyStringUtilsTest {

    @Test
    public void isBlankHandlesNullAndWhitespace() {
        assertTrue(LegacyStringUtils.isBlank(null));
        assertTrue(LegacyStringUtils.isBlank("   "));
        assertFalse(LegacyStringUtils.isBlank("x"));
    }

    @Test
    public void escapeXmlEscapesTheFiveEntities() {
        assertEquals("&lt;a&gt;", LegacyStringUtils.escapeXml("<a>"));
        assertEquals("O&apos;Hara", LegacyStringUtils.escapeXml("O'Hara"));
        assertEquals("R&amp;D", LegacyStringUtils.escapeXml("R&D"));
    }
}
