package com.techmart.ejb.jndi;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import lombok.extern.java.Log;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Singleton
@Startup
@Log
public class JNDIConfigurationManager {

    private Context initialContext;

    @PostConstruct
    public void init() {
        try {
            this.initialContext = new InitialContext();
            log.info("JNDIConfigurationManager initialized.");
        } catch (NamingException e) {
            log.severe("Failed to initialize JNDI Context: " + e.getMessage());
        }
    }

    public <T> T lookupDynamicResource(String jndiName, Class<T> type) {
        try {
            log.info("Performing dynamic manual JNDI lookup for: " + jndiName);
            return type.cast(initialContext.lookup(jndiName));
        } catch (NamingException e) {
            log.severe("Dynamic lookup failed for " + jndiName + ": " + e.getMessage());
            return null;
        }
    }
}
