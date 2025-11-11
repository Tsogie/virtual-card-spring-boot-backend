package net.otgon.backend.controller;

import lombok.AllArgsConstructor;
import net.otgon.backend.dto.RedeemRequestDto;
import net.otgon.backend.dto.RedeemResult;
import net.otgon.backend.service.TokenService;
import net.otgon.backend.util.QrCodeUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/qrcode")
@AllArgsConstructor
public class JwtController {

    private TokenService tokenService;

    @GetMapping("/{cardId}")
    public ResponseEntity<byte[]> generateToken(@PathVariable String cardId) throws Exception {
        // 1. Generate short-lived signed token
        String token = tokenService.generateSignedToken(cardId);

        // 2. Generate QR image as PNG bytes
        byte[] qrImage = QrCodeUtil.generateQrImage(token, 300, 300);

        // 3. Encode as Base64 string to send to app
        String base64Qr = Base64.getEncoder().encodeToString(qrImage);

        return ResponseEntity.ok(qrImage);
    }

    @PostMapping("/redeem")
    public ResponseEntity<RedeemResult> redeem(@RequestBody RedeemRequestDto redeemRequestDto) {
        RedeemResult result = tokenService.redeemByToken(redeemRequestDto);
        return ResponseEntity.ok(result);
    }


}
