package amir.code.assessment.wallet.transaction;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * This is a Account class storing balance of an account which can be shared by 3 different methods
 * In order to avoid wrong and unexpected balance when accessing (reading the balance value) or updating (writing the balance value)
 * We need to make sure at a time only one of these methods can read or update the balance, this is done by using synchronized
 * where it locks the shared resource (balance) while they could own it and then release it for the other method calls in a
 * multi-threading environment.
 *
 * We need to make sure only non-zero positive funds can be credited or debited.
 * We also need to make sure only when balance is not negative we can update balance while debiting funds amount.
 *
 * In the beginning double primitive was used for balance but since tests were failing due to the mismatch fractional digits numbers when
 * comparing, BigDecimal is used. And also it should support very very big decimal number as well.
 *
 */
class Account {

    private BigDecimal balance;

    Account(){
        this.balance = BigDecimal.ZERO;
    }

    synchronized BigDecimal getBalance(){
        return balance;
    }

    /**
     *
     * @param funds .
     * @return
     *  return 0: if credited successfully
     *  return -1: if funds is NOT a non-zero positive number
     */
    synchronized int credit(BigDecimal funds) {
        if (funds.compareTo(BigDecimal.ZERO) > 0) {
            balance = balance.add(funds, MathContext.UNLIMITED);
            return 0;
        }
        return -1;
    }

    /**
     *
     * @param funds .
     * @return
     *  return 0: if debited successfully
     *  return -1: if funds is NOT a non-zero positive number
     *  return -2: if not enough money exist to debit expected amount of funds
     */
    synchronized int debit(BigDecimal funds) {
        if (funds.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal futureBalance = balance.subtract(funds);
            if (futureBalance.compareTo(BigDecimal.ZERO) >= 0) {
                balance = futureBalance;
                return 0;
            } else {
                return -2;
            }
        }
        return -1;
    }
}