import authentification.ClientUserAuth;
import org.apache.tomcat.jni.Error;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.RegistrationException;

import java.io.File;
import java.security.PrivateKey;
import java.util.Properties;
import java.util.Scanner;

/**
 * Created by shomakhov on 11.08.2017.
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
    public static ChaincodeEndorsementPolicy chaincodeEndorsementPolicy;
    public final static ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
            .setVersion(CHAIN_CODE_VERSION)
            .setPath(CHAIN_CODE_PATH).build();

    public static void main(String[] args) {
        try {
            File cf = new File(Main.CFPATH);
            properties = new Properties();
            properties.setProperty("allowAllHostNames", "true");
            properties.setProperty("pemFile", cf.getAbsolutePath());
            PropertiesManager.SetProperties(properties,"src\\main\\java\\properties\\Hostnames.properties");


            System.out.println("1-начать сначала (требуется перезапуск докера), 2-использовать готовый канал");
            Scanner in= new Scanner(System.in);
            int mode = in.nextInt();
            if(mode==1){
                PropertiesManager.SetProperties(properties,"src\\main\\java\\properties\\Hostnames.properties");
                RegisterUser.register("asadssdass7");
            channel = OpenChannel.openChannel("mychannel");
           SetChainCode.setChainCode();
            Commands.sendTransInit();}
            else{channel = RestoreManager.Restore();
            //SetChainCode.setChainCode();
            }
        } catch (RegistrationException e){
            e.printStackTrace();
            System.out.println("Перезапусти докер");
            System.out.println("Пользователь уже существует");
            System.exit(0);
        } catch (Exception e){
            System.out.println(e.getMessage());
            System.exit(0);
        }

        String secreteWord = getSecreteWordFromUser();

        System.out.println("Введите команду...");
        Scanner scanner = new Scanner(System.in);
        String line = "";

        while (!(line.equals("exit"))) {
            line = scanner.nextLine();


            switch (line) {
                case "add":
                    System.out.println("Введите имя файла");
                    Commands.sendTransAdd(scanner.nextLine(), secreteWord);
                    break;
                case "update":
                    System.out.println("Введите имя файла");
                    Commands.sendTransUpdate(scanner.nextLine(), secreteWord);
                    break;
                case "query":
                    System.out.println("Введите имя файла");
                    Commands.sendTransQuery(scanner.nextLine());
                    break;
                case "get":
                    System.out.println("Введите имя файла");
                    Commands.getOldHash(scanner.nextLine());
                case "check":
                    System.out.println("Введите имя файла");
                    Commands.checkFile(scanner.nextLine());
                default:
                    if (!(line.equals("exit")))
                        System.out.println("Введите команду");
                    else
                        break;
            }


        }

        channel.shutdown(true);
    }


    /**
     * Function get login and password from user using console.
     * Than it convert it to secrete word using hash algorithm and
     * return this secrete word.
     * @return - secrete word that aquired from hashing login and password.
     */
    private static String getSecreteWordFromUser()
    {
        System.out.println("Введите логин:");
        Scanner scanner = new Scanner(System.in);
        String login = scanner.nextLine();

        System.out.println("Введите пароль:");
        String password = scanner.nextLine();

        String salt = new String("blockchain2017");

        ClientUserAuth clientUserAuth = new ClientUserAuth(login.toCharArray(), password.toCharArray(), salt);
        String secreteWord = clientUserAuth.getHashLogPassword();
        return secreteWord;
    }




}
