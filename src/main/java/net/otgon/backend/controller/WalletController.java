package net.otgon.backend.controller;


import lombok.AllArgsConstructor;
import net.otgon.backend.dto.*;
import net.otgon.backend.service.RedeemService;
import net.otgon.backend.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/wallet")
@AllArgsConstructor
public class WalletController {


    private WalletService walletService;
    private RedeemService redeemService;


    @PostMapping("/redeem")
    public ResponseEntity<?> redeem(@RequestBody RedeemDeviceRequestDto dto){
        RedeemResult result = redeemService.redeem(dto);
        return ResponseEntity.ok(result);

    }

    @PutMapping("/topup/{cardId}")
    public ResponseEntity<TopUpResponse> topup(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable String cardId){
            return ResponseEntity.ok(walletService.topup(cardId));
    }

}
