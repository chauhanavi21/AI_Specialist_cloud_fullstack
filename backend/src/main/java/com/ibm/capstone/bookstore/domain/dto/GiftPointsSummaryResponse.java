package com.ibm.capstone.bookstore.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class GiftPointsSummaryResponse {
    private Integer currentBalance;
    private String pointValueRate = "10 points = $1.00 USD";
    private List<GiftPointsTransactionDto> history = new ArrayList<>();

    public GiftPointsSummaryResponse() {}

    public GiftPointsSummaryResponse(Integer currentBalance, List<GiftPointsTransactionDto> history) {
        this.currentBalance = currentBalance;
        this.pointValueRate = "10 points = $1.00 USD";
        this.history = history;
    }

    public Integer getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(Integer currentBalance) { this.currentBalance = currentBalance; }

    public String getPointValueRate() { return pointValueRate; }
    public void setPointValueRate(String pointValueRate) { this.pointValueRate = pointValueRate; }

    public List<GiftPointsTransactionDto> getHistory() { return history; }
    public void setHistory(List<GiftPointsTransactionDto> history) { this.history = history; }
}
