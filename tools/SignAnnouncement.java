import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class SignAnnouncement {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java tools/SignAnnouncement.java <announcement.json> <private-key.pem>");
            System.exit(2);
        }

        Path announcement = Path.of(args[0]);
        Path privateKeyPath = Path.of(args[1]);
        byte[] body = Files.readAllBytes(announcement);

        String pem = Files.readString(privateKeyPath, StandardCharsets.US_ASCII)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyDer = Base64.getDecoder().decode(pem);
        PrivateKey privateKey = KeyFactory.getInstance("Ed25519")
                .generatePrivate(new PKCS8EncodedKeySpec(keyDer));

        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(privateKey);
        signer.update(body);
        String signature = Base64.getEncoder().encodeToString(signer.sign());

        Path output = Path.of(announcement.toString() + ".sig");
        Files.writeString(output, signature + System.lineSeparator(), StandardCharsets.US_ASCII);
        System.out.println("Written: " + output);
    }
}
