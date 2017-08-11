import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.transaction.TransactionBuilder;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by shomakhov on 11.08.2017.
 */
public class SetChainCode {
    public static void setChainCode(){
        try {
            File cf = new File(Main.CFPATH);
            Main.properties = new Properties();
            Main.properties.setProperty("allowAllHostNames", "true");
            Main.properties.setProperty("pemFile", cf.getAbsolutePath());

            System.out.println("ChaincodeID: " +  Main.chaincodeID.getPath() + " " +  Main.chaincodeID.getName());
            Set<Peer> peersFromOrg = new HashSet<>();
            peersFromOrg.add( Main.peer);

            Main.chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            Main.chaincodeEndorsementPolicy.fromYamlFile(new File("src/main/env/chaincodeendorsementpolicy.yaml"));

            InstallProposalRequest installProposalRequest =  Main.client.newInstallProposalRequest();
            installProposalRequest.setChaincodeID( Main.chaincodeID);

            //    installProposalRequest.setChaincodeSourceLocation(new File("src/main/java"));

            File initialFile = new File("src/main/cc/src/doc_cc");

            installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(initialFile, "src/doc_cc"));
            installProposalRequest.setChaincodeVersion( Main.CHAIN_CODE_VERSION);
            installProposalRequest.setProposalWaitTime(120000);
            installProposalRequest.setUserContext( Main.org1_peer_admin);
//            File initialFile = new File("C:\\Users\\Shomakhov\\go_cc");
//            installProposalRequest.setChaincodeSourceLocation(initialFile);

            //-----------------------------------------------------------------------

            Collection<ProposalResponse> responses =  Main.client.sendInstallProposal(installProposalRequest, peersFromOrg);

            List<FabricProposalResponse.Endorsement> ed = new LinkedList<>();
            FabricProposal.Proposal proposal = null;
            ByteString proposalResponsePayload = ByteString.copyFromUtf8("1234");

            for (ProposalResponse sdkProposalResponse : responses) {
                try {
                    System.out.println("Chaincore status "+sdkProposalResponse.getStatus());
                    System.out.println("Chaincore message "+sdkProposalResponse.getMessage());

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
                    .setSignature(ByteString.copyFrom( Main.client.getCryptoSuite().sign( Main.org1_user.getEnrollment().getKey(), transactionPayload.toByteArray())))
                    .build();

            Collection<Orderer> orderers = new ArrayList<>();
            orderers.add( Main.orderer);
            Main.channel.sendTransaction(responses, orderers);

        } catch (Exception e){}
    }
}
