package net.otgon.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import net.otgon.backend.dto.RedeemDeviceRequestDto;
import net.otgon.backend.dto.RedeemResult;
import net.otgon.backend.dto.TransactionPayload;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Device;
import net.otgon.backend.entity.Transaction;
import net.otgon.backend.exception.ValidationException;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.DeviceRepo;
import net.otgon.backend.repository.TransactionRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;

@Service
public class RedeemService {

    private static final Logger log = LoggerFactory.getLogger(RedeemService.class);

    private final DeviceRepo deviceRepo;
    private final CardRepo cardRepo;
    private final TransactionRepo transactionRepo;
    private final ObjectMapper objectMapper;
    private final CryptoService cryptoService;

    public RedeemService(DeviceRepo deviceRepo,
                         CardRepo cardRepo,
                         TransactionRepo transactionRepo,
                         ObjectMapper objectMapper,
                         CryptoService cryptoService) {
        this.deviceRepo = deviceRepo;
        this.cardRepo = cardRepo;
        this.transactionRepo = transactionRepo;
        this.objectMapper = objectMapper;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public RedeemResult redeem(RedeemDeviceRequestDto dto) {

        log.info("=== TRANSACTION PROCESSING STARTED ===");
        log.info("Device ID: {}", dto.getDeviceId());

        // 1. Load device
        Device device = deviceRepo.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not registered"));

        log.info("Device found: {} (User ID: {})", device.getId(), device.getUser().getId());

        // 2. Load public key, converting base64 string stored in db to PublicKey object
        log.debug("Public key (Base64): {}", device.getPublicKey());
        PublicKey pubKey = cryptoService.loadPublicKey(device.getPublicKey());

        // 3. Decode raw payload bytes (Base64)
        byte[] payloadBytes = Base64.getDecoder().decode(dto.getPayload());
        byte[] signatureBytes = Base64.getDecoder().decode(dto.getSignature());

        log.debug("Payload bytes length: {}", payloadBytes.length);
        log.debug("Signature bytes length: {}", signatureBytes.length);

        // 4. Verify signature
        log.info("Verifying ECDSA signature...");
        boolean valid = cryptoService.verify(payloadBytes, signatureBytes, pubKey);

        if (!valid) {
            log.error("❌ SIGNATURE VERIFICATION FAILED - Transaction rejected");
            log.error("Payload bytes: {}", Arrays.toString(payloadBytes));
            log.error("Signature bytes: {}", Arrays.toString(signatureBytes));
            throw new RuntimeException("Invalid signature — request tampered");
        }

        log.info("✓ Signature valid");

        // 5. Parse JSON inside payload
        String json = new String(payloadBytes, StandardCharsets.UTF_8);
        log.debug("Decoded payload JSON: {}", json);

        TransactionPayload payload;
        try {
            payload = objectMapper.readValue(json, TransactionPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse transaction payload JSON", e);
            throw new RuntimeException("Invalid payload JSON", e);
        }

        String txId = payload.getTxId();
        double fare = payload.getFare();
        long timestamp = payload.getTimestamp();

        log.info("Transaction ID: {}", txId);
        log.info("Fare amount: €{}", String.format("%.2f", fare));
        log.info("Timestamp: {} ({}ms)", new java.util.Date(timestamp), timestamp);

        // Validate fare amount
        if (fare <= 0) {
            log.error("Invalid fare: amount must be positive (received: €{})", fare);
            throw new ValidationException("Invalid fare: amount must be positive");
        }
        if (fare > 10.0) {
            log.error("Invalid fare: exceeds maximum €10.00 (received: €{})", fare);
            throw new ValidationException("Invalid fare: exceeds maximum €10.00");
        }

        long currentTime = System.currentTimeMillis();
        long maxAge = 24 * 60 * 60 * 1000; 

        if (Math.abs(currentTime - timestamp) > maxAge) {
            log.error("Transaction expired: timestamp outside 24-hour window");
            log.error("Current time: {}, Transaction time: {}, Difference: {}ms",
                    currentTime, timestamp, Math.abs(currentTime - timestamp));
            throw new ValidationException("Transaction expired: timestamp outside 24-hour window");
        }

        log.info("✓ Timestamp valid (within 24-hour window)");

        // 6. Prevent duplicate processing
        if (transactionRepo.existsByTxId(txId)) {
            log.warn("⚠ Transaction already processed: {}", txId);
            Card card = device.getUser().getCard();
            return new RedeemResult("Already processed", card.getBalance(), fare);
        }
        log.info("✓ Transaction ID valid (Not duplicate)");
        // 7. Get user and card
        Card card = device.getUser().getCard();
        double currentBalance = card.getBalance();

        log.info("Current balance: €{}", String.format("%.2f", currentBalance));

        // 8. Check balance
        if (currentBalance < fare) {
            log.warn("INSUFFICIENT FUNDS");
            log.warn("Required: €{}, Available: €{}, Shortfall: €{}",
                    String.format("%.2f", fare),
                    String.format("%.2f", currentBalance),
                    String.format("%.2f", fare - currentBalance));
            return new RedeemResult("Insufficient funds", currentBalance, fare);
        }

        // 9. Deduct balance
        double newBalance = currentBalance - fare;
        card.setBalance(newBalance);
        cardRepo.save(card);

        log.info("✓ Balance updated:");
        log.info("  Before: €{}", String.format("%.2f", currentBalance));
        log.info("  Deducted: €{}", String.format("%.2f", fare));
        log.info("  After: €{}", String.format("%.2f", newBalance));

        // 10. Save transaction
        Transaction tx = new Transaction();
        tx.setTxId(txId);
        tx.setCard(card);
        tx.setType("DEDUCT");
        tx.setAmount(fare);
        tx.setSignature(dto.getSignature());
        tx.setTimestamp(timestamp);
        tx.setStatus("SUCCESS");
        tx.setProcessed(true);
        transactionRepo.save(tx);

        log.info("✓ Transaction saved to database");
        log.info("=== TRANSACTION PROCESSING COMPLETED SUCCESSFULLY ===");

        return new RedeemResult("Success", newBalance, fare);
    }
}