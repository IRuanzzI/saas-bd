package br.com.ruan_dev.saas.security;

import br.com.ruan_dev.saas.models.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenProvider {

    private String secret = "saas_secret";

    public String generateToken(Usuario user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);

        return JWT.create()
                .withIssuer("saas-api")
                .withSubject(user.getEmail())
                .withClaim("position", user.getRole())
                .withClaim("tenantId", user.getTenant().getId().toString())
                .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm)
                .withIssuer("saas-api")
                .build()
                .verify(token);
    }
}