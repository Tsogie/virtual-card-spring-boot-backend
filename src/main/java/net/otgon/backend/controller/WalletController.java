package net.otgon.backend.controller;


import lombok.AllArgsConstructor;
import net.otgon.backend.dto.*;
import net.otgon.backend.service.RedeemService;
import net.otgon.backend.service.TokenService;
import net.otgon.backend.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/wallet")
@AllArgsConstructor
public class WalletController {

    private TokenService tokenService;
    private WalletService walletService;
    private RedeemService redeemService;

    @GetMapping("/{cardId}")
    public ResponseEntity<String> generateQr(@PathVariable String cardId) throws Exception {

        String token = tokenService.generateSignedToken(cardId);

        return ResponseEntity.ok(token);
    }


    //ResponseEntity<Map<String, Object>>
//    @PostMapping("/redeem")
//    public ResponseEntity<RedeemResult> redeemee(@RequestBody RedeemRequestDto redeemRequestDto) {
//        RedeemResult result = tokenService.redeemByToken(redeemRequestDto);
//        return ResponseEntity.ok(result);
//    }

//    @PostMapping("/redeem-device")
//    public ResponseEntity<RedeemResult> redeemByDevice(@RequestBody RedeemDeviceRequestDto dto) {
//        return ResponseEntity.ok(redeemService.redeemFare(dto));
//    }
//
//    @PostMapping("/redeem-offline")
//    public ResponseEntity<RedeemResult> redeemOffline(@RequestBody RedeemOfflineRequest dto) {
//        return ResponseEntity.ok(redeemService.redeemOfflineFare(dto));
//    }


    @PostMapping("/redeem")
    public ResponseEntity<?> redeem(@RequestBody RedeemDeviceRequestDto dto){
        RedeemResult result = redeemService.redeem(dto);
        return ResponseEntity.ok(result);

    }

    @PutMapping("/topup/{cardId}")
    public ResponseEntity<TopUpResponse> topup(@PathVariable String cardId){
            return ResponseEntity.ok(walletService.topup(cardId));
    }

}
