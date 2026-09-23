package com.campusconnect.sis.mapper;

import com.campusconnect.sis.common.model.CustomerCodes;
import com.campusconnect.sis.common.model.Student;
import com.campusconnect.sis.ws.common.StudentDetail;

/**
 * SMELL #1: the central if/else chain.
 *
 * There is no common interface across the three mappers, because
 * {@link RivertonStudentMapper#toDetail} returns a different (sub)type. So the
 * "factory" cannot return a mapper; it has to do the dispatch AND the call.
 * Adding a fourth institution means editing this method, the two DAOs, the
 * endpoint, the shaper and the REST resource.
 */
public final class StudentMapperFactory {

    private static final NorthlakeStudentMapper NORTHLAKE = new NorthlakeStudentMapper();
    private static final RivertonStudentMapper  RIVERTON  = new RivertonStudentMapper();
    private static final SummitStudentMapper    SUMMIT    = new SummitStudentMapper();

    private StudentMapperFactory() {
    }

    /**
     * Maps to the BASE type. Used by findStudentsByTerm for every institution,
     * which is why RIVERTON loses its four extra elements on that operation.
     */
    public static StudentDetail mapForSearch(String customerCode, Student s) {
        if (CustomerCodes.RIVERTON.equals(customerCode)) {
            return RIVERTON.toBaseDetailForSearch(s);
        } else if (CustomerCodes.SUMMIT.equals(customerCode)) {
            return SUMMIT.toDetail(s);
        } else {
            // TODO: LAKESHORE rows also land here and are reported as NORTHLAKE.
            return NORTHLAKE.toDetail(s);
        }
    }

    public static NorthlakeStudentMapper northlake() {
        return NORTHLAKE;
    }

    public static RivertonStudentMapper riverton() {
        return RIVERTON;
    }

    public static SummitStudentMapper summit() {
        return SUMMIT;
    }
}
