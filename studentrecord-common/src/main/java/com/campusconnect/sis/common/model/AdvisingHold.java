package com.campusconnect.sis.common.model;

import java.io.Serializable;

public class AdvisingHold implements Serializable {

    private static final long serialVersionUID = 20140301L;

    private String holdCode;
    private String holdReason;
    private java.util.Date placedOn;
    private boolean releasable;

    public String getHoldCode() { return holdCode; }
    public void setHoldCode(String holdCode) { this.holdCode = holdCode; }

    public String getHoldReason() { return holdReason; }
    public void setHoldReason(String holdReason) { this.holdReason = holdReason; }

    public java.util.Date getPlacedOn() { return placedOn; }
    public void setPlacedOn(java.util.Date placedOn) { this.placedOn = placedOn; }

    public boolean isReleasable() { return releasable; }
    public void setReleasable(boolean releasable) { this.releasable = releasable; }
}
