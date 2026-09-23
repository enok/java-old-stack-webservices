package com.campusconnect.sis.common;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import com.campusconnect.sis.common.model.AdvisingHold;
import com.campusconnect.sis.common.util.DateFormats;

/**
 * SMELL #7: DEAD CLASS.
 *
 * This printed a fixed-width advising hold report to a line printer in the
 * registrar's office at NORTHLAKE. The printer was decommissioned in 2016.
 * Nothing in this repository, and nothing in the sibling repository
 * java-old-stack-web, references this class. It is still compiled, still
 * shipped in the WAR, and still shows up in every dependency audit.
 *
 * It is kept here deliberately: "is this actually dead?" is the first question
 * of the modernization exercise, and the answer needs to be demonstrated by a
 * call-graph, not assumed.
 *
 * XXX: do not delete without checking the NORTHLAKE cron box (nl-batch-01).
 */
public class LegacyAdvisingReportPrinter {

    private static final int PAGE_WIDTH = 132;

    private PrintStream out;

    public LegacyAdvisingReportPrinter(PrintStream out) {
        this.out = out;
    }

    public void printHoldReport(String studentId, List holds) {
        banner();
        out.println(pad("STUDENT: " + studentId, PAGE_WIDTH));
        if (holds == null || holds.isEmpty()) {
            out.println(pad("  NO ACTIVE HOLDS", PAGE_WIDTH));
        } else {
            for (Iterator it = holds.iterator(); it.hasNext();) {
                AdvisingHold h = (AdvisingHold) it.next();
                StringBuffer line = new StringBuffer();
                line.append("  ").append(pad(h.getHoldCode(), 10));
                line.append(pad(DateFormats.formatQuietly(DateFormats.ISO, h.getPlacedOn()), 12));
                line.append(h.getHoldReason());
                out.println(pad(line.toString(), PAGE_WIDTH));
            }
        }
        banner();
        out.flush();
    }

    private void banner() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < PAGE_WIDTH; i++) {
            sb.append('*');
        }
        out.println(sb.toString());
    }

    private static String pad(String s, int width) {
        StringBuffer sb = new StringBuffer(s == null ? "" : s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
