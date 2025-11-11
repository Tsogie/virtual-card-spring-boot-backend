package net.otgon.backend.dto;


public class RedeemResult {

    private String status;
    private double newBalance;
    private double fareDeducted;

    public RedeemResult(String status, double newBalance, double fareDeducted) {
        this.status = status;
        this.newBalance = newBalance;
        this.fareDeducted = fareDeducted;
    }

    public String getStatus() { return status; }
    public double getNewBalance() { return newBalance; }
    public double getFareDeducted() { return fareDeducted; }
}

