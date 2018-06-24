package amir.code.assessment.wallet.transaction;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/AccountTransactionAPI")
@Produces(MediaType.APPLICATION_JSON)
public class AccountTransactionAPI {
    private static final String TRANSACTION_TYPE = "transactionType";
    private static final String ACCOUNT_ID = "accountId";
    private static final String FUNDS = "funds";
    private static final String EXTERNAL_TRANSACTION_ID = "externalTransactionId";
    private static final String TRANSACTION_ID = "transactionId";
    private static final String MESSAGE = "message";
    private static final String BALANCE = "balance";
    private static final char UNIQUE_TRANSACTION_ID_SEPARATOR = '-';

    private static final String CREDIT = "CREDIT";
    private static final String DEBIT = "DEBIT";

    private static final String COULD_NOT_REGISTER_TRANSACTION = "could not register transaction. ";
    private static final String NOT_ENOUGH_BALANCE = "not enough balance!";
    private static final String UNEXPECTED_ZERO_OR_NEGATIVE_FUNDS_AMOUNT = "unexpected zero or negative funds amount:";
    private static final String UNEXPECTED_FUNDS_VALUE_NUMBER_FORMAT_EXCEPTION = "unexpected funds value, NumberFormatException: ";
    private static final String JSON_EXCEPTION = "JSONException: ";
    private static final String TRANSACTION_REGISTERED_SUCCESSFULLY = "transaction registered successfully";
    private static final String EXCEPTION_CAUGHT = "exception caught: ";
    private static final String UNEXPECTED_TRANSACTION_TYPE = "unexpected transaction type: ";
    private static final String UNEXPECTED_ACCOUNT_ID_ACCOUNT_ID_SHOULD_BE_A_NON_ZERO_POSITIVE_INTEGER_NUMBER = "unexpected accountId: accountId should be a non-zero positive integer number";

    private static final Logger logger = LoggerFactory.getLogger(AccountTransactionAPI.class);

    // singleton class for RestAPI
    private static AccountTransactionAPI INSTANCE = null;

    private AccountTransactionAPI(){}

    /**
     * A Singleton class created for RestAPI which stores a globally unique transactionId counter.
     * @return
     */
    static AccountTransactionAPI getInstance(){
        if (INSTANCE == null){
            INSTANCE = new AccountTransactionAPI();
        }
        return INSTANCE;
    }

    //unique transactionId
    private static AtomicLong transactionId = new AtomicLong();

    private synchronized static String getUniqueTransactionId(String externalTransactionId) {
        return externalTransactionId+UNIQUE_TRANSACTION_ID_SEPARATOR+String.valueOf(transactionId.incrementAndGet());
    }

    /**
     * HashMap data structure is used for fast access to the Account value by its unique key (AccountID).
     * Long should makes sense as number of accounts for players but in case later other types can be used
     * to represent larger range of Integers if still we want to use numbered IDs.
     */
    private static final HashMap<Long, Account> accounts = new HashMap<>();

    /**
     * transactions will store history of registered transactions. There is a one-2-many relationship
     * between one account and its transactions. meaning that each transaction should be connected to
     * only and only one account but there can be an account to have zero or many transactions connected to.
     *
     * Key is the transactionId where a transaction can be found using this unique identifier.
     */
    private static final HashMap<String, Transaction> transactions = new HashMap<>();

    /**
     *
     * @param accountId . They key to create a new unique mapped Account
     * @return
     */
    @POST
    @Path("/register-account/accountId/{accountId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerAccount(
            @PathParam(ACCOUNT_ID) long accountId) {

        try {
            if (accountId <= 0)
                return getFailResponse(400, UNEXPECTED_ACCOUNT_ID_ACCOUNT_ID_SHOULD_BE_A_NON_ZERO_POSITIVE_INTEGER_NUMBER);
            if (accounts.containsKey(accountId))
                return getFailResponse(400,"account with id:"+accountId+" already registered");

            Account account = new Account();
            accounts.put(accountId, account);

            return Response.ok(new JSONObject()
                    .put(ACCOUNT_ID, accountId)
                    .put(BALANCE, account.getBalance()).toString())
                    .build();

        } catch (Exception e) {
            logger.error(EXCEPTION_CAUGHT, e);
        }
        return Response.serverError().build();
    }

    /**
     *
     * @param accountId . They key to get balance of mapped Account
     * @return
     */
    @GET
    @Path("/get-account-balance/accountId/{accountId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountBalance(
            @PathParam(ACCOUNT_ID) long accountId) {

        try {
            if (accounts.containsKey(accountId))
                return Response.ok(new JSONObject()
                        .put(ACCOUNT_ID, accountId)
                        .put(BALANCE, accounts.get(accountId).getBalance()).toString())
                        .build();

            else return getFailResponse(400, "account with id:"+accountId+" does not exist");

        } catch (Exception e) {
            logger.error(EXCEPTION_CAUGHT, e);
        }
        return Response.serverError().build();
    }

