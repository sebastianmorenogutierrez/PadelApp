package com.example.servicio;

import com.example.model.Pago; // Si el paquete es 'model'
import com.example.domain.PadelMatch; // Necesitas importar la clase PadelMatch
import com.example.domain.usuario.Usuario;
import com.example.domain.torneo.Torneo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat; // Importación necesaria
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Importación necesaria
import java.util.HashMap;
import java.util.List;
import java.util.Locale; // Importación necesaria
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CorreoServicio {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    // Estadísticas de envío
    private final AtomicInteger correosEnviados = new AtomicInteger(0);
    private final AtomicInteger correosError = new AtomicInteger(0);
    private LocalDateTime ultimoEnvio = LocalDateTime.now();

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

    public void enviarCorreoHTML(String para, String asunto, String nombreTemplate, Context context)
            throws MessagingException {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

        String contenidoHTML = templateEngine.process(nombreTemplate, context);

        helper.setTo(para);
        helper.setSubject(asunto);
        helper.setText(contenidoHTML, true);
        helper.setFrom("tu-email@empresa.com");

        mailSender.send(mensaje);
    }


    public void enviarConfirmacionPago(Pago pago) {
        try {
            Usuario usuario = pago.getUsuario();
            PadelMatch partido = pago.getPartido();

            // Formatos
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

            // 1. Crear el Contexto para Thymeleaf
            Context context = new Context();
            context.setVariable("usuarioNombre", usuario.getNombreUsuario());
            context.setVariable("montoFormateado", currencyFormat.format(pago.getMonto()));
            context.setVariable("nombrePartido", partido.getNombrePartido());
            context.setVariable("fechaPartido", partido.getFecha().format(dateFormatter));
            context.setVariable("horaPartido", partido.getHora().toString());
            context.setVariable("club", partido.getClub());
            context.setVariable("nivelJuego", partido.getNivelJuego());
            context.setVariable("idTransaccion", pago.getStripePaymentIntentId() != null ?
                    pago.getStripePaymentIntentId().substring(0, 20) + "..." :
                    "N/A");
            context.setVariable("fechaPago", pago.getFechaPago() != null ?
                    pago.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) :
                    "N/A");

            String asunto = "✅ Confirmación de Pago - " + partido.getNombrePartido();
            // Asegúrate de que este archivo exista en src/main/resources/templates/email/confirmacion-pago.html
            String templateName = "email/confirmacion-pago";

            // 2. Usar el método general de envío HTML
            enviarCorreoHTML(usuario.getIndividuo().getCorreo(), asunto, templateName, context);

            correosEnviados.incrementAndGet();
            System.out.println("Email de confirmación de pago enviado a: " + usuario.getIndividuo().getCorreo());

        } catch (MessagingException e) {
            correosError.incrementAndGet();
            System.err.println("Error al enviar email de confirmación de pago: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> enviarCorreoMasivo(List<Usuario> jugadores, String asunto,
                                                      String mensaje, String tipoEvento) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("Iniciando envío masivo - Total destinatarios: " + jugadores.size());
            int exitosos = 0;
            int fallos = 0;

            for (Usuario jugador : jugadores) {
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
                        Thread.sleep(100); // Evitar spam

                    } catch (Exception e) {
                        fallos++;
                        correosError.incrementAndGet();
                        System.err.println("Error enviando correo a: " +
                                jugador.getIndividuo().getCorreo() + " - " + e.getMessage());
                    }
                }
            }

            ultimoEnvio = LocalDateTime.now();
            System.out.println("Envío masivo completado - Exitosos: " + exitosos + ", Fallos: " + fallos);
        });
    }

    // NUEVO: Método para envío individual (compatible con el controlador)
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

    // NUEVOS: Métodos adicionales para el controlador
    public boolean verificarConexion() {
        try {
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
        // En una implementación más avanzada, aquí cancelarías tareas pendientes
        System.out.println("Solicitud de cancelación de envíos pendientes recibida");
        return true;
    }
}