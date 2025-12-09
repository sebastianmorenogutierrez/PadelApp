package com.example.web;

import com.example.domain.torneo.Torneo;
import com.example.servicio.TorneoServicio;
import com.example.servicio.UsuarioServicio;
import com.example.domain.usuario.Usuario;
import com.example.domain.Individuo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("torneos", torneoService.listarTodos());
        return "lista_torneo";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("torneo", new Torneo());
        return "formulario_torneo";
    }
    @GetMapping("/editar/{id}")
    public String editarTorneo(@PathVariable Long id, Model model) {
        Torneo torneo = torneoService.buscarPorId(id).orElseThrow(
                () -> new NoSuchElementException("Torneo no encontrado con ID: " + id)
        );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String fechaInicioFormateada = torneo.getFechaInicio().format(formatter);
        String fechaFinFormateada = torneo.getFechaFin().format(formatter);
        model.addAttribute("torneo", torneo);
        model.addAttribute("fechaInicioFormateada", fechaInicioFormateada);
        model.addAttribute("fechaFinFormateada", fechaFinFormateada);
        return "torneo_detalle";
    }
    @PostMapping("/{id}/eliminar")
    public String eliminarTorneo(@PathVariable Long id) {
        torneoService.eliminar(id);
        return "redirect:/torneo";
    }
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Torneo torneo, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "formulario_torneo";
        }
        torneoService.guardar(torneo);
        return "redirect:/torneo";
    }

    @GetMapping("/{id}")
    public String verTorneo(@PathVariable Long id, Model model) {
        Torneo torneo = torneoService.buscarPorId(id).orElseThrow();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        // Formateo de fechas
        String fechaInicioFormateada = torneo.getFechaInicio().format(formatter);
        String fechaFinFormateada = torneo.getFechaFin().format(formatter);

        model.addAttribute("torneo", torneo);
        // Adición de fechas formateadas
        model.addAttribute("fechaInicioFormateada", fechaInicioFormateada);
        model.addAttribute("fechaFinFormateada", fechaFinFormateada);
        return "torneo_detalle"; // <-- Aquí también usas la vista
    }

    @PostMapping("/{id}/inscribir")
    public String inscribir(@PathVariable Long id, @RequestParam String nombre) {
        torneoService.inscribirPersona(id, nombre);
        return "redirect:/torneo/" + id;
    }

    @GetMapping("/{id}/pdf")
    public void generarPDFTorneo(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        Torneo torneo = null;
        try {
            torneo = torneoService.buscarPorId(id).orElseThrow();
        } catch (NoSuchElementException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Torneo no encontrado");
            return;
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=torneo_" + torneo.getNombre().replaceAll("\\s+", "_") + ".pdf");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter pdfWriter = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);

            Document document = new Document(pdfDocument, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            PdfFont helvetica = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont helveticaBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont helveticaBoldItalic = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLDOBLIQUE);

            // Título
            Paragraph titulo = new Paragraph("INFORMACIÓN DEL TORNEO")
                    .setFont(helveticaBold)
                    .setFontSize(24)
                    .setFontColor(ColorConstants.BLUE) // ✅ Usamos ColorConstants
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30f);
            document.add(titulo);

            // Información del torneo
            Paragraph pNombre = new Paragraph().add(new Paragraph("Nombre del Torneo:").setFont(helveticaBold).setBold())
                    .add(new Paragraph(torneo.getNombre()).setFont(helvetica));
            document.add(pNombre);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            String fechaInicioStr = dateFormat.format(Date.from(torneo.getFechaInicio().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            String fechaFinStr = dateFormat.format(Date.from(torneo.getFechaFin().atStartOfDay(ZoneId.systemDefault()).toInstant()));

            Paragraph pFechas = new Paragraph().add(new Paragraph("Fecha de Inicio:").setFont(helveticaBold))
                    .add(new Paragraph(fechaInicioStr).setFont(helvetica))
                    .add(new Paragraph("Fecha de Finalización:").setFont(helveticaBold))
                    .add(new Paragraph(fechaFinStr).setFont(helvetica));
            document.add(pFechas);

            if (torneo.getUbicacion() != null && !torneo.getUbicacion().isEmpty()) {
                Paragraph pUbicacion = new Paragraph().add(new Paragraph("Ubicación:").setFont(helveticaBold))
                        .add(new Paragraph(torneo.getUbicacion()).setFont(helvetica));
                document.add(pUbicacion);
            }

            // Lista de jugadores registrados
            List<Usuario> jugadores = usuarioServicio.listarTodos();
            jugadores.removeIf(u -> u.getIndividuo() == null || u.getIndividuo().isEliminado());

            Paragraph subtitulo = new Paragraph("JUGADORES REGISTRADOS EN EL SISTEMA")
                    .setFont(helveticaBold)
                    .setFontSize(16)
                    .setFontColor(ColorConstants.DARK_GRAY) // ✅ Usamos ColorConstants
                    .setMarginTop(20f)
                    .setMarginBottom(15f);
            document.add(subtitulo);

            // Tabla de jugadores
            float[] columnWidths = {2f, 2f, 1f, 2.5f, 1.5f};
            Table tabla = new Table(UnitValue.createPercentArray(columnWidths));
            tabla.setWidth(UnitValue.createPercentValue(100));

            // Encabezados
            String[] headers = {"Nombre", "Apellido", "Edad", "Correo", "Teléfono"};
            for (String header : headers) {
                Cell headerCell = new Cell().add(new Paragraph(header).setFont(helveticaBold)
                                .setFontColor(ColorConstants.WHITE) // ✅ Usamos ColorConstants
                                .setTextAlignment(TextAlignment.CENTER))
                        .setBackgroundColor(ColorConstants.GRAY) // ✅ Usamos ColorConstants
                        .setPadding(8)
                        .setBorder(new SolidBorder(ColorConstants.GRAY, 1)); // ✅ Usamos ColorConstants
                tabla.addHeaderCell(headerCell);
            }

            // Datos de jugadores
            for (Usuario jugador : jugadores) {
                Individuo individuo = jugador.getIndividuo();
                if (individuo != null) {
                    tabla.addCell(new Cell().add(new Paragraph(individuo.getNombre()).setFont(helvetica)));
                    tabla.addCell(new Cell().add(new Paragraph(individuo.getApellido()).setFont(helvetica)));
                    tabla.addCell(new Cell().add(new Paragraph(String.valueOf(individuo.getEdad())).setFont(helvetica)));
                    tabla.addCell(new Cell().add(new Paragraph(individuo.getCorreo()).setFont(helvetica)));
                    tabla.addCell(new Cell().add(new Paragraph(individuo.getTelefono()).setFont(helvetica)));
                }
            }
            document.add(tabla);

            // Footer
            String footerText = "Documento generado el: " + dateFormat.format(new Date());
            Paragraph footer = new Paragraph(footerText)
                    .setFont(helveticaBoldItalic)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY) // ✅ Usamos ColorConstants
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30f);
            document.add(footer);

            document.close();

            response.getOutputStream().write(baos.toByteArray());
        } catch (Exception e) {
            throw new IOException("Error generando PDF", e);
        }
    }
}