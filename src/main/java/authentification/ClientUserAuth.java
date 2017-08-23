package authentification;


import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class provide authentification of user that identify as client of
 * blockchain.
 */
public class ClientUserAuth {

    private String salt;

    private char[] login;
    private char[] password;

    private ClientUserAuth() {
    }

    public ClientUserAuth(char[] login, char[] password, String salt) {
        this.salt = salt;
        this.login = login;
        this.password = password;
    }

    /**
     * Method get login {@link #login}, password {@link #password} and salt {@link #salt} and
     * then return hash value.
     * @return - secrete word that is hashed value of login, password and salt.
     */
    public String getHashLogPassword()
    {
        StringBuilder secretWord = new StringBuilder();
        secretWord.append(login);
        secretWord.append(" ");
        secretWord.append(password);
        secretWord.append(" ");
        secretWord.append(salt);

        try {
            byte[] secretInBytes = secretWord.toString().getBytes("UTF-8");

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hashedBytes = md5.digest(secretInBytes);

            BigInteger bigInt = new BigInteger(1, hashedBytes);
            String resultHashWord = bigInt.toString(16);

            return resultHashWord;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }



}
