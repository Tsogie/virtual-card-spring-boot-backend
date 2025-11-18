package net.otgon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RedeemOfflineRequest {

    /** Unique transaction ID from device */
    private String txId;

    /** Device identifier (maps to wallet / user) */
    private String deviceKey;

    /** Fare to deduct */
    private double fare;

    /** Original timestamp from device (epoch milliseconds) */
    private long timestamp;
}
