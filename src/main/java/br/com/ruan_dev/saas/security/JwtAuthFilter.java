package br.com.ruan_dev.saas.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Substituído 'var' por 'String'
        String token = this.recoverToken(request);

        if (token != null) {
            try {
                DecodedJWT decodedJWT = jwtTokenProvider.verifyToken(token);
                String nome = decodedJWT.getSubject();
                String cargo = decodedJWT.getClaim("position").asString();
                String tenantId = decodedJWT.getClaim("tenantId").asString();

                if (tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId);
                }

                // Substituído 'var' pelos tipos explícitos
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(cargo));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(nome, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                System.out.println("Token inválido ou expirado: " + e.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        // Substituído 'var' por 'String'
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}