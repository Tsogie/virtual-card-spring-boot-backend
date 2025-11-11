package net.otgon.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import net.otgon.backend.dto.RedeemRequestDto;
import net.otgon.backend.dto.RedeemResult;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.QrToken;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.QrTokenRepo;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenService {

    private final QrTokenRepo qrTokenRepo;
    private final CardRepo cardRepo;

    public TokenService(QrTokenRepo qrTokenRepo, CardRepo cardRepo) {
        this.qrTokenRepo = qrTokenRepo;
        this.cardRepo = cardRepo;
    }

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generateSignedToken(String cardId) {
        Instant now = Instant.now();
        String jwt =  Jwts.builder()
                .claim("cardId", cardId)
                .setId(UUID.randomUUID().toString())   // jti
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(60)))
                .signWith(key)
                .compact();

        // save token to db
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
        String jti = claims.getId();

        QrToken token = new QrToken();
        token.setJti(jti);
        token.setCardId(cardId);
        token.setIssuedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(180));
        token.setUsed(false);

        qrTokenRepo.save(token);

        return jwt;

    }

    public RedeemResult redeemByToken(RedeemRequestDto redeemRequestDto) {
        String token = redeemRequestDto.getToken();
        double fare = redeemRequestDto.getFare();

        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return new RedeemResult("Token has expired", -1, fare);
        } catch (JwtException e) {
            return new RedeemResult("Invalid token", -1, fare);
        }

        String tokenId = claims.getId();

        QrToken foundToken = qrTokenRepo.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        if (foundToken.isUsed()) {
            return new RedeemResult("Token is already used", -1, fare);
        }

        Card foundCard = cardRepo.findById(foundToken.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        if (foundCard.getBalance() < fare) {
            return new RedeemResult("Insufficient funds", foundCard.getBalance(), fare);
        }

        double newBalance = foundCard.getBalance() - fare;
        foundCard.setBalance(newBalance);
        cardRepo.save(foundCard);

        foundToken.setUsed(true);
        qrTokenRepo.save(foundToken);

        return new RedeemResult("Success", newBalance, fare);
    }

}

