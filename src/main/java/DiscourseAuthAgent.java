

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

    String[] generateKeyPair(){
        String[] result = new String[2];

        // generate KeyPair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        // get and base64 encode public key
        byte[] publicKey  = keyPair.getPublic().getEncoded();
        String b64PublicKey = Base64.encodeToString(publicKey, Base64.DEFAULT);
        result[0] = b64PublicKey;

        // get and base63 encode private key
        byte[] privateKey = keyPair.getPrivate().getEncoded();
        String b64PrivateKey = Base64.encodeToString(privateKey, Base64.DEFAULT);
        b64PublicKey[1] = b64PrivateKey;

        return result;
    }
}
