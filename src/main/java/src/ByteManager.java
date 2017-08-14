package src;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.*;
import java.security.PrivateKey;

public class ByteManager {


    public static byte[] Convert(String filePath) throws IOException {
        final File file = new File(filePath);
        final FileInputStream fis = new FileInputStream(filePath);
        byte[] bytes = new byte[(int) file.length()];
        fis.read(bytes);
        fis.close();

        return bytes;
    }


    public static void main(String[] args) {
        try {
            Convert("C:\\Users\\agliullin\\Desktop\\idea projects\\fabric7\\src\\main\\env\\channel\\crypto-config\\peerOrganizations\\org1.example.com\\ca\\ca.org1.example.com-cert.pem");
        } catch (IOException e) {

        }
    }
    public static byte[] Reverse(String input){
        String mas[] = input.split(" ");
        byte[] bytes=new byte[input.split(" ").length];
        for (int i=0;i<input.split(" ").length;i++
             ) {
            bytes[i]= Byte.parseByte(mas[i]);
        }
        return bytes;
    };


//    public static PrivateKey getPK(String input) throws PEMException {
//        //String original = input.split(" ")[4];
//      //  String pk = input.substring(32,input.length()-2);
//        final Reader pemReader = new StringReader(input);
//        PrivateKeyInfo pemPair=null;
//        try (PEMParser pemParser = new PEMParser(pemReader)) {
//            pemPair = (PrivateKeyInfo) pemParser.readObject();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);
//
//        return privateKey;
//    }


 public    static PrivateKey getPrivateKeyFromBytes(byte[] data) throws Exception {
        final Reader pemReader = new StringReader(new String(data));

        PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }

        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);

        return privateKey;
    }

}
