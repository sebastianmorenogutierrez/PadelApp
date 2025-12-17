package com.example.web;

import com.example.servicio.UsuarioDetailsServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class ConfSeg {

    private final UsuarioDetailsServices usuarioDetailsServices;

    public ConfSeg(UsuarioDetailsServices usuarioDetailsServices) {
        this.usuarioDetailsServices = usuarioDetailsServices;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsServices);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/API/registro").permitAll()

                        // Permitir todas las rutas de acceso público (principalmente GET)
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**",
                                "/", "/principal", "/login", "/registro",
                                "/login?rolDesconocido"
                        ).permitAll()

                        // Rutas que requieren autenticación
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers(
                                "/correo/enviar", "/correo/masivo", "/correo/individual/{idUsuario}",
                                "/correo/estadisticas", "/correo/estado", "/correo/previsualizar",
                                "/correo/cancelar"
                        ).authenticated()
                        .requestMatchers("/torneo/**").authenticated()
                        .requestMatchers("/partido/**").authenticated()
                        .requestMatchers(
                                "/anexar", "/salvar", "/cambiar/{idIndividuo}", "/cambiar/guardar",
                                "/borrar/{idIndividuo}", "/jugadores", "/jugadores-registrados"
                        ).authenticated()

                        .requestMatchers("/equipo", "/equipo/**").authenticated()

                        .requestMatchers(
                                "/redirigir", "/indicejugador", "/indice", "/datos", "/modificar",
                                "/eliminarCuenta", "/torneos-vista", "/exportarExcel"
                        ).authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/redirigir", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // Configuración de Logout
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}