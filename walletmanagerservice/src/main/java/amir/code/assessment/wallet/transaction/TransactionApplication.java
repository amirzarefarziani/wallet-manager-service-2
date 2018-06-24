package amir.code.assessment.wallet.transaction;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class TransactionApplication extends Application<TransactionConfiguration> {

    public static void main(String[] args) throws Exception {
        new TransactionApplication().run(args);
    }
    public void run(TransactionConfiguration transactionConfiguration, Environment environment) throws Exception {
        //Register resource
        AccountTransactionAPI transactionAPI = AccountTransactionAPI.getInstance();
        environment.jersey().register(transactionAPI);
    }
}
