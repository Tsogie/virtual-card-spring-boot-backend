package net.otgon.backend.controller;


import lombok.AllArgsConstructor;
import net.otgon.backend.dto.RedeemDeviceRequestDto;
import net.otgon.backend.dto.RedeemRequestDto;
import net.otgon.backend.dto.RedeemResult;
import net.otgon.backend.dto.TopUpResponse;
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
    @PostMapping("/redeem")
    public ResponseEntity<RedeemResult> redeem(@RequestBody RedeemRequestDto redeemRequestDto) {
        RedeemResult result = tokenService.redeemByToken(redeemRequestDto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/redeem-device")
    public ResponseEntity<RedeemResult> redeemByDevice(@RequestBody RedeemDeviceRequestDto dto) {
        return ResponseEntity.ok(redeemService.redeemFare(dto));
    }



    @PutMapping("/topup/{cardId}")
    public ResponseEntity<TopUpResponse> topup(@PathVariable String cardId){
            return ResponseEntity.ok(walletService.topup(cardId));
    }

}
