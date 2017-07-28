
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class TestChaincode extends ChaincodeBase {

    @Override
    public Response init(ChaincodeStub chaincodeStub) {

        List<String> args = chaincodeStub.getStringArgs();

        switch (args.get(0)) {
            case "init":
                return init(chaincodeStub, args.stream().skip(0).toArray(String[]::new));
            default:
                return newErrorResponse(format("Unknown function: %s", args.get(0)));
        }
    }

    @Override
    public Response invoke(ChaincodeStub chaincodeStub) {
        final List<String> argList = chaincodeStub.getStringArgs();
        final String function = argList.get(0);
        final String[] args = argList.stream().skip(1).toArray(String[]::new);

        switch (function) {
            case "init":
                return init(chaincodeStub, args);
            case "invoke":
                return invoke(chaincodeStub, args);
            case "transfer":
                return transfer(chaincodeStub, args);
            case "put":
                for (int i = 0; i < args.length; i += 2)
                    chaincodeStub.putStringState(args[i], args[i + 1]);
                return newSuccessResponse();
            case "del":
                for (String arg : args)
                    chaincodeStub.delState(arg);
                return newSuccessResponse();
            case "query":
                return query(chaincodeStub, args);
            default:
                return newErrorResponse(newErrorJson("Unknown function: %s", function));
        }
    }
    
    private Response init(ChaincodeStub stub, String[] args) {
        if (args.length != 4) throw new IllegalArgumentException("Incorrect number of arguments. Expecting: init(account1, amount1, account2, amount2 )");

        final String account1Key = args[0];
        final String account1Balance = args[1];
        final String account2Key = args[2];
        final String account2Balance = args[3];

        stub.putStringState(account1Key, new Integer(account1Balance).toString());
        stub.putStringState(account2Key, new Integer(account2Balance).toString());

        return newSuccessResponse();
    }

    private Response invoke(ChaincodeStub stub, String[] args) {
        System.out.println("ENTER invoke with args: " + Arrays.toString(args));

        if (args.length < 2) throw new IllegalArgumentException("Incorrect number of arguments. Expecting at least 2, got " + args.length);
        final String subFunction = args[0];
        final String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (subFunction) {
            case "move":
                return transfer(stub, subArgs);
            case "query":
                return query(stub, subArgs);
            case "delete":
                for (String arg : args)
                    stub.delState(arg);
                return newSuccessResponse();
            default:
                return newErrorResponse(newErrorJson("Unknown invoke sub-function: %s", subFunction));
        }
    }

    private Response transfer(ChaincodeStub stub, String[] args) {
        if (args.length != 3) throw new IllegalArgumentException("Incorrect number of arguments. Expecting: transfer(from, to, amount)");
        final String fromKey = args[0];
        final String toKey = args[1];
        final String amount = args[2];

        // get state of the from/to keys
        final String fromKeyState = stub.getStringState(fromKey);
        final String toKeyState = stub.getStringState(toKey);

        // parse states as integers
        int fromAccountBalance = Integer.parseInt(fromKeyState);
        int toAccountBalance = Integer.parseInt(toKeyState);

        // parse the transfer amount as an integer
        int transferAmount = Integer.parseInt(amount);

        // make sure the transfer is possible
        if (transferAmount > fromAccountBalance) {
            throw new IllegalArgumentException("Insufficient asset holding value for requested transfer amount.");
        }

        // perform the transfer
        int newFromAccountBalance = fromAccountBalance - transferAmount;
        int newToAccountBalance = toAccountBalance + transferAmount;

        stub.putStringState(fromKey, Integer.toString(newFromAccountBalance));
        stub.putStringState(toKey, Integer.toString(newToAccountBalance));

        return newSuccessResponse(String.format("Successfully transferred %d assets from %s to %s.", transferAmount, fromKey, toKey));
    }

    private Response query(ChaincodeStub stub, String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Incorrect number of arguments. Expecting: query(account)");

        final String accountKey = args[0];

        return newSuccessResponse(String.valueOf(Integer.parseInt(stub.getStringState(accountKey))));

    }

    private String newErrorJson(final String message, final Object... args) {
        return newErrorJson(null, message, args);
    }

    private String newErrorJson(final Throwable throwable, final String message, final Object... args) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (message != null)
            builder.add("Error", String.format(message, args));
        if (throwable != null) {
            final StringWriter buffer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(buffer));
            builder.add("Stacktrace", buffer.toString());
        }
        return builder.build().toString();
    }
}
