package com.techmart.ejb.jndi;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import lombok.extern.java.Log;

@Stateless
@Log
public class EnvironmentPropertyManager {

    @Resource(name = "env/appName")
    private String appName;

    @Resource(name = "env/maxRetryCount")
    private Integer maxRetryCount;

    @Resource(name = "env/featureFlags/emailEnabled")
    private Boolean emailEnabled;

    @Resource(name = "env/adminAlertQueueJndi")
    private String adminAlertQueueJndi;

    public void displayEnvironmentConfig() {
        log.info("=== Environment Configuration ===");
        log.info("App Name: " + appName);
        log.info("Max Retry Count: " + maxRetryCount);
        log.info("Email Feature Enabled: " + emailEnabled);
        log.info("=================================");
    }

    public String getAppName() {
        return appName;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public Boolean isEmailEnabled() {
        return emailEnabled;
    }

    public String getAdminAlertQueueJndi() {
        return adminAlertQueueJndi;
    }
}
