package net.otgon.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import net.otgon.backend.dto.RedeemRequestDto;
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
public class QrService {

    private final QrTokenRepo qrTokenRepo;
    private final CardRepo cardRepo;

    public QrService(QrTokenRepo qrTokenRepo, CardRepo cardRepo) {
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
        token.setExpiresAt(LocalDateTime.now().plusSeconds(60));
        token.setUsed(false);

        qrTokenRepo.save(token);

        return jwt;

    }

    public String redeemByQr(RedeemRequestDto redeemRequestDto) {

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
            return "Token has expired";
        } catch (JwtException e) {
            return "Invalid token";
        }

        String tokenId = claims.getId();

        QrToken foundToken = qrTokenRepo.findById(tokenId).orElseThrow();

        if(foundToken.isUsed()){
            return "Token is already used";
        }else{

            Card foundCard = cardRepo.findById(foundToken.getCardId()).orElseThrow();
            if(foundCard.getBalance() < fare){
                return "Insufficient funds";
            }

            foundCard.setBalance(foundCard.getBalance() - fare);
            cardRepo.save(foundCard);
            foundToken.setUsed(true);
            qrTokenRepo.save(foundToken);
        }
        return "Success";
    }
}

