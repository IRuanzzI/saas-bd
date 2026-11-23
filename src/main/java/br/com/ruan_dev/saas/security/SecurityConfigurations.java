package br.com.ruan_dev.saas.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfigurations {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Página de erro interna do Spring: sem isso, qualquer erro (ex.: JSON
                        // inválido) vira um 403 mudo em vez do status real (400, 404...).
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/clientes")
                            .hasAnyAuthority("ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/clientes/edit/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/clientes/delete/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/clientes/create")
                            .hasAnyAuthority("ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tenants/register").hasAuthority("MASTER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/tenants/me")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")
                        // Painel do MASTER: todo o resto de /tenants (lista, detalhe, suspender...)
                        .requestMatchers("/api/v1/tenants/**").hasAuthority("MASTER")
                        .requestMatchers("/api/v1/whatsapp/**").hasAnyAuthority("MASTER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/create").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me")
                            .hasAnyAuthority("ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/agendamentos", "/api/v1/agendamentos/hoje")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/agendamentos/cliente")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/agendamentos/edit/**")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/agendamentos/delete/**")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agendamentos/create")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")

                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/evolution").permitAll()


                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * CORS só importa para o app rodando no navegador (Flutter web); em
     * Android/Windows não é usado. Em produção, defina
     * app.cors.allowed-origin-patterns com o endereço real do app.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
            List<String> origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origens);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.boot.CommandLineRunner gerarHash(PasswordEncoder encoder) {
        return args -> System.out.println(">>> USE ESTE HASH NO BANCO: " + encoder.encode("123456"));
    }
}