package com.example.servicio;

import com.example.domain.PadelMatch; // Asumida, necesaria por los comentarios
import com.example.domain.usuario.Usuario;
import com.example.domain.torneo.Torneo; // Asumida, necesaria por los comentarios

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async; // Necesaria para el envío asíncrono
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio encargado de la gestión y envío de correos electrónicos.
 * Utiliza JavaMailSender para la conexión SMTP y Thymeleaf para la generación de plantillas HTML.
 */
@Service
public class CorreoServicio {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    // Estadísticas atómicas para medir el rendimiento de los envíos.
    private final AtomicInteger correosEnviados = new AtomicInteger(0);
    private final AtomicInteger correosError = new AtomicInteger(0);
    private LocalDateTime ultimoEnvio = LocalDateTime.now();

    // --------------------------------------------------------------------
    // Métodos de Envío Básico
    // --------------------------------------------------------------------

    /**
     * Envía un correo de texto plano simple.
     */
    public void enviarCorreoSimple(String para, String asunto, String mensaje) {
        try {
            SimpleMailMessage correo = new SimpleMailMessage();
            correo.setTo(para);
            correo.setSubject(asunto);
            correo.setText(mensaje);
            correo.setFrom("tu-email@empresa.com");

            mailSender.send(correo);
            correosEnviados.incrementAndGet();
            System.out.println("Correo simple enviado a: " + para);
        } catch (Exception e) {
            correosError.incrementAndGet();
            System.err.println("Error enviando correo simple a " + para + ": " + e.getMessage());
        }
    }

    /**
     * Envía un correo utilizando una plantilla HTML de Thymeleaf.
     * @throws MessagingException Si hay un problema al crear o enviar el mensaje MIME.
     */
    public void enviarCorreoHTML(String para, String asunto, String nombreTemplate, Context context)
            throws MessagingException {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

        // Procesa la plantilla Thymeleaf para obtener el contenido HTML
        String contenidoHTML = templateEngine.process(nombreTemplate, context);

        helper.setTo(para);
        helper.setSubject(asunto);
        helper.setText(contenidoHTML, true); // El 'true' indica que el contenido es HTML.
        helper.setFrom("tu-email@empresa.com");

        mailSender.send(mensaje);
        // Nota: La métrica 'correosEnviados' se incrementa dentro de los métodos que llaman a este.
    }

    // --------------------------------------------------------------------
    // Métodos de Envío Asíncrono (Usando CompletableFuture)
    // --------------------------------------------------------------------

    /**
     * Inicia el envío de correos masivos a una lista de jugadores de forma asíncrona.
     */
    @Async // La anotación es útil aquí aunque CompletableFuture también maneja hilos.
    public CompletableFuture<Void> enviarCorreoMasivo(List<Usuario> jugadores, String asunto,
                                                      String mensaje, String tipoEvento) {
        // Utilizamos CompletableFuture.runAsync para ejecutar la lógica en un hilo separado.
        return CompletableFuture.runAsync(() -> {
            System.out.println("Iniciando envío masivo - Total destinatarios: " + jugadores.size());
            int exitosos = 0;
            int fallos = 0;

            for (Usuario jugador : jugadores) {
                // Validación de que el usuario tiene un individuo y un correo válido.
                if (jugador.getIndividuo() != null &&
                        jugador.getIndividuo().getCorreo() != null &&
                        !jugador.getIndividuo().isEliminado()) {

                    try {
                        Context context = new Context();
                        context.setVariable("nombreJugador",
                                jugador.getIndividuo().getNombre() + " " + jugador.getIndividuo().getApellido());
                        context.setVariable("mensaje", mensaje);
                        context.setVariable("tipoEvento", tipoEvento);

                        enviarCorreoHTML(jugador.getIndividuo().getCorreo(), asunto,
                                "email/notificacion", context);

                        exitosos++;
                        correosEnviados.incrementAndGet();
                        Thread.sleep(100); // Pequeña pausa anti-spam

                    } catch (Exception e) {
                        fallos++;
                        correosError.incrementAndGet();
                        System.err.println("Error enviando correo masivo a: " +
                                jugador.getIndividuo().getCorreo() + " - " + e.getMessage());
                    }
                }
            }
            ultimoEnvio = LocalDateTime.now();
            System.out.println("Envío masivo completado - Exitosos: " + exitosos + ", Fallos: " + fallos);
        });
    }

