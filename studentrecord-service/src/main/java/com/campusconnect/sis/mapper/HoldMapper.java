package com.campusconnect.sis.mapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.campusconnect.sis.common.model.CustomerCodes;
import com.campusconnect.sis.common.util.DateFormats;
import com.campusconnect.sis.ws.common.AdvisingHold;

/**
 * SMELL #1: a third class with customer branching, this time on the date
 * format of placedOn and on whether non-releasable holds are visible at all.
 */
public class HoldMapper {

    /**
     * @param holds raw List of {@link com.campusconnect.sis.common.model.AdvisingHold}
     * @return raw List of {@link AdvisingHold} (the JAXB type)
     */
    public List toDetails(String customerCode, List holds) {
        List out = new ArrayList();
        if (holds == null) {
            return out;
        }
        for (Iterator it = holds.iterator(); it.hasNext();) {
            com.campusconnect.sis.common.model.AdvisingHold src =
                    (com.campusconnect.sis.common.model.AdvisingHold) it.next();

            // XXX: RIVERTON never sees non-releasable holds through SOAP. They
            // get them by nightly file instead. Nobody documented this until now.
            if (CustomerCodes.RIVERTON.equals(customerCode) && !src.isReleasable()) {
                continue;
            }

            AdvisingHold d = new AdvisingHold();
            d.setHoldCode(src.getHoldCode());
            d.setHoldReason(src.getHoldReason());

            if (CustomerCodes.RIVERTON.equals(customerCode)) {
                d.setPlacedOn(DateFormats.formatQuietly(DateFormats.RIVERTON, src.getPlacedOn()));
            } else if (CustomerCodes.SUMMIT.equals(customerCode)) {
                d.setPlacedOn(DateFormats.formatQuietly(DateFormats.SUMMIT, src.getPlacedOn()));
            } else {
                d.setPlacedOn(DateFormats.formatQuietly(DateFormats.ISO, src.getPlacedOn()));
            }

            d.setReleasable(Boolean.valueOf(src.isReleasable()));
            out.add(d);
        }
        return out;
    }
}
