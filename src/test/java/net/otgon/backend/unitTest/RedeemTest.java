package net.otgon.backend.unitTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONObject;
import net.otgon.backend.dto.RedeemDeviceRequestDto;
import net.otgon.backend.dto.RedeemResult;
import net.otgon.backend.dto.TransactionPayload;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Device;
import net.otgon.backend.entity.Transaction;
import net.otgon.backend.entity.User;
import net.otgon.backend.exception.ValidationException;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.DeviceRepo;
import net.otgon.backend.repository.TransactionRepo;
import net.otgon.backend.service.CryptoService;
import net.otgon.backend.service.RedeemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Redeem service unit tests")
public class RedeemTest {

    @Mock
    private DeviceRepo deviceRepo;
    @Mock
    private CardRepo cardRepo;
    @Mock
    private TransactionRepo transactionRepo;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CryptoService cryptoService;
    @InjectMocks
    private RedeemService redeemService;

    //Test data
    KeyPair keyPair;
    PrivateKey privateKey;
    PublicKey publicKey;
    String publicKeyBase64;
    String username = "alice";
    String txId = UUID.randomUUID().toString();
    double fare = 10;
    long timestamp = System.currentTimeMillis();

    @BeforeEach
    void setup() throws Exception{
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    @Test
    @DisplayName("Success path")
    void redeemSuccessPath() throws Exception{

        //Arrange
        byte[] payloadBytes = createPayload(txId, fare, timestamp);
        byte[] signatureBytes = signPayload(payloadBytes);

        String payloadBase64 = Base64.getEncoder().encodeToString(payloadBytes);
        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

        User user = createUserByUsername(username);
        Card card = user.getCard();

        Device device = new Device();
        device.setId("device.id");
        device.setPublicKey(publicKeyBase64);
        device.setUser(user);

        TransactionPayload transactionPayload = new TransactionPayload();
        transactionPayload.setTxId(txId);
        transactionPayload.setFare(fare);
        transactionPayload.setTimestamp(timestamp);

        RedeemDeviceRequestDto request = new RedeemDeviceRequestDto();
        request.setDeviceId(device.getId());
        request.setPayload(payloadBase64);
        request.setSignature(signatureBase64);

        when(deviceRepo.findById(device.getId())).thenReturn(Optional.of(device));
        when(cryptoService.loadPublicKey(device.getPublicKey())).thenReturn(publicKey);
        when(cryptoService.verify(any(), any(), any())).thenReturn(true);
        when(objectMapper.readValue(anyString(), eq(TransactionPayload.class)))
                .thenReturn(transactionPayload);
        when(transactionRepo.existsByTxId(anyString())).thenReturn(false);

        //Act
        RedeemResult result = redeemService.redeem(request);

        //Assert
        assertEquals("Success", result.getStatus());
        assertEquals(10.0 - fare, result.getNewBalance());
        assertEquals(fare, result.getFareDeducted());
        verify(cardRepo, times(1)).save(card);
        verify(transactionRepo, times(1)).save(any(Transaction.class));
    }

    //TEST-2 FAIL DEVICE NOT FOUND
    @Test
    @DisplayName("Fail: Device not found")
    void redeemDeviceNotFound() throws Exception{

        //Arrange
        String notExistingId = "notExistingId";

        RedeemDeviceRequestDto request = new RedeemDeviceRequestDto();
        request.setDeviceId(notExistingId);
        request.setPayload("MockPayload");
        request.setSignature("MockSignature");

        when(deviceRepo.findById(notExistingId)).thenReturn(Optional.empty());

        //Act & Assert
        RuntimeException ex =  assertThrows(
                RuntimeException.class,
                () -> redeemService.redeem(request),
                "Expected Runtime Exception for not found device");
        assertEquals("Device not registered", ex.getMessage());
        verify(deviceRepo, times(1)).findById(notExistingId);
        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    //TEST-3 FAIL
    @Test
    @DisplayName("Fail: negative fare amount")
    void  validatePayloadNegativeAmount(){

        //Arrange
        TransactionPayload transactionPayload = new TransactionPayload();
        transactionPayload.setTxId(txId);
        transactionPayload.setFare(-1);
        transactionPayload.setTimestamp(timestamp);

        //Act & Assert
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> redeemService.validatePayload(transactionPayload),
                "Expected custom exception");
        assertEquals("Invalid fare: amount must be positive", ex.getMessage());
    }

    //TEST-4 FAIL FARE AMOUNT EXCEEDS
    @Test
    @DisplayName("Fail: fare amount exceeds")
    void  validatePayloadExceededAmount(){

        //Arrange
        TransactionPayload transactionPayload = new TransactionPayload();
        transactionPayload.setTxId(txId);
        transactionPayload.setFare(11);
        transactionPayload.setTimestamp(timestamp);

        //Act & Assert
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> redeemService.validatePayload(transactionPayload),
                "Expected custom exception");
        assertEquals("Invalid fare: exceeds maximum €10.00", ex.getMessage());
    }

