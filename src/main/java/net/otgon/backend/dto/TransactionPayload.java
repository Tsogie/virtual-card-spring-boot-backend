package net.otgon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionPayload {
    private String txId;
    private double fare;
    private long timestamp;
}
