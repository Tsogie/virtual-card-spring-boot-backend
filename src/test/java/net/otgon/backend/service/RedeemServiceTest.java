package net.otgon.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedeemService
 * Tests the most critical service that handles money transactions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedeemService Unit Tests")
class RedeemServiceTest {

    @Mock
    private DeviceRepo deviceRepo;

    @Mock
    private CardRepo cardRepo;

    @Mock
    private TransactionRepo transactionRepo;

    @Mock
    private CryptoService cryptoService;

    @Mock
    private PublicKey mockPublicKey;

    private ObjectMapper objectMapper;

    @InjectMocks
    private RedeemService redeemService;

    // Test data
    private Device mockDevice;
    private User mockUser;
    private Card mockCard;
    private String testDeviceId;
    private String testTxId;

    @BeforeEach
    void setUp() {
        // Initialize real ObjectMapper
        objectMapper = new ObjectMapper();

        // Re-inject with real ObjectMapper
        redeemService = new RedeemService(
                deviceRepo,
                cardRepo,
                transactionRepo,
                objectMapper,
                cryptoService
        );

        // Setup test data
        testDeviceId = UUID.randomUUID().toString();
        testTxId = UUID.randomUUID().toString();

        // Create mock user
        mockUser = new User();
        mockUser.setId(UUID.randomUUID().toString());
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");

        // Create mock card with €20 initial balance
        mockCard = new Card();
        mockCard.setId(UUID.randomUUID().toString());
        mockCard.setBalance(20.0);
        mockCard.setUser(mockUser);

        // Link card to user
        mockUser.setCard(mockCard);

        // Create mock device
        mockDevice = new Device();
        mockDevice.setId(testDeviceId);
        mockDevice.setPublicKey("mockPublicKeyBase64String");
        mockDevice.setUser(mockUser);
    }