    /**
     * Envía un correo individual de forma asíncrona (usa CompletableFuture.supplyAsync).
     */
    public CompletableFuture<Boolean> enviarCorreoIndividual(Usuario jugador, String asunto, String mensaje) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (jugador.getIndividuo() != null &&
                        jugador.getIndividuo().getCorreo() != null &&
                        !jugador.getIndividuo().isEliminado()) {

                    Context context = new Context();
                    context.setVariable("nombreJugador",
                            jugador.getIndividuo().getNombre() + " " + jugador.getIndividuo().getApellido());
                    context.setVariable("mensaje", mensaje);
                    context.setVariable("tipoEvento", "INDIVIDUAL");

                    enviarCorreoHTML(jugador.getIndividuo().getCorreo(), asunto,
                            "email/notificacion", context);

                    correosEnviados.incrementAndGet();
                    return true;
                }
            } catch (Exception e) {
                correosError.incrementAndGet();
                System.err.println("Error enviando correo individual: " + e.getMessage());
            }
            return false;
        });
    }

    /**
     * Notifica a los jugadores sobre un nuevo torneo.
     */
    public CompletableFuture<Void> notificarNuevoTorneo(List<Usuario> jugadores, Torneo torneo) {
        return CompletableFuture.runAsync(() -> {
            String asunto = "Nuevo Torneo: " + torneo.getNombre();

            for (Usuario jugador : jugadores) {
                if (jugador.getIndividuo() != null &&
                        jugador.getIndividuo().getCorreo() != null &&
                        !jugador.getIndividuo().isEliminado()) {

                    try {
                        Context context = new Context();
                        context.setVariable("nombreJugador",
                                jugador.getIndividuo().getNombre() + " " + jugador.getIndividuo().getApellido());
                        context.setVariable("torneo", torneo);

                        enviarCorreoHTML(jugador.getIndividuo().getCorreo(), asunto,
                                "email/nuevo-torneo", context);

                        Thread.sleep(150);
                    } catch (Exception e) {
                        System.err.println("Error enviando notificación de torneo a: " +
                                jugador.getIndividuo().getCorreo() + " - " + e.getMessage());
                    }
                }
            }
        });
    }

    // --------------------------------------------------------------------
    // Métodos de Utilidad y Estadísticas
    // --------------------------------------------------------------------

    public boolean verificarConexion() {
        try {
            // Intenta crear un mensaje MIME; si funciona, la configuración básica está OK.
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            System.err.println("Error de conexión del servicio de correo: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> obtenerEstadisticasEnvio() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("correosEnviados", correosEnviados.get());
        stats.put("correosConError", correosError.get());
        stats.put("ultimoEnvio", ultimoEnvio);
        stats.put("servicioActivo", verificarConexion());
        return stats;
    }

    public Map<String, Object> generarPrevisualizacion(String asunto, String mensaje, String tipoEvento) {
        Map<String, Object> preview = new HashMap<>();
        preview.put("asunto", "[" + tipoEvento + "] " + asunto);
        preview.put("mensaje", mensaje);
        preview.put("tipoEvento", tipoEvento);
        preview.put("fechaCreacion", LocalDateTime.now());
        return preview;
    }

    public boolean cancelarEnviosPendientes() {
        System.out.println("Solicitud de cancelación de envíos pendientes recibida (Simulación)");
        // En una implementación real, esto interactuaría con un ThreadPoolExecutor para cancelar tareas.
        return true;
    }
}