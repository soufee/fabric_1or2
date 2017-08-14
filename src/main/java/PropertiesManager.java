import java.io.*;
import java.util.Properties;

public class PropertiesManager {


    public static void SetProperties(Properties properties, String filePath) throws IOException {
        OutputStream output = null;
        output = new FileOutputStream(filePath);
        properties.store(output,null);
    }

    public static Properties getProperties(String filePath) throws IOException {

        Properties properties = new Properties();
        FileInputStream fis= new FileInputStream(filePath);
properties.load(fis);
        return properties;
    }



}
