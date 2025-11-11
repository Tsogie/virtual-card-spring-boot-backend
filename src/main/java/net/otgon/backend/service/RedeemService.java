package net.otgon.backend.service;

import jakarta.transaction.Transactional;
import net.otgon.backend.dto.RedeemDeviceRequestDto;
import net.otgon.backend.dto.RedeemResult;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Device;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.DeviceRepo;
import org.springframework.stereotype.Service;

@Service
public class RedeemService {

    private final DeviceRepo deviceRepo;
    private final CardRepo cardRepo;

    public RedeemService(DeviceRepo deviceRepo, CardRepo cardRepo) {
        this.deviceRepo = deviceRepo;
        this.cardRepo = cardRepo;
    }

    @Transactional
    public RedeemResult redeemFare(RedeemDeviceRequestDto dto) {
        // 1. Find device by deviceKey
        Device device = deviceRepo.findByDeviceKey(dto.getDeviceKey())
                .orElseThrow(() -> new RuntimeException("Device not registered"));

        double fare = dto.getFare();

        // 2. Get user and card (lazy-safe because of @Transactional)
        User user = device.getUser();
        Card card = user.getCard();

        // 3. Check balance
        if (card.getBalance() < fare) {
            return new RedeemResult("Insufficient funds", card.getBalance(), fare);
        }

        // 4. Deduct fare
        double newBalance = card.getBalance() - fare;
        card.setBalance(newBalance);
        cardRepo.save(card);

        return new RedeemResult("Success", newBalance, fare);
    }

}
