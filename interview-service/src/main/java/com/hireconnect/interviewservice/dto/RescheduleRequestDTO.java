package com.hireconnect.interviewservice.dto;

public class RescheduleRequestDTO {

    private String requestedInterviewDate;
    private String reason;
    private String notes;

    public RescheduleRequestDTO() {}

    public String getRequestedInterviewDate() { return requestedInterviewDate; }
    public void setRequestedInterviewDate(String requestedInterviewDate) { this.requestedInterviewDate = requestedInterviewDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
