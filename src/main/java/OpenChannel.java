import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.io.*;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by shomakhov on 09.08.2017.
 */
public class OpenChannel {
    public static  Channel channel;
    public static EventHub eventHub;
    public static Channel openChannel(String channelName) {
        try {
            destroyChannel();
            Main.certificateFile = Paths.get(Main.SERTIFICATEPATH).toFile();
            Main.privateKeyFile = Paths.get(Main.PRIVATKEY).toFile();

            String certificate = new String(IOUtils.toByteArray(new FileInputStream(Main.certificateFile.getAbsolutePath())), "UTF-8");


            Main.privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(Main.privateKeyFile.getAbsolutePath())));
            Main.org1_peer_admin = new FCUser("Org1Admin");
            Main.org1_peer_admin.setMspId(Main.MSPID);

            Main.org1_peer_admin.setEnrollment(new FCEnrollment(Main.privateKey, certificate));

            Main.client = HFClient.createNewInstance();
            Main.client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            Main.client.setUserContext(Main.org1_peer_admin);

            File cf = new File(Main.SERVERCRT);
            Properties ordererProperties = new Properties();
            ordererProperties.setProperty("pemFile", cf.getAbsolutePath());
            ordererProperties.setProperty("hostnameOverride", "orderer.example.com");
            ordererProperties.setProperty("sslProvider", "openSSL");
            ordererProperties.setProperty("negotiationType", "TLS");
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[]{5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[]{8L, TimeUnit.SECONDS});
            Main.orderer = Main.client.newOrderer("orderer.example.com", "grpc://" + Main.IP + ":7050", ordererProperties);

            Properties peerProperties = new Properties();
            cf = new File(Main.PEERSERVER);
            peerProperties.setProperty("pemFile", cf.getAbsolutePath());
            peerProperties.setProperty("peerOrg1.mspid", "Org1MSP");

            peerProperties.setProperty("hostnameOverride", "peer0.org1.example.com");
            peerProperties.setProperty("sslProvider", "openSSL");
            peerProperties.setProperty("negotiationType", "TLS");
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);
            Main.peer = Main.client.newPeer("peer0.org1.example.com", "grpc://" + Main.IP + ":7051", peerProperties);

            Properties ehProperties = new Properties();
            cf = new File(Main.PEERSERVER);
            ehProperties.setProperty("pemFile", cf.getAbsolutePath());
            ehProperties.setProperty("hostnameOverride", "peer0.org1.example.com");
            ehProperties.setProperty("sslProvider", "openSSL");
            ehProperties.setProperty("negotiationType", "TLS");
            ehProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[]{5L, TimeUnit.MINUTES});
            ehProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[]{8L, TimeUnit.SECONDS});
            eventHub = Main.client.newEventHub("peer0.org1.example.com", "grpc://" + Main.IP + ":7053", ehProperties);

            ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(Main.CHANELTX));

          channel = Main.client.newChannel(channelName, Main.orderer, channelConfiguration,
                   Main.client.getChannelConfigurationSignature(channelConfiguration, Main.org1_peer_admin));

            channel.addOrderer(Main.orderer);
           // channel.addPeer(Main.peer);
            channel.joinPeer(Main.peer);
            channel.addEventHub(eventHub);

            channel.initialize();
            System.out.println(channel.getName() + " created!");
            return channel;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
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

    static void destroyChannel(){
        Main.certificateFile = null;
        Main.privateKeyFile = null;
        Main.privateKey = null;
        Main.org1_peer_admin = null;
        Main.client = null;
        Main.orderer = null;
        Main.peer = null;
        if (channel!=null){
       channel.shutdown(true);
        channel = null;}
        eventHub = null;

    }

}
