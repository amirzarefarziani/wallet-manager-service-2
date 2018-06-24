package amir.code.assessment.wallet.transaction;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

@RunWith(MockitoJUnitRunner.class)
public class AccountTest {

    // from https://www.piday.org/million/ up to a big enough number of fractional digits used for below PI_NUMBER
    private static final String PI_NUMBER = "3.141592653589793238462643383279502884197169399375105820974944592307816406286208998628034825342117";
    private static final String DEFAULT_FUNDS_VALUE = PI_NUMBER;
    private static final long DEFAULT_ITERATIONS_VALUE = 2_000_000;

    private ExecutorService service = Executors.newFixedThreadPool(2_000);

    @Test
    public void testCreditSync() throws InterruptedException {
        Account account = new Account();

        long iterations = DEFAULT_ITERATIONS_VALUE;
        BigDecimal funds = new BigDecimal(DEFAULT_FUNDS_VALUE);
        BigDecimal expectedBalance = funds.multiply(BigDecimal.valueOf(iterations));

        LongStream.range(0, iterations).forEach(
                count -> service.submit( () -> account.credit(funds)) );
        terminateExecutorService("testCreditSync");

        print("BALANCE", account.getBalance());
        print("EXPECTED BALANCE", expectedBalance);
        Assert.assertEquals(0, account.getBalance().compareTo(expectedBalance));
    }

    @Test
    public void testDebitSync() throws InterruptedException {
        Account account = new Account();

        long iterations = DEFAULT_ITERATIONS_VALUE;
        BigDecimal funds = new BigDecimal(DEFAULT_FUNDS_VALUE);
        BigDecimal initialBalance = funds.multiply(BigDecimal.valueOf(iterations));
        BigDecimal expectedBalance = BigDecimal.ZERO;

        account.credit(initialBalance);

        LongStream.range(0, iterations).forEach(
                count -> service.submit( () -> account.debit(funds)) );
        terminateExecutorService("testDebitSync");

        print("BALANCE", account.getBalance());
        print("EXPECTED BALANCE", expectedBalance);
        Assert.assertEquals(0, account.getBalance().compareTo(expectedBalance));
    }

    @Test
    public void testAccountBalanceSync() throws InterruptedException {
        Account account = new Account();

        long iterations = DEFAULT_ITERATIONS_VALUE;
        BigDecimal funds = new BigDecimal(DEFAULT_FUNDS_VALUE);
        BigDecimal expectedBalance = funds.multiply(BigDecimal.valueOf(iterations));

        //initialBalance==expectedBalance to avoid any unsuccessful DEBIT (multi-threaded computing) meaning that
        //using max possible amount will cover the worst rare test-case when ALL DEBITS executed BEFORE THE FIRST CREDIT
        account.credit(expectedBalance);

        //multi-threaded calls to all 3 types of synchronized methods: CREDIT, DEBIT, GET BALANCE
        LongStream.range(0, iterations).forEach(
                count -> {
                    service.submit( () -> account.credit(funds) );
                    service.submit( account::getBalance);
                    service.submit( () -> account.debit(funds)  );
                    service.submit( account::getBalance);
                });
        terminateExecutorService("testAccountBalanceSync");

        print("BALANCE", account.getBalance());
        print("EXPECTED BALANCE", expectedBalance);
        // check if same initial account balance
        Assert.assertEquals(0, account.getBalance().compareTo(expectedBalance));
    }

    @Test
    public void testCreditFailWhenFundsNotGreaterThanZero() throws InterruptedException {
        Account account = new Account();

        // funds = 0
        Assert.assertEquals(-1, account.credit(BigDecimal.ZERO));
        Assert.assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
        print("BALANCE", account.getBalance());

        // funds = negative number
        BigDecimal minusPi = new BigDecimal('-'+PI_NUMBER);
        Assert.assertEquals(-1, account.credit(minusPi));
        Assert.assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
        print("BALANCE", account.getBalance());


        // funds = positive number
        Assert.assertEquals(0, account.credit(minusPi.negate()));
        Assert.assertEquals(0, account.getBalance().compareTo(minusPi.negate()));
        print("BALANCE", account.getBalance());
    }

    @Test
    public void testDebitFailWhenFundsNotGreaterThanZero() throws InterruptedException {
        Account account = new Account();

        BigDecimal expectedBalance = new BigDecimal(PI_NUMBER);
        account.credit(expectedBalance);

        // funds = 0
        Assert.assertEquals(-1, account.debit(BigDecimal.ZERO));
        Assert.assertEquals(0, account.getBalance().compareTo(expectedBalance));
        print("BALANCE", account.getBalance());

        // funds = negative number
        BigDecimal minusPi = new BigDecimal('-'+PI_NUMBER);
        Assert.assertEquals(-1, account.debit(minusPi));
        Assert.assertEquals(0, account.getBalance().compareTo(expectedBalance));
        print("BALANCE", account.getBalance());


        // funds = positive number
        Assert.assertEquals(0, account.debit(minusPi.negate()));
        Assert.assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
        print("BALANCE", account.getBalance());
    }

    @Test
    public void testDebitFailWhenNotEnoughBalance() throws InterruptedException {
        Account account = new Account();

        BigDecimal Pi = new BigDecimal(PI_NUMBER);

        // balance = 0 and funds =
        Assert.assertEquals(-2, account.debit(Pi));
        Assert.assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
        print("BALANCE", account.getBalance());
    }

    private void terminateExecutorService(String methodName) throws InterruptedException{

        do {
            System.out.println(methodName+": terminating...");
            service.shutdown();
            service.awaitTermination(2000, TimeUnit.MILLISECONDS);
        } while (!service.isTerminated());
        System.out.println(methodName+": terminated!");
    }

    private void print(String name, BigDecimal value){
        System.out.println("The value of "+name+" is: "+value);
    }
}