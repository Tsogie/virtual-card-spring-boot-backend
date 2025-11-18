package net.otgon.backend.service;

import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class CryptoService {

    // Load EC public key from Base64 string (from Android Keystore)
    public PublicKey loadPublicKey(String base64Key) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory factory = KeyFactory.getInstance("EC"); // <-- changed from RSA to EC
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    // Verify payload signature using SHA256withECDSA
    public boolean verify(byte[] payload, byte[] signature, PublicKey publicKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA"); // <-- match Android frontend
            sig.initVerify(publicKey);
            sig.update(payload);
            boolean isValid = sig.verify(signature);
            System.out.println("[LOG] Signature verification result: " + isValid);
            return isValid;
        } catch (Exception e) {
            throw new RuntimeException("Signature verification failed", e);
        }
    }
}
