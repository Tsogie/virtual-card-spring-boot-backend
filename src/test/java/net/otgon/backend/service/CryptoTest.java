package net.otgon.backend.service;

import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests for Transaction Service")
@ActiveProfiles("test")
public class CryptoTest {

    PublicKey publicKey;
    PrivateKey privateKey;
    String publicKeyBase64;

    private final CryptoService cryptoService = new CryptoService();

    //Test data for verify() method
    String txId = UUID.randomUUID().toString();
    double fare = 10;
    long timestamp = System.currentTimeMillis();

    @BeforeEach
    void setup() throws Exception{
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    @Test
    @DisplayName("Success path for loading public key")
    void loadPublicKeySuccess() {

        //Act
        PublicKey loadedKey = cryptoService.loadPublicKey(publicKeyBase64);

        //Assert
        assertNotNull(loadedKey);
        assertEquals("EC", loadedKey.getAlgorithm());
    }

    @Test
    @DisplayName("Fail: algorithm mismatch")
    void loadPublicKeyFailWhenAlgorithmMismatch() throws Exception {

        //Arrange
        //Wrong algorithm
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyBase64withRSA = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        //Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cryptoService.loadPublicKey(publicKeyBase64withRSA),
                "Expected RuntimeEx when algorithm mismatch");

        assertEquals("Failed to load public key", exception.getMessage());
    }

    //TEST-3 FAIL WHEN MALFORMED BASE64
    @Test
    @DisplayName("Fail: malformed string")
    void loadPublicKeyFailWhenMalformedString() throws Exception {

        //Arrange
        String malformedString = "Malformed String";

        //Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cryptoService.loadPublicKey(malformedString),
                "Expected RuntimeEx when malformed String");

        assertEquals("Failed to load public key", exception.getMessage());
    }

    //TEST-4 SUCCESS PATH FOR VERIFY()
    @Test
    @DisplayName("Success path for verification")
    void verifySuccessPath() throws Exception {

        //Arrange
        byte[] payload = createPayload(txId, fare, timestamp);
        byte[] signature = signPayload(payload);

        //Act
        boolean isValid = cryptoService.verify(payload, signature, publicKey);

        //Assert
        assertTrue(isValid, "Valid signature should return true");
    }

    //TEST-5 FAIL WHEN PAYLOAD IS TAMPERED
    @Test
    @DisplayName("Fail: payload is tampered")
    void verifyFailPayloadIsTampered() throws Exception {

        //Arrange
        byte[] payload = createPayload(txId, fare, timestamp);
        byte[] signature = signPayload(payload);

        //tampered payload
        byte[] tamperedPayload = createPayload(txId, 5, timestamp);

        //Act
        boolean isValid = cryptoService.verify(tamperedPayload, signature, publicKey);

        //Assert
        assertFalse(isValid, "Tampered load should return false");
    }

    //TEST-6 FAIL WHEN SIGNATURE IS BROKEN
    @Test
    @DisplayName("Fail: signature is broken")
    void verifyFailSignatureIsBroken() throws Exception {

        //Arrange
        byte[] payload = createPayload(txId, fare, timestamp);
        byte[] signature = "brokenSignature".getBytes(StandardCharsets.UTF_8);

        //Act & Assert
        RuntimeException e = assertThrows(
                RuntimeException.class,
                () -> cryptoService.verify(payload, signature, publicKey),
                "Expected RuntimeEx when signature is broken");

        assertEquals("Signature verification failed", e.getMessage());
    }

    //TEST-7 FAIL WHEN USING WRONG PUBLIC KEY, DIFFERENT ALG
    @Test
    @DisplayName("Fail: wrong/incorrect public key for verification")
    void verifyFailWrongPublicKey() throws Exception {

        //Arrange
        byte[] payload = createPayload(txId, fare, timestamp);
        byte[] signature = signPayload(payload);
        //Incorrect key
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKeyRSA = keyPair.getPublic();

        //Act & Assert
        RuntimeException e = assertThrows(
                RuntimeException.class,
                () -> cryptoService.verify(payload, signature, publicKeyRSA),
                "Expected RuntimeEx when public key is not fair with private key");

        assertEquals("Signature verification failed", e.getMessage());
    }

    //TEST-8 FAIL WHEN WRONG PUBLIC KEY BUT SAME ALG(EC)
    @Test
    @DisplayName("Fail: wrong key returns false")
    void verifyFailWrongKeyOfSameAlgorithm() throws Exception {

        //Arrange
        byte[] payload = createPayload(txId, fare, timestamp);
        byte[] signature = signPayload(payload);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey2 = keyPair.getPublic();

        //Act
        boolean isValid = cryptoService.verify(payload, signature, publicKey2);

        //Assert
        assertFalse(isValid, "Wrong public key should return false");

    }

    //Helper method to create payload in bytes
    private byte[] createPayload(String txId, double fare, long timestamp){

        JSONObject payload = new JSONObject();
        payload.put("txId", txId);
        payload.put("fare", fare);
        payload.put("timestamp", timestamp);
        return payload.toString().getBytes(StandardCharsets.UTF_8);

    }

    //Helper method to sign a payload
    private byte[] signPayload(byte[] payload) throws Exception{

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(payload);
        return signature.sign();

    }



}
