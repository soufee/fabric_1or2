import org.apache.commons.codec.binary.Base64;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.RegistrationException;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.codec.CharEncoding.UTF_8;

/**
 * Created by shomakhov on 09.08.2017.
 */
public class HashMaker {
    public static String makeHash(String string){
        return Base64.decodeBase64(string).toString();
    }

    public static void sendTransInit() {
        FCUser org1_user = Main.org1_user;
        try {
            TransactionProposalRequest instantiateProposalRequest = Main.client.newTransactionProposalRequest();
            instantiateProposalRequest.setProposalWaitTime(15L);
            instantiateProposalRequest.setChaincodeID(Main.chaincodeID);
            instantiateProposalRequest.setFcn("Init");

            instantiateProposalRequest.setUserContext(org1_user);
            //  instantiateProposalRequest.setArgs(new String[]{"add", "val0", "val1"});
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File("src\\main\\env\\chaincodeendorsementpolicy.yaml"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);
            Collection<ProposalResponse> transactionPropResp =   Main.channel.sendTransactionProposal(instantiateProposalRequest, Main.channel.getPeers());

            for (ProposalResponse response:transactionPropResp) {
                System.out.println(response.getStatus()+" : "+response.getMessage());
            }

        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public static void sendTransAdd() {
        FCUser org1_user = Main.org1_user;
        try {
            TransactionProposalRequest instantiateProposalRequest = Main.client.newTransactionProposalRequest();
            instantiateProposalRequest.setProposalWaitTime(15L);
            instantiateProposalRequest.setChaincodeID(Main.chaincodeID);
            instantiateProposalRequest.setFcn("add");

            instantiateProposalRequest.setUserContext(org1_user);
             instantiateProposalRequest.setArgs(new String[]{"val0", "val1"});
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File("src\\main\\env\\chaincodeendorsementpolicy.yaml"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);
            Collection<ProposalResponse> transactionPropResp =   Main.channel.sendTransactionProposal(instantiateProposalRequest, Main.channel.getPeers());

            for (ProposalResponse response:transactionPropResp) {
                System.out.println(response.getStatus()+" : "+response.getMessage());
            }

        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

}
