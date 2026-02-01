package com.myapp.identityservice.dto.response;

public class UserDisplayInfo {

    private String canonicalName;
    private boolean isVerified;
    private String viewerAlias;

    public UserDisplayInfo() {}

    public UserDisplayInfo(String canonicalName, boolean isVerified, String viewerAlias) {
        this.canonicalName = canonicalName;
        this.isVerified = isVerified;
        this.viewerAlias = viewerAlias;
    }

    public String getCanonicalName() { return canonicalName; }
    public void setCanonicalName(String canonicalName) { this.canonicalName = canonicalName; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getViewerAlias() { return viewerAlias; }
    public void setViewerAlias(String viewerAlias) { this.viewerAlias = viewerAlias; }
}
