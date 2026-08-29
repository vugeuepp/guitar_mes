package com.example.guitarmes.process.analysis;

public class ComponentStatusCountResponse {

    private String status;
    private String displayName;
    private long count;
    private String cssClass;

    public ComponentStatusCountResponse() {
    }

    public ComponentStatusCountResponse(
            String status,
            String displayName,
            long count,
            String cssClass) {

        this.status = status;
        this.displayName = displayName;
        this.count = count;
        this.cssClass = cssClass;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status) {

        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(
            String displayName) {

        this.displayName = displayName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(
            long count) {

        this.count = count;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(
            String cssClass) {

        this.cssClass = cssClass;
    }
}