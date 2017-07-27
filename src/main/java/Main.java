import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Properties;

/**
 * Created by user on 21.07.2017.
 */
public class Main {

    public static void main(String[] args) {
        try {

            Properties properties = new Properties();
            properties.setProperty("allowAllHostNames", "true");

            File cf = new File("src/main/env/channel/crypto-config/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem");
            properties.setProperty("pemFile", cf.getAbsolutePath());
            HFCAClient org1_ca = HFCAClient.createNewInstance("http://192.168.99.100:7054" , properties);

            org1_ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            FCUser org1_admin = new FCUser("admin");
            org1_admin.setEnrollment(org1_ca.enroll(org1_admin.getName(), "adminpw"));

            FCUser org1_user = new FCUser("user1");
            RegistrationRequest registrationRequest = new RegistrationRequest(org1_user.getName(), "org1.department1");
            String enrollmentSecret = org1_ca.register(registrationRequest, org1_admin);

            org1_user.setEnrollment(org1_ca.enroll(org1_user.getName(), enrollmentSecret));
            org1_user.setMspId("Org1MSP");








            cf = new File("src/main/env/channel/crypto-config/peerOrganizations/org2.example.com/ca/ca.org2.example.com-cert.pem");
            properties.setProperty("pemFile", cf.getAbsolutePath());
            HFCAClient org2_ca = HFCAClient.createNewInstance("http://192.168.99.100:8054" , properties);





            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            //client.setUserContext()

            //Persistence is not part of SDK. Sample file store is for demonstration purposes only!
            //   MUST be replaced with more robust application implementation  (Database, LDAP)
//            File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
//            if (sampleStoreFile.exists()) { //For testing start fresh
//                sampleStoreFile.delete();
//            }

            //final SampleStore sampleStore = new SampleStore(sampleStoreFile);




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

}
