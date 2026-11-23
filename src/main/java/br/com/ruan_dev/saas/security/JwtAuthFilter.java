package br.com.ruan_dev.saas.security;

import br.com.ruan_dev.saas.services.TenantAcessoService;
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

    @Autowired
    private TenantAcessoService tenantAcessoService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Substituído 'var' por 'String'
        String token = this.recoverToken(request);

        try {
            if (token != null) {
                try {
                    DecodedJWT decodedJWT = jwtTokenProvider.verifyToken(token);
                    String nome = decodedJWT.getSubject();
                    String cargo = decodedJWT.getClaim("position").asString();
                    String tenantId = decodedJWT.getClaim("tenantId").asString();

                    // Empresa suspensa/vencida: o token deixa de valer na hora.
                    String bloqueio = tenantAcessoService.motivoBloqueio(
                            tenantId != null ? Long.valueOf(tenantId) : null, cargo);
                    if (bloqueio != null) {
                        responderBloqueio(response, bloqueio);
                        return;
                    }

                    if (tenantId != null) {
                        TenantContext.setCurrentTenant(tenantId);
                    }
                    TenantContext.setCurrentUsuarioEmail(nome);

                    // Substituído 'var' pelos tipos explícitos
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(cargo));
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(nome, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    System.out.println("Token inválido ou expirado: " + e.getMessage());
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void responderBloqueio(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":403,\"message\":\"" + mensagem.replace("\"", "'") + "\"}");
    }

    private String recoverToken(HttpServletRequest request) {
        // Substituído 'var' por 'String'
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
