import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.transaction.TransactionBuilder;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.io.*;
import java.security.PrivateKey;
import java.util.*;

/**
 * Created by user on 21.07.2017.
 */
public class Main {
    public static final String IP = "192.168.99.100";
    public static final String CFPATH = "src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem";
    public static final String SERTIFICATEPATH = "src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem";
    public static final String PRIVATKEY = "src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/db92ba8af79da54b38bb06b114f1831cce020c15b4f630b30a4505f21ed8b344_sk";
    public static final String SERVERCRT = "src/main/env/channel/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.crt";
    public static final String PEERSERVER = "src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/server.crt";
    public static final String CHANELTX = "src/main/env/channel/channel.tx";
    public static final String ADMINSECRET = "adminpw";
    public static final String MSPID = "Org1MSP";
    public static final String CHAIN_CODE_PATH = "doc_cc";
    public static final String CHAIN_CODE_VERSION = "1";
    public static final String CHAIN_CODE_NAME = "doc_cc_go";

    public static HFClient client;
    public static Peer peer;
    public static Channel channel;
    public static Orderer orderer;
    public static FCUser org1_user;

  //  public static InstallProposalRequest installProposalRequest;
    public static FCUser org1_peer_admin;
    public static HFCAClient org1_ca;
    public static FCUser org1_admin;
    public static Properties properties;
    public static File certificateFile;
    public static File privateKeyFile;
    public static PrivateKey privateKey;
  public static  ChaincodeEndorsementPolicy chaincodeEndorsementPolicy;
    public final static ChaincodeID chaincodeID =  ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
            .setVersion(CHAIN_CODE_VERSION)
            .setPath(CHAIN_CODE_PATH).build();

    public static void main(String[] args) {
        try {
            File cf = new File(Main.CFPATH);
            properties = new Properties();
            properties.setProperty("allowAllHostNames", "true");
            properties.setProperty("pemFile", cf.getAbsolutePath());


            RegisterUser.register("Ashamaz");
            channel = OpenChannel.openChannel("mychannel");

            //------------------------------------------------------

            System.out.println("ChaincodeID: "+chaincodeID.getPath()+" "+chaincodeID.getName());
            Set<Peer> peersFromOrg = new HashSet<>();
            peersFromOrg.add(peer);

             chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File("src/main/env/chaincodeendorsementpolicy.yaml"));

            InstallProposalRequest   installProposalRequest = client.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);

        //    installProposalRequest.setChaincodeSourceLocation(new File("src/main/java"));

            File initialFile = new File("src/main/cc/src/doc_cc");

            installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(initialFile, "src/doc_cc"));
            installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);
            installProposalRequest.setProposalWaitTime(120000);
            installProposalRequest.setUserContext(org1_peer_admin);
//            File initialFile = new File("C:\\Users\\Shomakhov\\go_cc");
//            installProposalRequest.setChaincodeSourceLocation(initialFile);

            //-----------------------------------------------------------------------

            Collection<ProposalResponse> responses = client.sendInstallProposal(installProposalRequest, peersFromOrg);

            List<FabricProposalResponse.Endorsement> ed = new LinkedList<>();
            FabricProposal.Proposal proposal = null;
            ByteString proposalResponsePayload = ByteString.copyFromUtf8("1234");

            for (ProposalResponse sdkProposalResponse : responses) {
                try {
                    System.out.println(sdkProposalResponse.getStatus());
                    System.out.println(sdkProposalResponse.getMessage());

                    FabricProposalResponse.Endorsement element = sdkProposalResponse.getProposalResponse().getEndorsement();
                    ed.add(element);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                if (proposal == null) {
                    proposal = sdkProposalResponse.getProposal();
                    //  proposalTransactionID = sdkProposalResponse.getTransactionID();
                    proposalResponsePayload = sdkProposalResponse.getProposalResponse().getPayload();
                }
            }

            TransactionBuilder transactionBuilder = TransactionBuilder.newBuilder();
            Common.Payload transactionPayload = transactionBuilder
                    .chaincodeProposal(proposal)
                    .endorsements(ed)
                    .proposalResponsePayload(proposalResponsePayload).build();

            Common.Envelope transactionEnvelope = Common.Envelope.newBuilder()
                    .setPayload(transactionPayload.toByteString())
                    .setSignature(ByteString.copyFrom(client.getCryptoSuite().sign(org1_user.getEnrollment().getKey(), transactionPayload.toByteArray())))
                    .build();

            Collection<Orderer> orderers = new ArrayList<>();
            orderers.add(orderer);
            channel.sendTransaction(responses, orderers);

            Scanner scanner = new Scanner(System.in);
            String line = "";
            System.out.println("Введите команду...");
            while (!(line.equals("exit"))) {
                line = scanner.nextLine();
                switch (line){
                    case "init":
                        Commands.sendTransInit();
                        break;
                    case "add":
                        Commands.sendTransAdd();
                        break;
                    case "update":
                        Commands.sendTransUpdate();
                        break;
                    case "query":
                        Commands.sendTransQuery();
                        break;
                        default:
                            System.out.println("Введите корректную команду (init, add... exit)");
                            break;
                }


            }

            channel.shutdown(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
