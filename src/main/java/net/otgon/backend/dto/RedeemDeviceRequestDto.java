package net.otgon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RedeemDeviceRequestDto {

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "Payload is required")
    private String payload;     // Raw transaction JSON, Base64 encoded

    @NotBlank(message = "Signature is required")
    private String signature;

}