    // =====================================================
    // TEST 1: SUCCESSFUL REDEMPTION
    // =====================================================
    @Test
    @DisplayName("Test 1: Redeem Success - Valid signature with sufficient balance")
    void testRedeemSuccess() throws Exception {
        // ============ ARRANGE ============
        double initialBalance = 20.0;
        double fare = 5.0;
        double expectedNewBalance = 15.0;

        // Mock device lookup
        when(deviceRepo.findById(testDeviceId)).thenReturn(Optional.of(mockDevice));

        // Mock public key loading
        when(cryptoService.loadPublicKey(anyString())).thenReturn(mockPublicKey);

        // Mock signature verification (VALID signature)
        when(cryptoService.verify(any(byte[].class), any(byte[].class), eq(mockPublicKey)))
                .thenReturn(true);

        // Mock transaction duplicate check (NOT duplicate)
        when(transactionRepo.existsByTxId(testTxId)).thenReturn(false);

        // Mock card repo save (returns the modified card)
        when(cardRepo.save(any(Card.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        // Mock transaction repo save
        when(transactionRepo.save(any(Transaction.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        // Create valid request DTO
        RedeemDeviceRequestDto requestDto = createValidRedeemRequest(
                testDeviceId,
                testTxId,
                fare,
                System.currentTimeMillis()
        );

        // ============ ACT ============
        RedeemResult result = redeemService.redeem(requestDto);

        // ============ ASSERT ============
        // Verify result
        assertNotNull(result);
        assertEquals("Success", result.getStatus());
        assertEquals(expectedNewBalance, result.getNewBalance(), 0.01);
        assertEquals(fare, result.getFareDeducted(), 0.01);

        // Verify card balance was updated
        assertEquals(expectedNewBalance, mockCard.getBalance(), 0.01);

        // Verify repositories were called correctly
        verify(deviceRepo, times(1)).findById(testDeviceId);
        verify(cryptoService, times(1)).loadPublicKey(anyString());
        verify(cryptoService, times(1)).verify(any(byte[].class), any(byte[].class),
                eq(mockPublicKey));
        verify(transactionRepo, times(1)).existsByTxId(testTxId);
        verify(cardRepo, times(1)).save(mockCard);
        verify(transactionRepo, times(1)).save(any(Transaction.class));

        // Verify transaction was saved with correct details
        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepo).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();

        assertEquals(testTxId, savedTransaction.getTxId());
        assertEquals("DEDUCT", savedTransaction.getType());
        assertEquals(fare, savedTransaction.getAmount(), 0.01);
        assertEquals(mockCard, savedTransaction.getCard());
        assertTrue(savedTransaction.isProcessed());
    }

    // =====================================================
    // TEST 2: INSUFFICIENT FUNDS
    // =====================================================
    @Test
    @DisplayName("Test 2: Insufficient Funds - Balance less than fare")
    void testRedeemInsufficientFunds() throws Exception {
        // ============ ARRANGE ============
        double lowBalance = 3.0;
        double fare = 5.0;

        // Update mock card to have low balance
        mockCard.setBalance(lowBalance);

        // Mock device lookup
        when(deviceRepo.findById(testDeviceId)).thenReturn(Optional.of(mockDevice));

        // Mock public key loading
        when(cryptoService.loadPublicKey(anyString())).thenReturn(mockPublicKey);

        // Mock signature verification (VALID signature)
        when(cryptoService.verify(any(byte[].class), any(byte[].class), eq(mockPublicKey)))
                .thenReturn(true);

        // Mock transaction duplicate check (NOT duplicate)
        when(transactionRepo.existsByTxId(testTxId)).thenReturn(false);

        // Create valid request DTO
        RedeemDeviceRequestDto requestDto = createValidRedeemRequest(
                testDeviceId,
                testTxId,
                fare,
                System.currentTimeMillis()
        );

        // ============ ACT ============
        RedeemResult result = redeemService.redeem(requestDto);

        // ============ ASSERT ============
        assertNotNull(result);
        assertEquals("Insufficient funds", result.getStatus());
        assertEquals(lowBalance, result.getNewBalance(), 0.01); 
        assertEquals(fare, result.getFareDeducted(), 0.01);

        // Verify balance was NOT deducted
        assertEquals(lowBalance, mockCard.getBalance(), 0.01);

        // Verify card was NOT saved
        verify(cardRepo, never()).save(any(Card.class));

        // Verify transaction was NOT saved
        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    // =====================================================
    // TEST 3: INVALID SIGNATURE (TAMPERED)
    // =====================================================
    @Test
    @DisplayName("Test 3: Invalid Signature - Tampered transaction rejected")
    void testRedeemInvalidSignature() throws Exception {
        // ============ ARRANGE ============
        // Mock device lookup
        when(deviceRepo.findById(testDeviceId)).thenReturn(Optional.of(mockDevice));

        // Mock public key loading
        when(cryptoService.loadPublicKey(anyString())).thenReturn(mockPublicKey);

        // Mock signature verification (INVALID - returns false)
        when(cryptoService.verify(any(byte[].class), any(byte[].class), eq(mockPublicKey)))
                .thenReturn(false); 

        // Create request DTO with tampered signature
        RedeemDeviceRequestDto requestDto = createValidRedeemRequest(
                testDeviceId,
                testTxId,
                5.0,
                System.currentTimeMillis()
        );

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> redeemService.redeem(requestDto),
                "Expected RuntimeException for invalid signature"
        );

        // Verify exception message
        assertEquals("Invalid signature — request tampered", exception.getMessage());

        // Verify signature was checked
        verify(cryptoService, times(1)).verify(any(byte[].class), any(byte[].class),
                eq(mockPublicKey));

        // Verify card was NOT saved (no balance deduction)
        verify(cardRepo, never()).save(any(Card.class));

        // Verify transaction was NOT saved
        verify(transactionRepo, never()).save(any(Transaction.class));

        // Verify duplicate check was NOT performed 
        verify(transactionRepo, never()).existsByTxId(anyString());
    }

    // =====================================================
    // TEST 4: DUPLICATE TRANSACTION
    // =====================================================
    @Test
    @DisplayName("Test 4: Duplicate Transaction - Same txId already processed")
    void testRedeemDuplicateTransaction() throws Exception {
        // ============ ARRANGE ============
        double currentBalance = 20.0;
        double fare = 5.0;

        // Mock device lookup
        when(deviceRepo.findById(testDeviceId)).thenReturn(Optional.of(mockDevice));

        // Mock public key loading
        when(cryptoService.loadPublicKey(anyString())).thenReturn(mockPublicKey);

        // Mock signature verification (VALID signature)
        when(cryptoService.verify(any(byte[].class), any(byte[].class), eq(mockPublicKey)))
                .thenReturn(true);

        // Mock transaction duplicate check (IS DUPLICATE)
        when(transactionRepo.existsByTxId(testTxId)).thenReturn(true); 

        // Create request DTO
        RedeemDeviceRequestDto requestDto = createValidRedeemRequest(
                testDeviceId,
                testTxId,
                fare,
                System.currentTimeMillis()
        );

        // ============ ACT ============
        RedeemResult result = redeemService.redeem(requestDto);

        // ============ ASSERT ============
        assertNotNull(result);
        assertEquals("Already processed", result.getStatus());
        assertEquals(currentBalance, result.getNewBalance(), 0.01); 
        assertEquals(fare, result.getFareDeducted(), 0.01);

        // Verify balance was NOT deducted
        assertEquals(currentBalance, mockCard.getBalance(), 0.01);

        // Verify card was NOT saved
        verify(cardRepo, never()).save(any(Card.class));

        // Verify transaction was NOT saved again
        verify(transactionRepo, never()).save(any(Transaction.class));

        // Verify duplicate check WAS performed
        verify(transactionRepo, times(1)).existsByTxId(testTxId);
    }

    // =====================================================
    // TEST 5: EXPIRED TIMESTAMP (> 24 hours old)
    // =====================================================
    @Test
    @DisplayName("Test 5: Expired Timestamp - Transaction older than 24 hours rejected")
    void testRedeemExpiredTimestamp() throws Exception {
        // ============ ARRANGE ============
        // Create timestamp from 25 hours ago (outside 24-hour window)
        long expiredTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L);

        // Mock device lookup
        when(deviceRepo.findById(testDeviceId)).thenReturn(Optional.of(mockDevice));

        // Mock public key loading
        when(cryptoService.loadPublicKey(anyString())).thenReturn(mockPublicKey);

        // Mock signature verification (VALID signature)
        when(cryptoService.verify(any(byte[].class), any(byte[].class), eq(mockPublicKey)))
                .thenReturn(true);

        // Create request DTO with expired timestamp
        RedeemDeviceRequestDto requestDto = createValidRedeemRequest(
                testDeviceId,
                testTxId,
                5.0,
                expiredTimestamp 
        );

        // ============ ACT & ASSERT ============
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> redeemService.redeem(requestDto),
                "Expected ValidationException for expired timestamp"
        );

        // Verify exception message
        assertEquals("Transaction expired: timestamp outside 24-hour window",
                exception.getMessage());

        // Verify card was NOT saved
        verify(cardRepo, never()).save(any(Card.class));

        // Verify transaction was NOT saved
        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    // =====================================================
    // TEST 6: FARE EXCEEDS MAXIMUM (€10)
    // =====================================================
    @Test
    @DisplayName("Test 6: Fare Exceeds Maximum - Fare > €10 rejected")
    void testRedeemFareExceedsMaximum() throws Exception {
        // ============ ARRANGE ============
        double excessiveFare = 15.0; // > €10 max

        // Mock device lookup
        when(deviceRepo.findById(testDeviceId)).thenReturn(Optional.of(mockDevice));

        // Mock public key loading
        when(cryptoService.loadPublicKey(anyString())).thenReturn(mockPublicKey);

        // Mock signature verification (VALID signature)
        when(cryptoService.verify(any(byte[].class), any(byte[].class), eq(mockPublicKey)))
                .thenReturn(true);

        // Create request DTO with excessive fare
        RedeemDeviceRequestDto requestDto = createValidRedeemRequest(
                testDeviceId,
                testTxId,
                excessiveFare,
                System.currentTimeMillis()
        );

        // ============ ACT & ASSERT ============
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> redeemService.redeem(requestDto),
                "Expected ValidationException for fare > €10"
        );

        // Verify exception message
        assertEquals("Invalid fare: exceeds maximum €10.00", exception.getMessage());

        // Verify card was NOT saved
        verify(cardRepo, never()).save(any(Card.class));

        // Verify transaction was NOT saved
        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    // =====================================================
    // TEST 7: NEGATIVE FARE
    // =====================================================
    @Test
    @DisplayName("Test 7: Negative Fare - Fare <= 0 rejected")
    void testRedeemNegativeFare() throws Exception {
        // ============ ARRANGE ============
        double negativeFare = -5.0;

        // Mock device lookup
        when(deviceRepo.findById(testDeviceId)).thenReturn(Optional.of(mockDevice));

        // Mock public key loading
        when(cryptoService.loadPublicKey(anyString())).thenReturn(mockPublicKey);

        // Mock signature verification (VALID signature)
        when(cryptoService.verify(any(byte[].class), any(byte[].class), eq(mockPublicKey)))
                .thenReturn(true);

        // Create request DTO with negative fare
        RedeemDeviceRequestDto requestDto = createValidRedeemRequest(
                testDeviceId,
                testTxId,
                negativeFare,
                System.currentTimeMillis()
        );

        // ============ ACT & ASSERT ============
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> redeemService.redeem(requestDto),
                "Expected ValidationException for negative fare"
        );

        // Verify exception message
        assertEquals("Invalid fare: amount must be positive", exception.getMessage());

        // Verify card was NOT saved
        verify(cardRepo, never()).save(any(Card.class));

        // Verify transaction was NOT saved
        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    // =====================================================
    // TEST 8: DEVICE NOT FOUND
    // =====================================================
    @Test
    @DisplayName("Test 8: Device Not Found - Invalid deviceId rejected")
    void testRedeemDeviceNotFound() throws Exception {
        // ============ ARRANGE ============
        String invalidDeviceId = "invalid-device-id";

        // Mock device lookup (NOT FOUND)
        when(deviceRepo.findById(invalidDeviceId)).thenReturn(Optional.empty());

        // Create request DTO with invalid device ID
        RedeemDeviceRequestDto requestDto = createValidRedeemRequest(
                invalidDeviceId,
                testTxId,
                5.0,
                System.currentTimeMillis()
        );

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> redeemService.redeem(requestDto),
                "Expected RuntimeException for device not found"
        );

        // Verify exception message
        assertEquals("Device not registered", exception.getMessage());

        // Verify repositories were called correctly
        verify(deviceRepo, times(1)).findById(invalidDeviceId);

        // Verify card was NOT saved
        verify(cardRepo, never()).save(any(Card.class));

        // Verify transaction was NOT saved
        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    /**
     * Creates a valid RedeemDeviceRequestDto for testing
     */
    private RedeemDeviceRequestDto createValidRedeemRequest(
            String deviceId,
            String txId,
            double fare,
            long timestamp) throws Exception {

        // Create transaction payload JSON
        TransactionPayload payload = new TransactionPayload();
        payload.setTxId(txId);
        payload.setFare(fare);
        payload.setTimestamp(timestamp);

        // Convert to JSON string
        String payloadJson = objectMapper.writeValueAsString(payload);

        // Encode as Base64
        String payloadBase64 = Base64.getEncoder().encodeToString(
                payloadJson.getBytes(StandardCharsets.UTF_8)
        );

        // Mock signature 
        String mockSignatureBase64 = Base64.getEncoder().encodeToString(
                "mock-signature-bytes".getBytes()
        );

        // Create DTO
        RedeemDeviceRequestDto dto = new RedeemDeviceRequestDto();
        dto.setDeviceId(deviceId);
        dto.setPayload(payloadBase64);
        dto.setSignature(mockSignatureBase64);

        return dto;
    }

    /**
     * Creates a mock Device with User and Card
     */
    private Device createMockDeviceWithCard(double cardBalance) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername("testuser");

        Card card = new Card();
        card.setId(UUID.randomUUID().toString());
        card.setBalance(cardBalance);
        card.setUser(user);

        user.setCard(card);

        Device device = new Device();
        device.setId(UUID.randomUUID().toString());
        device.setPublicKey("mockPublicKey");
        device.setUser(user);

        return device;
    }
}