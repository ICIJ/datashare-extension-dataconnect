import org.apache.http.client.utils.URIBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

// https://riyazali.net/posts/discourse-login-on-android/
public class DiscourseAuthAgent {
    // Randomly generated or static client id for your app
    private String clientId;

    // Random string to verify that the
    // reply indeed comes from Discourse server
    private String nonce;

    // RSA Public key
    private String publicKey;

    // RSA Private key
    private String privateKey;

    // API key we get from the server
    private String apiKey;

    String[] generateKeyPair() throws NoSuchAlgorithmException {
        String[] result = new String[2];

        // generate KeyPair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        KeyPair keyPair = kpg.generateKeyPair();

        // get and base64 encode public key
        byte[] publicKey  = keyPair.getPublic().getEncoded();
        String b64PublicKey = Base64.getEncoder().encodeToString(publicKey);
        result[0] = b64PublicKey;

        // get and base63 encode private key
        byte[] privateKey = keyPair.getPrivate().getEncoded();
        String b64PrivateKey = Base64.getEncoder().encodeToString(privateKey);
        result[1] = b64PrivateKey;

        return result;
    }

    static String randomString(final int sizeOfRandomString) {
        // taken from https://stackoverflow.com/a/12116194/6611700
        String ALLOWED_CHARACTERS =
                "0123456789qwertyuiopasdfghjklzxcvbnm";

        final Random random= new Random();
        final StringBuilder sb= new StringBuilder(sizeOfRandomString);

        for(int i=0;i<sizeOfRandomString;++i)
            sb.append(
                    ALLOWED_CHARACTERS.charAt(
                            random.nextInt(ALLOWED_CHARACTERS.length())
                    )
            );
        return sb.toString();
    }

    URL generateAuthUri(String appName, String[] scopes, String redirectUri) throws NoSuchAlgorithmException, URISyntaxException, MalformedURLException {
        clientId = randomString(32 /* size */);
        nonce = randomString(16 /* size */);
        String[] rsaKeyPair = generateKeyPair();
        publicKey = rsaKeyPair[0];
        privateKey = rsaKeyPair[1];

        new URIBuilder();
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http")
                .setHost("discourse")
                .setPort(3000)
                .setPath("/user-api-key/new")
                .addParameter("scopes", String.join(",", scopes))
                .addParameter("client_id", clientId)
                .addParameter("nonce", nonce)
                .addParameter("auth_redirect", redirectUri)
                .addParameter("application_name", appName)
                .addParameter("public_key", getFormattedPublicKey(Base64.getDecoder().decode(publicKey)));
        return builder.build().toURL();
    }

    String getFormattedPublicKey(byte[] unformatted){
        String pkcs1pem = "-----BEGIN PUBLIC KEY-----\n";
        pkcs1pem += Base64.getEncoder().encodeToString(unformatted);
        pkcs1pem += "\n-----END PUBLIC KEY-----";
        return pkcs1pem;
    }

    public static void main(String[] args) throws Exception {
        URL dataconnectUrl = new DiscourseAuthAgent().generateAuthUri("dataconnect", new String[]{"read"}, "http://localhost:8888");
        BufferedReader in = new BufferedReader(new InputStreamReader(dataconnectUrl.openStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            System.out.println(inputLine);
        in.close();
    }
}
