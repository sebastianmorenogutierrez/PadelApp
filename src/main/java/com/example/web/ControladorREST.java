package com.example.web;

import com.example.domain.Individuo;
import com.example.servicio.UsuarioServicio;
import com.example.domain.usuario.Usuario;
import com.example.servicio.IndividuoServicio;
import com.example.servicio.CorreoServicio;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

@Controller
public class ControladorREST {

    @Autowired
    private IndividuoServicio individuoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private CorreoServicio correoServicio;

    @PostMapping("/enviar-correo-masivo")
    public String enviarCorreoMasivo(
            @RequestParam("asunto") String asunto,
            @RequestParam("mensaje") String mensaje,
            @RequestParam(value = "tipoEvento", defaultValue = "NOTIFICACIÓN GENERAL") String tipoEvento,
            RedirectAttributes redirectAttributes) {

        List<Usuario> jugadoresActivos = usuarioServicio.listarTodos()
                .stream()
                .filter(usuario -> !usuario.isEliminado() && usuario.getIndividuo() != null && !usuario.getIndividuo().isEliminado())
                .collect(Collectors.toList());

        if (jugadoresActivos.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeAdvertencia", "No hay jugadores activos para enviar correos.");
            return "redirect:/jugadores";
        }

        try {
            CompletableFuture<Void> futuroEnvio = correoServicio.enviarCorreoMasivo(jugadoresActivos, asunto, mensaje, tipoEvento);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "El envío masivo de correos a " + jugadoresActivos.size() +
                            " jugadores ha sido iniciado en segundo plano.");

        } catch (Exception e) {
            System.err.println("Error al iniciar el proceso de envío masivo: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "Error interno al iniciar el envío de correos: " + e.getMessage());
        }

        return "redirect:/jugadores";
    }




    @GetMapping("/")
    public String comienzo(Model model) {
        List<Individuo> individuos = individuoServicio.listaIndividuos();
        model.addAttribute("individuos", individuos);
        return "principal";
    }

    @GetMapping("/anexar")
    public String anexar(Individuo individuo) {
        return "agregar";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid Individuo individuo, Errors errors) {
        if (errors.hasErrors()) {
            return "agregar";
        }
        individuoServicio.salvar(individuo);
        return "redirect:/jugadores";
    }

    @PostMapping("/cambiar/guardar")
    public String guardarCambios(@Valid @ModelAttribute("individuo") Individuo individuo, Errors errors, Model model) {
        if (errors.hasErrors()) {
            return "cambiar";
        }
        individuoServicio.salvar(individuo);
        return "redirect:/jugadores";
    }

    @GetMapping("/cambiar/{idIndividuo}")
    public String editarJugador(@PathVariable("idIndividuo") Long idIndividuo, Model model) {
        Individuo individuo = individuoServicio.localizarIndividuo(idIndividuo);
        if (individuo != null) {
            model.addAttribute("individuo", individuo);
            return "cambiar";
        } else {
            return "redirect:/jugadores";
        }
    }

    @GetMapping("/borrar/{idIndividuo}")
    public String eliminarJugador(@PathVariable("idIndividuo") Long idIndividuo, RedirectAttributes redirectAttributes) {
        Individuo individuo = individuoServicio.localizarIndividuo(idIndividuo);
        if (individuo != null) {
            individuo.setEliminado(true);
            individuoServicio.salvar(individuo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Jugador eliminado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("mensajeError", "Error: Jugador no encontrado.");
        }
        return "redirect:/jugadores";
    }

    @GetMapping("/datos")
    public String mostrarDatos(Model model, Authentication authentication) {
        String nombreUsuario = authentication.getName();
        Usuario usuario = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);
        if (usuario != null && usuario.getIndividuo() != null && !usuario.isEliminado()) {
            model.addAttribute("usuario", usuario);
            return "datos";
        } else {
            return "redirect:/login?error";
        }
    }

    @GetMapping("/indice")
    public String mostrarIndice(Model model, Authentication authentication) {
        String nombreUsuario = authentication.getName();
        // Usamos el método que trae al usuario para verificar que no esté eliminado
        Usuario usuarioAutenticado = usuarioServicio.obtenerUsuarioActual(nombreUsuario);

        if (usuarioAutenticado != null && usuarioAutenticado.getIndividuo() != null) {

            // 1. Construir el Nombre Completo
            String nombre = usuarioAutenticado.getIndividuo().getNombre();
            String apellido = usuarioAutenticado.getIndividuo().getApellido();
            String nombreCompleto = (nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "");

            // 2. Agregar las variables al Modelo de Thymeleaf
            model.addAttribute("nombreCompleto", nombreCompleto.trim());
            model.addAttribute("usuario", usuarioAutenticado); // Necesario para el rol

            System.out.println("Cargando índice para: " + nombreCompleto.trim());

            return "indice";
        } else {
            // Redirige al login si el usuario no se encuentra o está incompleto
            return "redirect:/login?error=usuarioInvalido";
        }
    }