    /**
     * This API is to register a CREDIT or DEBIT transaction on one account which is identified by its unique Id.
     * The funds amount can only be a non-zero positive fractional number.
     *
     * @param body . Decided to use request json format body for expected input values (which are not only one or two values).
     *             Also, it will be more secure considering that is a monetary transaction (comparing to URL params)
     *             //TODO: encryption of request body to increase security
     *
     *             Example of body:
     *               {
     *                   "accountId" : "1",
     *                   "transactionType" : "credit",
     *                   "funds" : 43242434.34234244243244423424234432424423432424234242,
     *                   "externalTransactionId" : "2dsff"
     *               }
     * @return
     */
    @POST
    @Path("/register-transaction")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerTransaction(String body)  {

        try {
            try {
                // fetch transaction register info from JSON request body
                JSONObject jsonBody = new JSONObject(body);
                String transactionType = jsonBody.getString(TRANSACTION_TYPE).toUpperCase(); // transaction type
                long accountId = jsonBody.getLong(ACCOUNT_ID); // account id
                BigDecimal funds = new BigDecimal(jsonBody.getString(FUNDS)); // funds
                String externalTransactionId = jsonBody.getString(EXTERNAL_TRANSACTION_ID); // external transaction id

                // check if account exists
                if (!accounts.containsKey(accountId)){
                    return getFailResponse(400, COULD_NOT_REGISTER_TRANSACTION + "account with id:" +accountId+" does not exist");
                }

                // register transaction CREDIT or DEBIT
                int registrationStatus;
                switch (transactionType) {
                    case CREDIT:
                        registrationStatus = accounts.get(accountId).credit(funds);
                        break;
                    case DEBIT:
                        registrationStatus = accounts.get(accountId).debit(funds);
                        break;
                    default:
                        return getFailResponse(400, COULD_NOT_REGISTER_TRANSACTION + UNEXPECTED_TRANSACTION_TYPE +transactionType);
                }

                // update transactions history
                switch (registrationStatus) {
                    case 0:
                        String transactionId = getUniqueTransactionId(externalTransactionId);
                        Transaction registeredTransaction = new Transaction(transactionId, externalTransactionId, transactionType, funds, accountId);

                        // since transactionId is unique simply add it to transactions history (thread-safe)
                        transactions.put(transactionId, registeredTransaction);
                        return getSuccessRegisterResponse(registeredTransaction);
                    case -1:
                        return getFailResponse(400, COULD_NOT_REGISTER_TRANSACTION + UNEXPECTED_ZERO_OR_NEGATIVE_FUNDS_AMOUNT +funds);
                    case -2:
                        return getFailResponse(400, COULD_NOT_REGISTER_TRANSACTION + NOT_ENOUGH_BALANCE);
                    default:
                        break;
                }

            } catch (JSONException e) {
                logger.error(EXCEPTION_CAUGHT, e);
                return getFailResponse(400, COULD_NOT_REGISTER_TRANSACTION + JSON_EXCEPTION +e.getMessage());

            } catch (NumberFormatException e) {
                // Unexpected FUNDS input value
                logger.error(EXCEPTION_CAUGHT, e);
                return getFailResponse(400, COULD_NOT_REGISTER_TRANSACTION + UNEXPECTED_FUNDS_VALUE_NUMBER_FORMAT_EXCEPTION +e.getMessage());
            }
        } catch (Exception e) {
            logger.error(EXCEPTION_CAUGHT, e);
        }
        return Response.serverError().build();
    }

    /**
     *
     * @param transaction . A registered transaction.
     * @return
     * @throws JSONException
     */
    private Response getSuccessRegisterResponse(Transaction transaction) throws JSONException{
        return Response.ok(new JSONObject()
                .put(MESSAGE, TRANSACTION_REGISTERED_SUCCESSFULLY)
                .put(TRANSACTION_ID, transaction.getTransactionId())
                .put(EXTERNAL_TRANSACTION_ID, transaction.getExternalTransactionId())
                .put(TRANSACTION_TYPE, transaction.getTransactionType())
                .put(ACCOUNT_ID, transaction.getAccountId())
                .put(FUNDS, transaction.getFunds())
                .toString())
                .build();
    }

    /**
     *
     * @param errorCode . Http error code. For now (could be more specific in usage later)
     * used only for Bad Request and only 400 meaning not expected information received from client.
     * @param errorMessage . errorMessage.
     * @return A JSON format Response.
     * @throws JSONException
     */
    private Response getFailResponse(int errorCode, String errorMessage) throws JSONException {
        return Response.status(errorCode).entity(new JSONObject().put(MESSAGE, errorMessage).toString()).build();
    }
}
