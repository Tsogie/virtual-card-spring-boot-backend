package net.otgon.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("CryptoService Unit Tests")
class CryptoServiceTest {

    @Autowired
    private CryptoService cryptoService;

    // =====================================================
    // TEST 1: LOAD PUBLIC KEY - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Test 1: Load Public Key Success - Valid Base64 EC key returns PublicKey")
    void testLoadPublicKeySuccess() throws Exception {
        // ============ ARRANGE ============
        // Generate real EC keypair with helper method
        KeyPair keyPair = generateECKeyPair();

        // Get public key bytes
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();

        // Encode to Base64 (string)
        String base64PublicKey = Base64.getEncoder().encodeToString(publicKeyBytes);

        // ============ ACT ============
        PublicKey loadedKey = cryptoService.loadPublicKey(base64PublicKey);

        // ============ ASSERT ============
        assertNotNull(loadedKey, "Loaded public key should not be null");
        assertEquals("EC", loadedKey.getAlgorithm()
                , "Key should be EC algorithm");
    }

    // =====================================================
    // TEST 2: LOAD PUBLIC KEY - FAIL, INVALID BASE64
    // =====================================================
    @Test
    @DisplayName("Test 2: Load Public Key Fails - Invalid Base64 throws exception")
    void testLoadPublicKeyInvalidBase64() throws Exception  {
        // ============ ARRANGE ============
        String invalidBase64 = "This is not valid Base64";

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cryptoService.loadPublicKey(invalidBase64),
                "Expected RuntimeException for failure of loading public key"
        );

        assertNotNull(exception);
        // Verify exception message
        assertEquals("Failed to load public key", exception.getMessage());
    }

    // =====================================================
    // TEST 3: VERIFY SIGNATURE - VALID
    // =====================================================
    @Test
    @DisplayName("Test 3: Verify Signature Success - Valid ECDSA signature returns true")
    void testVerifyValidSignature() throws Exception {
        // ============ ARRANGE ============
        // Generate real EC keypair
        KeyPair keyPair = generateECKeyPair();

        // Create payload
        String message = "test transaction data";
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);

        // Sign payload with private key (SHA256withECDSA)
        byte[] signature = signData(payload, keyPair.getPrivate());

        // ============ ACT ============
        boolean isValid = cryptoService.verify(payload, signature, keyPair.getPublic());

        // ============ ASSERT ============
        assertTrue(isValid, "Valid signature should return true");
    }

    // =====================================================
    // TEST 4: VERIFY SIGNATURE - INVALID (TAMPERED SIGNATURE)
    // =====================================================
    @Test
    @DisplayName("Test 4: Verify Signature Fails - Tampered signature returns false")
    void testVerifyInvalidSignature() throws Exception {
        // ============ ARRANGE ============
        // Generate real EC keypair
        KeyPair keyPair = generateECKeyPair();

        // Create payload
        String message = "test transaction data";
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);

        // Sign payload with private key (SHA256withECDSA)
        byte[] validSignature = signData(payload, keyPair.getPrivate());

        // Tamper with the signature
        byte[] tamperedSignature = validSignature.clone();
        tamperedSignature[0] = (byte) (tamperedSignature[0] + 1);

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cryptoService.verify(payload, tamperedSignature, keyPair.getPublic()),
                "Expected RuntimeException for failed signature verification"
        );

        assertNotNull(exception);
        // Verify exception message
        assertEquals("Signature verification failed", exception.getMessage());

    }

    // =====================================================
    // TEST 5: VERIFY SIGNATURE - TAMPERED PAYLOAD
    // =====================================================
    @Test
    @DisplayName("Test 5: Verify Signature Fails - Modified payload returns false")
    void testVerifyTamperedPayload() throws Exception {
        // ============ ARRANGE ============
        // Generate real EC keypair
        KeyPair keyPair = generateECKeyPair();

        // Create payload
        String message = "test transaction data";
        byte[] originalPayload = message.getBytes(StandardCharsets.UTF_8);

        // Sign payload with private key
        byte[] validSignature = signData(originalPayload, keyPair.getPrivate());

        // Create different payload
        message = "test transaction data modified";
        byte[] modifiedPayload = message.getBytes(StandardCharsets.UTF_8);

        // ============ ACT ============
        boolean isValid = cryptoService.verify(modifiedPayload, validSignature, keyPair.getPublic());

        // ============ ASSERT ============
        assertFalse(isValid, "Modified payload should return false");
    }

    // =====================================================
    // TEST 6: VERIFY SIGNATURE - WRONG KEY
    // =====================================================
    @Test
    @DisplayName("Test 6: Verify Signature Fails - Wrong public key returns false")
    void testVerifyWithWrongKey() throws Exception {
        // ============ ARRANGE ============
        // Generate real EC keypair
        KeyPair keyPairA = generateECKeyPair();

        // Generate different EC keypair
        KeyPair keyPairB = generateECKeyPair();

        // Create payload
        String message = "test transaction data";
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);

        // Sign payload with private key A
        byte[] signatureByA = signData(payload, keyPairA.getPrivate());

        // ============ ACT ============
        boolean isValid = cryptoService.verify(payload, signatureByA, keyPairB.getPublic());

        // ============ ASSERT ============
        assertFalse(isValid, "Wrong public key should return false");
    }

    // =====================================================
    // HELPER METHOD: Generate EC KeyPair
    // =====================================================
    private KeyPair generateECKeyPair() throws Exception {

        // 1. Get EC KeyPair generator
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");

        // 2. Initialize with secp256r1 curve
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        keyGen.initialize(ecSpec);

        // 3. Generate the keypair
        return keyGen.generateKeyPair();
    }

    // =====================================================
    // HELPER METHOD: Sign Data
    // =====================================================
    private byte[] signData(byte[] data, PrivateKey privateKey) throws Exception {

        // 1. Get signature instance
        Signature signature = Signature.getInstance("SHA256withECDSA");

        // 2. Initialize with private key
        signature.initSign(privateKey);

        // 3. Add data to sign
        signature.update(data);

        // 4. Generate signature
        return signature.sign();
    }
}
