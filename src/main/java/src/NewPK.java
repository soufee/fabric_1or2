package src;

import java.io.Serializable;
import java.security.PrivateKey;

public class NewPK implements PrivateKey,Serializable {
    @Override
    public String getAlgorithm() {
        return null;
    }

    @Override
    public String getFormat() {
        return null;
    }

    @Override
    public byte[] getEncoded() {
        return new byte[0];
    }
}
