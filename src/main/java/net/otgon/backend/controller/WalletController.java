package net.otgon.backend.controller;


import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import net.otgon.backend.dto.*;
import net.otgon.backend.service.RedeemService;
import net.otgon.backend.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/wallet")
@AllArgsConstructor
public class WalletController {

    private WalletService walletService;
    private RedeemService redeemService;

    @PostMapping("/redeem")
    public ResponseEntity<?> redeem(@Valid @RequestBody RedeemDeviceRequestDto dto){
        RedeemResult result = redeemService.redeem(dto);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/topup")
    public TopUpResponse topup(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TopUpRequest request) {
        // Extract JWT from "Bearer <token>"
        String token = authHeader.replace("Bearer ", "");

        return walletService.topup(token, request.getAmount());
    }

}
