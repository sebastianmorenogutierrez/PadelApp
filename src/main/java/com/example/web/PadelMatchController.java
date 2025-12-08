package com.example.web;

import com.example.domain.PadelMatch;
import com.example.domain.Individuo;
import com.example.domain.usuario.Usuario;
import com.example.servicio.PadelMatchService;
import com.example.servicio.UsuarioServicio;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.kernel.geom.PageSize;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/partido")
public class PadelMatchController {

    private final PadelMatchService padelMatchService;
    private final UsuarioServicio usuarioServicio;

    @Autowired
    public PadelMatchController(PadelMatchService padelMatchService, UsuarioServicio usuarioServicio) {
        this.padelMatchService = padelMatchService;
        this.usuarioServicio = usuarioServicio;
    }

    @GetMapping
    public String listarPartidosGestion(Model model) {
        model.addAttribute("matches", padelMatchService.listarTodos());
        return "lista_partidos_gestion";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("partido", new PadelMatch());
        // 🛠️ CORRECCIÓN 1: Devuelve la plantilla del formulario de creación (partido.html)
        return "partido";
    }

    @PostMapping("/crear")
    public String crearPartido(@ModelAttribute("partido") PadelMatch padelMatch,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {

        String nombreUsuario = auth.getName();
        Usuario creador = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

        if (creador == null || creador.getId_usuario() == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error: No se pudo identificar al usuario creador.");
            return "redirect:/partido";
        }

        Integer creadorId = creador.getId_usuario();

        try {
            padelMatchService.crearPartidoYRegistrarCreador(padelMatch, creadorId);
            redirectAttributes.addFlashAttribute("mensajeExito", "🎾 ¡Partido creado exitosamente! Has sido registrado automáticamente como jugador.");
            return "redirect:/partido";

        } catch (RuntimeException e) {
            System.err.println("Error al guardar el partido: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "⚠️ Error al crear el partido: " + e.getMessage());
            // Si falla el POST, redirecciona al GET /partido/nuevo
            return "redirect:/partido/nuevo";
        }
    }

    @GetMapping("/gestion")
    public String listarPartidosGestionRedundante(Model model) {
        model.addAttribute("matches", padelMatchService.listarTodos());
        // 🛠️ CORRECCIÓN 2: Este método de gestión debe devolver la lista, no la plantilla de detalle.
        return "lista_partidos_gestion";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminarPartido(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            padelMatchService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Partido eliminado exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al eliminar el partido.");
        }
        return "redirect:/partido";
    }

    @GetMapping("/{id}")
    public String verPartido(@PathVariable Long id,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Authentication auth) {

        String nombreUsuarioLogueado = (auth != null) ? auth.getName() : null;

        return padelMatchService.buscarPorId(id)
                .map(match -> {
                    model.addAttribute("partido", match);

                    boolean yaInscrito = false;
                    // boolean yaPago = false;  <-- Eliminada la variable de pago

                    if (nombreUsuarioLogueado != null) {
                        // Verificar si está inscrito
                        yaInscrito = match.getJugadores().stream()
                                .anyMatch(j -> j.getNombreUsuario().equals(nombreUsuarioLogueado));

                        // ⭐️ CAMBIO 2: Eliminada la lógica de verificación de pagos (Usuario, match, stripeService)
                    }

                    model.addAttribute("usuarioYaInscrito", yaInscrito);
                    // model.addAttribute("usuarioYaPago", yaPago); <-- Eliminada la variable de pago

                    return "partido_detalle";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("mensajeError", "Partido no encontrado.");
                    return "redirect:/partido";
                });
    }

    // ⭐️ CAMBIO 3: Eliminada la inyección incompleta @Autowired private

    @PostMapping("/{matchId}/inscribir")
    public String inscribirJugadorPorNombre(
            @PathVariable Long matchId,
            @RequestParam("nombreJugador") String nombre,
            RedirectAttributes redirectAttributes) {
        try {
            Usuario jugadorAInscribir = usuarioServicio.localizarPorNombreUsuario(nombre);

            if (jugadorAInscribir == null) {
                redirectAttributes.addFlashAttribute("mensajeError", "⚠️ Jugador con nombre '" + nombre + "' no encontrado.");
                return "redirect:/partido/" + matchId;
            }

            padelMatchService.inscribirJugador(matchId, jugadorAInscribir.getId_usuario());

            redirectAttributes.addFlashAttribute("mensajeExito", "✅ Jugador '" + nombre + "' inscrito exitosamente.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("mensajeAdvertencia", "❌ Error de inscripción: " + e.getMessage());
        }
        return "redirect:/partido/" + matchId;
    }

    @GetMapping("/{id}/pdf")
    public void generarPDFPartido(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        PadelMatch partido = null;
        try {
            partido = padelMatchService.buscarPorId(id).orElseThrow();
        } catch (NoSuchElementException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Partido no encontrado");
            return;
        }

        // Configuración de la respuesta HTTP
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=partido_" + partido.getNombrePartido().replaceAll("\\s+", "_") + ".pdf");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Paso 1: Usamos PdfWriter y PdfDocument
            PdfWriter pdfWriter = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);

            // Paso 2: Creamos el documento de layout
            Document document = new Document(pdfDocument, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            // Paso 3: Creamos los objetos de fuente
            PdfFont helvetica = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont helveticaBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Título
            Paragraph titulo = new Paragraph("DETALLES DEL PARTIDO")
                    .setFont(helveticaBold)
                    .setFontSize(24)
                    .setFontColor(ColorConstants.BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30f);
            document.add(titulo);

            // Información del Partido
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String fechaStr = partido.getFecha().format(dateFormatter);

            // ⭐️ CORRECCIÓN APLICADA: Convierte LocalTime a String
            String horaStr = partido.getHora().toString();

            // Tabla de Detalles
            float[] detailColumns = {2.5f, 4f};
            Table detailTable = new Table(UnitValue.createPercentArray(detailColumns));
            detailTable.setWidth(UnitValue.createPercentValue(60)); // Ancho reducido para detalles
            detailTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

            // Fila 1: Nombre
            detailTable.addCell(createDetailHeaderCell("Nombre:", helveticaBold));
            detailTable.addCell(createDetailValueCell(partido.getNombrePartido(), helvetica));

            // Fila 2: Fecha y Hora
            detailTable.addCell(createDetailHeaderCell("Fecha y Hora:", helveticaBold));
            detailTable.addCell(createDetailValueCell(fechaStr + " a las " + horaStr, helvetica));

            // Fila 3: Club
            detailTable.addCell(createDetailHeaderCell("Club:", helveticaBold));
            detailTable.addCell(createDetailValueCell(partido.getClub(), helvetica));

            // Fila 4: Nivel
            detailTable.addCell(createDetailHeaderCell("Nivel:", helveticaBold));
            detailTable.addCell(createDetailValueCell(partido.getNivelJuego(), helvetica));

            // Fila 5: Creador
            String creadorNombre = (partido.getCreador() != null) ? partido.getCreador().getNombreUsuario() : "N/A";
            detailTable.addCell(createDetailHeaderCell("Creador:", helveticaBold));
            detailTable.addCell(createDetailValueCell(creadorNombre, helvetica));

            document.add(detailTable);

            // Jugadores Inscritos
            List<Usuario> jugadores = partido.getJugadores();

            Paragraph subtitulo = new Paragraph("JUGADORES INSCRITOS (" + jugadores.size() + "/" + partido.getNumeroJugadores() + ")")
                    .setFont(helveticaBold)
                    .setFontSize(16)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginTop(30f)
                    .setMarginBottom(15f)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(subtitulo);

            // Tabla de jugadores
            float[] columnWidths = {1.5f, 2f, 2f, 1f, 2.5f};
            Table tablaJugadores = new Table(UnitValue.createPercentArray(columnWidths));
            tablaJugadores.setWidth(UnitValue.createPercentValue(100));

            // Encabezados
            String[] headers = {"Usuario", "Nombre", "Apellido", "Edad", "Teléfono"};
            for (String header : headers) {
                Cell headerCell = new Cell().add(new Paragraph(header).setFont(helveticaBold)
                                .setFontColor(ColorConstants.WHITE)
                                .setTextAlignment(TextAlignment.CENTER))
                        .setBackgroundColor(ColorConstants.GRAY)
                        .setPadding(8)
                        .setBorder(new SolidBorder(ColorConstants.GRAY, 1));
                tablaJugadores.addHeaderCell(headerCell);
            }

            // Datos de jugadores
            for (Usuario jugador : jugadores) {
                Individuo individuo = jugador.getIndividuo();

                tablaJugadores.addCell(new Cell().add(new Paragraph(jugador.getNombreUsuario()).setFont(helvetica)));

                if (individuo != null) {
                    tablaJugadores.addCell(new Cell().add(new Paragraph(individuo.getNombre()).setFont(helvetica)));
                    tablaJugadores.addCell(new Cell().add(new Paragraph(individuo.getApellido()).setFont(helvetica)));
                    tablaJugadores.addCell(new Cell().add(new Paragraph(String.valueOf(individuo.getEdad())).setFont(helvetica).setTextAlignment(TextAlignment.CENTER)));
                    tablaJugadores.addCell(new Cell().add(new Paragraph(individuo.getTelefono()).setFont(helvetica)));
                } else {
                    // Caso donde el usuario no tiene Individuo (Debería ser raro, pero es un fallback)
                    tablaJugadores.addCell(new Cell().add(new Paragraph("N/A").setFont(helvetica)));
                    tablaJugadores.addCell(new Cell().add(new Paragraph("N/A").setFont(helvetica)));
                    tablaJugadores.addCell(new Cell().add(new Paragraph("N/A").setFont(helvetica).setTextAlignment(TextAlignment.CENTER)));
                    tablaJugadores.addCell(new Cell().add(new Paragraph("N/A").setFont(helvetica)));
                }
            }
            document.add(tablaJugadores);

            document.add(new Paragraph("Documento generado el: " + java.time.LocalDate.now().format(dateFormatter))
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE))
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(30f));

            document.close();

            response.getOutputStream().write(baos.toByteArray());
        } catch (Exception e) {
            throw new IOException("Error generando PDF del Partido: " + e.getMessage(), e);
        }
    }

    private Cell createDetailHeaderCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(11))
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setBackgroundColor(ColorConstants.WHITE)
                .setPadding(5);
    }

    private Cell createDetailValueCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(11))
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setBackgroundColor(ColorConstants.WHITE)
                .setPadding(5);
    }
}