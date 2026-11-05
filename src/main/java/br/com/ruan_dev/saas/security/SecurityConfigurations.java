package br.com.ruan_dev.saas.security;

import org.springframework.beans.factory.annotation.Autowired;
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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/clientes").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/clientes/edit/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/clientes/delete/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tenants/register").hasAnyAuthority("MASTER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/create").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/agendamentos", "/api/v1/agendamentos/hoje")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/agendamentos/edit/**")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/agendamentos/delete/**")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agendamentos/create")
                            .hasAnyAuthority("MASTER", "ADMIN", "USUARIO")

                        .requestMatchers(HttpMethod.POST, "/api/v1/clientes/create")
                            .hasAnyAuthority("ADMIN", "USUARIO")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
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