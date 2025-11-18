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
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;

@Service
public class RedeemService {

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

        // 1. Load device
        Device device = deviceRepo.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not registered"));

        // 2. Load public key
        System.out.println("Pub key: " + device.getPublicKey());
        PublicKey pubKey = cryptoService.loadPublicKey(device.getPublicKey());

        // 3. Decode raw payload bytes (Base64)
        byte[] payloadBytes = Base64.getDecoder().decode(dto.getPayload());
        byte[] signatureBytes = Base64.getDecoder().decode(dto.getSignature());

        // 4. Verify signature
        boolean valid = cryptoService.verify(payloadBytes, signatureBytes, pubKey);
        System.out.println("Payloads bytes: " + Arrays.toString(payloadBytes));
        System.out.println("Signature bytes: " + Arrays.toString(signatureBytes));
        if (!valid) {
            throw new RuntimeException("Invalid signature — request tampered");
        }

        // 5. Parse JSON inside payload
        String json = new String(payloadBytes, StandardCharsets.UTF_8);
        TransactionPayload payload;
        try {
            payload = objectMapper.readValue(json, TransactionPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid payload JSON", e);
        }

        String txId = payload.getTxId();
        double fare = payload.getFare();
        System.out.println("Fare in payload: " +  fare);
        long timestamp = payload.getTimestamp();

        // Validate fare amount
        if (fare <= 0) {
            throw new ValidationException("Invalid fare: amount must be positive");
        }
        if (fare > 10.0) {
            throw new ValidationException("Invalid fare: exceeds maximum €10.00");
        }

        long currentTime = System.currentTimeMillis();

        long maxAge = 24 * 60 * 60 * 1000; //24 hours timestamp

        if (Math.abs(currentTime - timestamp) > maxAge) {
            throw new ValidationException("Transaction expired: timestamp outside 24-hour window");
        }

        // 6. Prevent duplicate processing
        if (transactionRepo.existsByTxId(txId)) {
            Card card = device.getUser().getCard();
            return new RedeemResult("Already processed", card.getBalance(), fare);
        }

        // 7. Get user and card
        Card card = device.getUser().getCard();

        // 8. Check balance
        if (card.getBalance() < fare) {
            return new RedeemResult("Insufficient funds", card.getBalance(), fare);
        }

        // 9. Deduct balance
        double newBalance = card.getBalance() - fare;
        card.setBalance(newBalance);
        cardRepo.save(card);

        // 10. Save transaction
        Transaction tx = new Transaction();
        tx.setTxId(txId);                     // device-generated uuid
        tx.setCard(card);
        tx.setType("DEDUCT");
        tx.setAmount(fare);
        tx.setSignature(dto.getSignature());
        tx.setTimestamp(timestamp);
        tx.setProcessed(true);
//LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        transactionRepo.save(tx);

        return new RedeemResult("Success", newBalance, fare);
    }


}
