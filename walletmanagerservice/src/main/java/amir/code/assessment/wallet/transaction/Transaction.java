package amir.code.assessment.wallet.transaction;

import java.math.BigDecimal;
import java.util.Date;

public class Transaction {

    private String transactionId;
    private String externalTransactionId;
    private String transactionType;
    private BigDecimal funds;
    private Long accountId;
    private Date transactionDate;

    public Transaction(String transactionId,
                       String externalTransactionId,
                       String transactionType,
                       BigDecimal funds,
                       Long accountId) {
        this.transactionId = transactionId;
        this.externalTransactionId = externalTransactionId;
        this.transactionType = transactionType;
        this.funds = funds;
        this.accountId = accountId;
        this.transactionDate = new Date();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public BigDecimal getFunds() {
        return funds;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }
}
