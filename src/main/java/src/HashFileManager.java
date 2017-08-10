package src;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by shomakhov on 10.08.2017.
 */

public class HashFileManager {

    private String filePath;
    private String filename;
    private String filehash;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public static String getFilehash(String fileName) throws NoSuchAlgorithmException, FileNotFoundException {
        String output = null;
        MessageDigest digest = MessageDigest.getInstance("MD5");
        File f = new File(fileName);
        InputStream is = new FileInputStream(f);
        byte[] buffer = new byte[8192];
        int read = 0;
        try {
            while( (read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            output = bigInt.toString(16);
            //  System.out.println("MD5: " + output);
        }
        catch(IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        }
        finally {
            try {
                is.close();
                return output;
            }
            catch(IOException e) {
                throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
            }
        }



    }

    public static void main(String[] args) throws FileNotFoundException, NoSuchAlgorithmException {

    }
}

