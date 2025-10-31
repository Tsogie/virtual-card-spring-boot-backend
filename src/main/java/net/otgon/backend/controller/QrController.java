package net.otgon.backend.controller;

import lombok.AllArgsConstructor;
import net.otgon.backend.dto.RedeemRequestDto;
import net.otgon.backend.service.QrService;
import net.otgon.backend.util.QrCodeUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/qrcode")
@AllArgsConstructor
public class QrController {

    private QrService qrService;

    @GetMapping("/{cardId}")
    public ResponseEntity<byte[]> generateQr(@PathVariable String cardId) throws Exception {
        // 1. Generate short-lived signed token
        String token = qrService.generateSignedToken(cardId);

        // 2. Generate QR image as PNG bytes
        byte[] qrImage = QrCodeUtil.generateQrImage(token, 300, 300);

        // 3. Encode as Base64 string to send to app
        String base64Qr = Base64.getEncoder().encodeToString(qrImage);

        return ResponseEntity.ok(qrImage);
    }

//    @PostMapping("/redeem")
//    public ResponseEntity<String> redeemQR(@RequestBody RedeemRequestDto redeemRequestDto) {
//        String response = qrService.redeemByQr(redeemRequestDto);
//        return ResponseEntity.ok(response);
//    }

    @PostMapping("/redeem")
    public ResponseEntity<Map<String, Object>> redeemQR(@RequestBody RedeemRequestDto redeemRequestDto) {
        String resultMessage = qrService.redeemByQr(redeemRequestDto);

        Map<String, Object> response = new HashMap<>();
        response.put("status", resultMessage);  // e.g., "Success" or error message
        response.put("fareDeducted", redeemRequestDto.getFare()); // optional
        return ResponseEntity.ok(response);
    }


    /*
      var template = `
      <img id="qrcode" src="{{response.qrCode}}" alt="QR Code" />
      `;
      function constructVisualizerPayload() {
          var buffer = pm.response.stream; // Get the response stream
          var base64String = btoa(String.fromCharCode.apply(null, new Uint8Array(buffer))); // Convert to base64
          var url = "data:image/png;base64," + base64String; // Create a base64 URL
          return { response: { qrCode: url } }; // Return the URL
      }
      pm.visualizer.set(template, constructVisualizerPayload());
      *
      */

}
