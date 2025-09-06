package jp.readscape.consumer.domain.orders.model;

public enum PaymentMethod {
    CREDIT_CARD("クレジットカード"),
    DEBIT_CARD("デビットカード"),
    BANK_TRANSFER("銀行振込"),
    CONVENIENCE_STORE("コンビニ支払い"),
    CASH_ON_DELIVERY("代金引換");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}