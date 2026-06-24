package com.techmart.ejb.session.stateful;

import com.techmart.common.enums.CheckoutStep;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import lombok.Getter;
import lombok.extern.java.Log;

import java.io.Serializable;

@Stateful
@Log
public class CheckoutWizardBean implements Serializable {

    @Getter
    private CheckoutStep currentStep;

    @Getter
    private String shippingAddress;

    @Getter
    private String billingAddress;

    @Getter
    private String paymentToken;

    private boolean useSameAddressForBilling;

    @PostConstruct
    public void init() {
        this.currentStep = CheckoutStep.SHIPPING;
        log.info("CheckoutWizardBean initialized.");
    }

    @PrePassivate
    public void onPassivate() {
        log.info("Passivating CheckoutWizard at step: " + currentStep);
    }

    @PostActivate
    public void onActivate() {
        log.info("Activating CheckoutWizard at step: " + currentStep);
    }

    @PreDestroy
    public void destroy() {
        log.info("CheckoutWizardBean destroyed.");
    }


    public void setShippingAddress(String address) {
        this.shippingAddress = address;
        this.currentStep = CheckoutStep.BILLING;
    }

    public void setBillingAddress(String address, boolean sameAsShipping) {
        this.billingAddress = sameAsShipping ? this.shippingAddress : address;
        this.useSameAddressForBilling = sameAsShipping;
        this.currentStep = CheckoutStep.PAYMENT;
    }

    public void setPaymentToken(String token) {
        this.paymentToken = token;
        this.currentStep = CheckoutStep.REVIEW;
    }

    @Remove
    public void completeCheckout() {
        this.currentStep = CheckoutStep.COMPLETED;
        log.info("Checkout wizard completed. Destroying bean.");
    }

    @Remove
    public void cancelCheckout() {
        log.info("Checkout cancelled. Destroying bean.");
    }
}
