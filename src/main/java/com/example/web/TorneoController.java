package com.example.web;

import com.example.domain.torneo.Torneo;
import com.example.servicio.TorneoServicio;
import com.example.servicio.UsuarioServicio;
import com.example.domain.usuario.Usuario;
import com.example.domain.Individuo;
// ... (Otros imports) ...
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ... (Imports de PDF) ...
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/torneo")
public class TorneoController {

    private static final Logger logger = LoggerFactory.getLogger(TorneoController.class);

    @Autowired
    private TorneoServicio torneoService;

    @Autowired
    private UsuarioServicio usuarioServicio;

    // ... (Otros métodos listar, formularioNuevo, guardar, eliminarTorneo) ...

    @GetMapping("/editar/{id}")
    public String editarTorneo(@PathVariable Long id, Model model) {
        // ... (Tu código existente para editar, sin cambios) ...
        return "formulario_torneo";
    }

    // 🏆 MÉTODO CORREGIDO 1: Agregando la verificación 'yaInscrito'
    @GetMapping("/{id}")
    public String verTorneo(@PathVariable Long id, Model model) {
        Torneo torneo = torneoService.buscarPorId(id).orElseThrow(
                () -> new NoSuchElementException("Torneo no encontrado con ID: " + id)
        );

        Usuario usuarioActual = usuarioServicio.obtenerUsuarioActual();
        boolean esAdministrador = usuarioServicio.esAdministrador(usuarioActual);

        // 🏆 VERIFICACIÓN DE INSCRIPCIÓN
        boolean yaInscrito = false;
        if (usuarioActual != null) {
            yaInscrito = torneoService.esParticipante(torneo, usuarioActual);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String fechaInicioFormateada = torneo.getFechaInicio().format(formatter);
        String fechaFinFormateada = torneo.getFechaFin().format(formatter);

        model.addAttribute("torneo", torneo);
        model.addAttribute("fechaInicioFormateada", fechaInicioFormateada);
        model.addAttribute("fechaFinFormateada", fechaFinFormateada);
        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("esAdministrador", esAdministrador);
        model.addAttribute("yaInscrito", yaInscrito); // <--- ¡AÑADIDO!

        return "torneo_detalle";
    }

    // 🏆 MÉTODO CORREGIDO 2: Obtiene el Usuario logueado y maneja excepciones
    @PostMapping("/{id}/inscribir")
    public String inscribir(@PathVariable Long id, Model model) { // Eliminamos @RequestParam String nombre
        try {
            Usuario usuarioActual = usuarioServicio.obtenerUsuarioActual();

            if (usuarioActual == null) {
                // Esto debería ser raro, pero mejor manejarlo
                throw new IllegalStateException("Debe iniciar sesión para inscribirse.");
            }

            // Llama al servicio con el ID del torneo y el objeto Usuario
            torneoService.inscribirUsuario(id, usuarioActual);

            model.addAttribute("mensajeExito", "¡Inscripción exitosa al torneo!");

        } catch (IllegalStateException e) {
            // Captura si ya está inscrito, si no está logueado, o si el torneo no existe
            model.addAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            logger.error("Error al inscribir al usuario", e);
            model.addAttribute("mensajeError", "Ocurrió un error inesperado al procesar la inscripción.");
        }

        // Importante: Llamamos a verTorneo para recargar la vista con los mensajes y el estado de inscripción
        return verTorneo(id, model);
    }

    // ... (El resto de tus métodos de Controller, como generarPDFTorneo, van aquí) ...
}