    @GetMapping("/equipo")
    public String mostrarEquipo() {
        return "equipo";
    }


    @GetMapping("/torneos-vista")
    public String mostrarTorneo() {
        return "torneo";
    }

    @GetMapping("/redirigir")
    public String redirigirSegunPerfil(Authentication auth, HttpSession session) {
        String username = auth.getName();
        Usuario usuario = usuarioServicio.localizarPorNombreUsuario(username);
        session.setAttribute("usuarioActual", usuario);
        String rol = auth.getAuthorities().iterator().next().getAuthority();
        System.out.println("ROL QUE TRAIGO PARA VALIDAR: " + rol);
        switch (rol) {
            case "ROLE_ADMINISTRADOR":
                return "redirect:/indice";
            case "ROLE_JUGADOR":
                return "redirect:/jugador22";
            default:
                return "redirect:/error";
        }
    }

    @GetMapping("/jugadores")
    public String verJugadores(Model model) {
        try {
            List<Usuario> jugadores = usuarioServicio.listarTodos()
                    .stream()
                    .filter(usuario -> !usuario.isEliminado() &&
                            usuario.getIndividuo() != null &&
                            !usuario.getIndividuo().isEliminado())
                    .collect(Collectors.toList());
            model.addAttribute("jugadores", jugadores);
        } catch (Exception e) {
            System.err.println("Error al cargar jugadores: " + e.getMessage());
            model.addAttribute("jugadores", List.of());
        }
        return "jugadores";
    }

    @GetMapping("/modificar")
    public String mostrarFormularioDeEdicion(Model model, Authentication auth) {
        String nombreUsuario = auth.getName();
        Usuario usuario = usuarioServicio.obtenerUsuarioActual(nombreUsuario);
        if (usuario != null && usuario.getIndividuo() != null) {
            model.addAttribute("usuario", usuario);
            return "formulariomodificar";
        } else {
            return "redirect:/login?error";
        }
    }

    @PostMapping("/modificar")
    public String procesarModificacion(@Valid Usuario usuario, Errors errors, Model model) {
        if (errors.hasErrors()) {
            return "formulariomodificar";
        }
        Individuo individuo = usuario.getIndividuo();
        individuo.setEliminado(false);
        individuoServicio.salvar(individuo);
        model.addAttribute("mensajeExito", "Cambios guardados correctamente.");
        return "indice";
    }

    @GetMapping("/eliminarCuenta")
    public String eliminarCuenta(Authentication auth, HttpSession session) {
        String nombreUsuario = auth.getName();
        Usuario usuario = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);
        if (usuario != null) {
            Individuo individuo = usuario.getIndividuo();
            if (individuo != null) {
                individuo.setEliminado(true);
                individuoServicio.salvar(individuo);
            }
            usuarioServicio.eliminarCuentaPorId(Long.valueOf(usuario.getId_usuario()));
            session.invalidate();
        }
        return "redirect:/login?cuentaEliminada";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @GetMapping("/jugador")
    public String mostrarjugador() {
        return "jugador";
    }

    @GetMapping("/jugador22")
    public String mostrarvistajugador() {
        return "jugador22";
    }

    @GetMapping("/login?rolDesconocido")
    public String mostrarAccesodenegado() {
        return "login";
    }

    @GetMapping("/exportarExcel")
    public void exportarExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=individuos.xlsx");

        List<Individuo> list = individuoServicio.listaIndividuos();
        Workbook workbook = new XSSFWorkbook();
        Sheet hoja = workbook.createSheet("individuos");

        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("Nombre");
        header.createCell(1).setCellValue("Apellido");
        header.createCell(2).setCellValue("Edad");
        header.createCell(3).setCellValue("Correo");
        header.createCell(4).setCellValue("Telefono");

        int fila = 1;
        for (Individuo ind : list) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(ind.getNombre());
            row.createCell(1).setCellValue(ind.getApellido());
            row.createCell(2).setCellValue(ind.getEdad());
            row.createCell(3).setCellValue(ind.getCorreo());
            row.createCell(4).setCellValue(ind.getTelefono());
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}