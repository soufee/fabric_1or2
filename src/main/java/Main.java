import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by user on 21.07.2017.
 */
public class Main {

    public static void main(String[] args) {
        try {

            Properties properties = new Properties();
            properties.setProperty("allowAllHostNames", "true");

            // Get certification authority
            File cf = new File("src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem");
            properties.setProperty("pemFile", cf.getAbsolutePath());
            HFCAClient org1_ca = HFCAClient.createNewInstance("http://localhost:7054" , properties);

            org1_ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            // Organisation Certification authority administrator login
            FCUser org1_admin = new FCUser("admin");
            org1_admin.setEnrollment(org1_ca.enroll(org1_admin.getName(), "adminpw"));

            // Organisation user register
            FCUser org1_user = new FCUser("user1");
            RegistrationRequest registrationRequest = new RegistrationRequest(org1_user.getName(), "org1.department1");
            String enrollmentSecret = org1_ca.register(registrationRequest, org1_admin);

            org1_user.setEnrollment(org1_ca.enroll(org1_user.getName(), enrollmentSecret));
            org1_user.setMspId("Org1MSP");

            // Organisation peer admin config
            FCUser org1_peer_admin = new FCUser("Org1Admin");
            org1_peer_admin.setMspId("Org1MSP");

            File certificateFile = Paths.get("src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem")
                    .toFile();
            String certificate = new String(IOUtils.toByteArray(new FileInputStream(certificateFile.getAbsolutePath())), "UTF-8");


            File privateKeyFile = Paths.get("src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/db92ba8af79da54b38bb06b114f1831cce020c15b4f630b30a4505f21ed8b344_sk")
                    .toFile();
            PrivateKey privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(privateKeyFile.getAbsolutePath())));

            org1_peer_admin.setEnrollment(new FCEnrollment(privateKey, certificate));


            // Application client
            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            // Set to peer admin
            client.setUserContext(org1_peer_admin);


            // Get orderer
            cf = new File("src/main/env/channel/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.crt");
            Properties ordererProperties = new Properties();
            ordererProperties.setProperty("pemFile", cf.getAbsolutePath());
            ordererProperties.setProperty("hostnameOverride", "orderer.example.com");
            ordererProperties.setProperty("sslProvider", "openSSL");
            ordererProperties.setProperty("negotiationType", "TLS");
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            Orderer orderer = client.newOrderer("orderer.example.com", "grpc://localhost:7050", ordererProperties);

            // Get peer
            Properties peerProperties = new Properties();
            cf = new File("src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/server.crt");
            peerProperties.setProperty("pemFile", cf.getAbsolutePath());
            peerProperties.setProperty("hostnameOverride", "peer0.org1.example.com");
            peerProperties.setProperty("sslProvider", "openSSL");
            peerProperties.setProperty("negotiationType", "TLS");
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = client.newPeer("peer0.org1.example.com", "grpc://localhost:7051", peerProperties);

            // Add Event Hub
            Properties ehProperties = new Properties();
            cf = new File("src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/server.crt");
            ehProperties.setProperty("pemFile", cf.getAbsolutePath());
            ehProperties.setProperty("hostnameOverride", "peer0.org1.example.com");
            ehProperties.setProperty("sslProvider", "openSSL");
            ehProperties.setProperty("negotiationType", "TLS");
            ehProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ehProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            EventHub eventHub = client.newEventHub("peer0.org1.example.com", "grpc://localhost:7053", ehProperties);

            // Create new channel
            ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File("src/main/env/channel/channel.tx"));
            Channel channel = client.newChannel("mychannel", orderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, org1_peer_admin));

            channel.addOrderer(orderer);
            channel.addPeer(peer);
            channel.addEventHub(eventHub);

            // Open channel
            channel.initialize();

            // Install Chaincode
            ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                    .setName("example_cc_java")
                    .setVersion("1")
                    .build();

            InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);
            installProposalRequest.setChaincodeLanguage(TransactionRequest.Type.JAVA);
            installProposalRequest.setChaincodePath(null); // Must be null for Java!


            cf = new File("chaincode/build");
            installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(
                    cf, null));

            installProposalRequest.setChaincodeVersion("1");

            Collection<ProposalResponse> responses = client.sendInstallProposal(installProposalRequest, new HashSet<Peer>(Arrays.asList(peer)));


            SDKUtils.getProposalConsistencySets(responses);



            //channel.sendTransaction();


            // Close channel
            channel.shutdown(true);


            //Persistence is not part of SDK. Sample file store is for demonstration purposes only!
            //   MUST be replaced with more robust application implementation  (Database, LDAP)
//            File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
//            if (sampleStoreFile.exists()) { //For testing start fresh
//                sampleStoreFile.delete();
//            }

            //final SampleStore sampleStore = new SampleStore(sampleStoreFile);



//            cf = new File("src/main/env/channel/crypto-config/peerOrganizations/org2.example.com/ca/ca.org2.example.com-cert.pem");
//            properties.setProperty("pemFile", cf.getAbsolutePath());
//            HFCAClient org2_ca = HFCAClient.createNewInstance("http://localhost:8054" , properties);




        } catch (CryptoException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (EnrollmentException e) {
            e.printStackTrace();
        } catch (org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        final Reader pemReader = new StringReader(new String(data));

        final PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }

        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);

        return privateKey;
    }

}
