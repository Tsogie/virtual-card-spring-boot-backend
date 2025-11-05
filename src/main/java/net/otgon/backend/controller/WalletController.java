package net.otgon.backend.controller;


import lombok.AllArgsConstructor;
import net.otgon.backend.dto.RedeemRequestDto;
import net.otgon.backend.service.QrService;
import net.otgon.backend.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/wallet")
@AllArgsConstructor
public class WalletController {

    private QrService qrService;
    private WalletService walletService;

    @GetMapping("/{cardId}")
    public ResponseEntity<String> generateQr(@PathVariable String cardId) throws Exception {

        String token = qrService.generateSignedToken(cardId);

        return ResponseEntity.ok(token);
    }


    //ResponseEntity<Map<String, Object>>
    @PostMapping("/redeem")
    public ResponseEntity<String> redeemQR(@RequestBody RedeemRequestDto redeemRequestDto) {

        System.out.println("Incoming token: " + redeemRequestDto.getToken());

        String resultMessage = qrService.redeemByQr(redeemRequestDto);

        //Map<String, Object> response = new HashMap<>();
        //response.put("status", resultMessage);
        //response.put("requested fare", redeemRequestDto.getFare());
        return ResponseEntity.ok(resultMessage);
    }


    @PutMapping("/topup/{cardId}")
    public ResponseEntity<String> topup(@PathVariable String cardId){
        String response = walletService.topup(cardId);
        return ResponseEntity.ok(response);

    }

//    public ResponseBody<String> getTransactions(){
//
//
//    }

}