    //TEST-5 TRANSACTION EXPIRED
    @Test
    @DisplayName("Fail: transaction expired")
    void  validatePayloadTransactionExpired(){
        //Arrange
        TransactionPayload transactionPayload = new TransactionPayload();
        transactionPayload.setTxId(txId);
        transactionPayload.setFare(10);
        transactionPayload.setTimestamp(24 * 60 * 60 * 1000);

        //Act & Assert
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> redeemService.validatePayload(transactionPayload),
                "Expected custom exception");
        assertEquals("Transaction expired: timestamp outside 24-hour window", ex.getMessage());
    }

    //TEST-6
    @Test
    @DisplayName("Fail: duplicate transaction")
    void processTransactionDuplicateTransaction() throws Exception{

        //Arrange
        User user = createUserByUsername("alice");
        Card card = user.getCard();
        Device device = new Device();
        device.setId("device.id");
        device.setPublicKey(publicKeyBase64);
        device.setUser(user);

        TransactionPayload transactionPayload = new TransactionPayload();
        transactionPayload.setTxId(txId);
        transactionPayload.setFare(fare);
        transactionPayload.setTimestamp(timestamp);

        //String signature
        byte[] payloadBytes = createPayload(txId, fare, timestamp);
        byte[] signatureBytes = signPayload(payloadBytes);

        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);
        when(transactionRepo.existsByTxId(txId)).thenReturn(true);

        //Act
        RedeemResult response = redeemService
                .processTransaction(device, transactionPayload, signatureBase64);

        //Assert
        assertNotNull(response);
        assertEquals(fare, response.getFareDeducted());
        assertEquals("Already processed", response.getStatus());

        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    //TEST-7 FAIL: INSUFFICIENT FUNDS
    @Test
    @DisplayName("Fail: insufficient funds")
    void processTransactionInsufficientFunds() throws Exception{

        //Arrange
        User user = createUserByUsername("alice");
        Card card = user.getCard();
        card.setBalance(fare - 1);
        Device device = new Device();
        device.setId("device.id");
        device.setPublicKey(publicKeyBase64);
        device.setUser(user);

        TransactionPayload transactionPayload = new TransactionPayload();
        transactionPayload.setTxId(txId);
        transactionPayload.setFare(fare);
        transactionPayload.setTimestamp(timestamp);

        //String signature
        byte[] payloadBytes = createPayload(txId, fare, timestamp);
        byte[] signatureBytes = signPayload(payloadBytes);

        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);
        when(transactionRepo.existsByTxId(txId)).thenReturn(false);
        //Act
        RedeemResult response = redeemService
                .processTransaction(device, transactionPayload, signatureBase64);

        //Assert
        assertNotNull(response);
        assertEquals(fare, response.getFareDeducted());
        assertEquals("Insufficient funds", response.getStatus());
        verify(cardRepo, never()).save(any(Card.class));
        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    //TEST-8 FAIL INVALID SIGNATURE
    @Test
    @DisplayName("Invalid signature")
    void verifyAndParsePayloadInvalidSignature() throws Exception{

        //Arrange
        Device device = new Device();
        device.setId("device.id");
        device.setPublicKey(publicKeyBase64);

        //String signature
        byte[] payloadBytes = createPayload(txId, fare, timestamp);
        byte[] signatureBytes = signPayload(payloadBytes);
        String payloadBase64 = Base64.getEncoder().encodeToString(payloadBytes);
        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

        when(cryptoService.loadPublicKey(device.getPublicKey())).thenReturn(publicKey);
        when(cryptoService.verify(any(), any(), any())).thenReturn(false);

        //Act & Assert
        RuntimeException ex =  assertThrows(
                RuntimeException.class,
                () -> redeemService
                .verifyAndParsePayload(device, payloadBase64, signatureBase64),
                "Expected runtime exception");
        assertEquals("Invalid signature — request tampered", ex.getMessage());

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

    User createUserByUsername(String username){
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setEmail("email");

        Card card = new Card();
        card.setId(UUID.randomUUID().toString());
        card.setBalance(10);
        card.setUser(user);
        user.setCard(card);

        return user;
    }

}
