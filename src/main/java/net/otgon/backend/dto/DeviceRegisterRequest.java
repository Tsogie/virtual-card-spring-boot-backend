package net.otgon.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class DeviceRegisterRequest {

    private String alias;

    @NotBlank(message = "Public key is required") // ← Add this
    private String publicKey;

    public DeviceRegisterRequest() {
    }

    public DeviceRegisterRequest(String alias, String publicKey) {
        this.alias = alias;
        this.publicKey = publicKey;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
