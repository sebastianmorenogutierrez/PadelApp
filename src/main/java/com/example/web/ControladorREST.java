package com.example.web;

import com.example.domain.Individuo;
import com.example.servicio.UsuarioServicio;
import com.example.domain.usuario.Usuario;
import com.example.servicio.IndividuoServicio;
import com.example.servicio.CorreoServicio;
import com.example.servicio.PerfilServicio;
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

    @Autowired
    private PerfilServicio perfilServicio;


    // 🔑 MÉTODOS DE AUTENTICACIÓN Y REGISTRO

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @GetMapping("/login?rolDesconocido")
    public String mostrarAccesodenegado() {
        return "login";
    }

    // 1. Muestra el formulario de registro (Movido de UsuarioController)
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        Usuario usuario = new Usuario();
        usuario.setIndividuo(new Individuo());
        model.addAttribute("usuario", usuario);
        // Necesario para que el formulario pueda elegir el perfil si lo deseas
        model.addAttribute("perfiles", perfilServicio.listarTodos());
        return "registro";
    }

    @PostMapping("/API/registro")
    public String procesarRegistro(@Valid Usuario usuario, Errors errors, RedirectAttributes redirectAttributes) {

        System.out.println(">>> Intentando procesar registro. ¿Hay errores de validación?: " + errors.hasErrors());

        if (errors.hasErrors()) {
            System.out.println("Errores de validación en el registro: " + errors.getAllErrors());
            // ... (Redirección de error de validación) ...
            return "redirect:/registro";
        }

        try {
            usuario.setPerfil(perfilServicio.buscarPorId(2));

            usuario.getIndividuo().setEliminado(false);

            usuarioServicio.registrarNuevoUsuario(usuario);

            redirectAttributes.addFlashAttribute("mensajeExito", "¡Registro exitoso! Ya puedes iniciar sesión.");
            return "redirect:/login";

        } catch (Exception e) {
            System.err.println("Error al guardar el nuevo usuario: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("mensajeError", "Hubo un error al crear la cuenta: " + e.getMessage());
            return "redirect:/registro";
        }
    }

    @GetMapping("/redirigir")
    public String redirigirSegunPerfil(Authentication auth, HttpSession session) {

        String username = auth.getName();
        Usuario usuario = usuarioServicio.localizarPorNombreUsuario(username);
        session.setAttribute("usuarioActual", usuario);
        String rol = auth.getAuthorities().iterator().next().getAuthority();
        switch (rol) {
            case "ROLE_ADMINISTRADOR":
                return "redirect:/indice"; // Dashboard Admin
            case "ROLE_JUGADOR":
                return "redirect:/indicejugador"; // Dashboard Jugador
            default:
                return "redirect:/error";
        }
    }

    @GetMapping("/indicejugador") // ¡La ruta que faltaba!
    public String mostrarDashboardJugador(Model model, Authentication authentication) {
        return "indicejugador";
    }

    @GetMapping("/indice")
    public String mostrarIndice(Model model, Authentication authentication) {
        // Muestra el dashboard del administrador, cargando el nombre completo del usuario.
        String nombreUsuario = authentication.getName();
        Usuario usuarioAutenticado = usuarioServicio.obtenerUsuarioActual(nombreUsuario);

        if (usuarioAutenticado != null && usuarioAutenticado.getIndividuo() != null) {
            String nombre = usuarioAutenticado.getIndividuo().getNombre();
            String apellido = usuarioAutenticado.getIndividuo().getApellido();
            String nombreCompleto = (nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "");

            model.addAttribute("nombreCompleto", nombreCompleto.trim());
            model.addAttribute("usuario", usuarioAutenticado);
            return "indice";
        } else {
            return "redirect:/login?error=usuarioInvalido";
        }
    }

    @GetMapping("/datos")
    public String mostrarDatos(Model model, Authentication authentication) {
        // Muestra la vista con la información detallada del perfil del usuario logueado.
        String nombreUsuario = authentication.getName();
        Usuario usuario = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);
        if (usuario != null && usuario.getIndividuo() != null && !usuario.isEliminado()) {
            model.addAttribute("usuario", usuario);
            return "datos";
        } else {
            return "redirect:/login?error";
        }
    }

    @GetMapping("/modificar")
    public String mostrarFormularioDeEdicion(Model model, Authentication auth) {
        // Carga el formulario para que el usuario logueado modifique su propio perfil.
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
        // Procesa el guardado de los cambios realizados en el perfil propio.
        if (errors.hasErrors()) {
            return "formulariomodificar";
        }
        Individuo individuo = usuario.getIndividuo();
        individuo.setEliminado(false); // Asegura que el individuo siga activo.
        individuoServicio.salvar(individuo);
        model.addAttribute("mensajeExito", "Cambios guardados correctamente.");
        return "indice";
    }

    @GetMapping("/eliminarCuenta")
    public String eliminarCuenta(Authentication auth, HttpSession session) {
        // Realiza la eliminación lógica del Individuo y del Usuario, e invalida la sesión.
        String nombreUsuario = auth.getName();
        Usuario usuario = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);
        if (usuario != null) {
            Individuo individuo = usuario.getIndividuo();
            if (individuo != null) {
                individuo.setEliminado(true); // Eliminación lógica del Individuo.
                individuoServicio.salvar(individuo);
            }
            usuarioServicio.eliminarCuentaPorId(Long.valueOf(usuario.getId_usuario())); // Eliminación del Usuario.
            session.invalidate(); // Desloguea al usuario.
        }
        return "redirect:/login?cuentaEliminada";
    }

    // --------------------------------------------------

    // 👥 MÉTODOS DE ADMINISTRACIÓN DE JUGADORES (CRUD)

    @GetMapping("/")
    public String comienzo(Model model) {
        // Muestra la vista principal (inicio), listando todos los individuos.
        List<Individuo> individuos = individuoServicio.listaIndividuos();
        model.addAttribute("individuos", individuos);
        return "principal";
    }

    @GetMapping("/jugadores")
    public String verJugadores(Model model) {
        // Muestra una lista de todos los usuarios y sus individuos, filtrando solo los activos.
        try {
            List<Usuario> jugadores = usuarioServicio.listarTodos()
                    .stream()
                    .filter(usuario -> !usuario.isEliminado() && usuario.getIndividuo() != null && !usuario.getIndividuo().isEliminado())
                    .collect(Collectors.toList());
            model.addAttribute("jugadores", jugadores);
        } catch (Exception e) {
            System.err.println("Error al cargar jugadores: " + e.getMessage());
            model.addAttribute("jugadores", List.of());
        }
        return "jugadores";
    }

    // 💡 Nuevo método para listar todos los jugadores (desde UsuarioController)
    @GetMapping("/jugadores-registrados")
    public String mostrarJugadoresRegistrados(Model model) {
        model.addAttribute("jugadores", usuarioServicio.listarTodos());
        return "jugadores";
    }

    @GetMapping("/anexar")
    public String anexar(Individuo individuo) {
        // Muestra el formulario para agregar un nuevo Individuo.
        return "agregar";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid Individuo individuo, Errors errors) {
        // Procesa el formulario para guardar un nuevo Individuo (Administrativo).
        if (errors.hasErrors()) {
            return "agregar";
        }
        individuoServicio.salvar(individuo);
        return "redirect:/jugadores";
    }

    @GetMapping("/cambiar/{idIndividuo}")
    public String editarJugador(@PathVariable("idIndividuo") Long idIndividuo, Model model) {
        // Muestra el formulario para editar un Individuo por su ID.
        Individuo individuo = individuoServicio.localizarIndividuo(idIndividuo);
        if (individuo != null) {
            model.addAttribute("individuo", individuo);
            return "cambiar";
        } else {
            return "redirect:/jugadores";
        }
    }

    @PostMapping("/cambiar/guardar")
    public String guardarCambios(@Valid @ModelAttribute("individuo") Individuo individuo, Errors errors, Model model) {
        // Guarda los cambios del Individuo editado.
        if (errors.hasErrors()) {
            return "cambiar";
        }
        individuoServicio.salvar(individuo);
        return "redirect:/jugadores";
    }

    @GetMapping("/borrar/{idIndividuo}")
    public String eliminarJugador(@PathVariable("idIndividuo") Long idIndividuo, RedirectAttributes redirectAttributes) {
        // Realiza la eliminación lógica de un Individuo (Administrativo).
        Individuo individuo = individuoServicio.localizarIndividuo(idIndividuo);
        if (individuo != null) {
            individuo.setEliminado(true); // Marca como eliminado.
            individuoServicio.salvar(individuo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Jugador eliminado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("mensajeError", "Error: Jugador no encontrado.");
        }
        return "redirect:/jugadores";
    }

    // --------------------------------------------------

    // 📧 MÉTODOS DE COMUNICACIÓN Y VISTAS ESTÁTICAS

    @PostMapping("/enviar-correo-masivo")
    public String enviarCorreoMasivo(
            @RequestParam("asunto") String asunto,
            @RequestParam("mensaje") String mensaje,
            @RequestParam(value = "tipoEvento", defaultValue = "NOTIFICACIÓN GENERAL") String tipoEvento,
            RedirectAttributes redirectAttributes) {

        // Filtra y obtiene la lista de jugadores activos.
        List<Usuario> jugadoresActivos = usuarioServicio.listarTodos()
                .stream()
                .filter(usuario -> !usuario.isEliminado() && usuario.getIndividuo() != null && !usuario.getIndividuo().isEliminado())
                .collect(Collectors.toList());

        if (jugadoresActivos.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeAdvertencia", "No hay jugadores activos para enviar correos.");
            return "redirect:/jugadores";
        }

        try {
            // Inicia el envío asíncrono para no bloquear la aplicación.
            CompletableFuture<Void> futuroEnvio = correoServicio.enviarCorreoMasivo(jugadoresActivos, asunto, mensaje, tipoEvento);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "El envío masivo de correos a " + jugadoresActivos.size() + " jugadores ha sido iniciado en segundo plano.");

        } catch (Exception e) {
            System.err.println("Error al iniciar el proceso de envío masivo: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "Error interno al iniciar el envío de correos: " + e.getMessage());
        }

        return "redirect:/jugadores";
    }

    @GetMapping("/equipo")
    public String mostrarEquipo() {
        return "equipo";
    }

    @GetMapping("/torneos-vista")
    public String mostrarTorneo() {
        return "torneo";
    }

    // --------------------------------------------------

    // 📊 EXPORTACIÓN DE DATOS

    @GetMapping("/exportarExcel")
    public void exportarExcel(HttpServletResponse response) throws IOException {
        // Genera y descarga un archivo Excel (XLSX) con los datos de todos los Individuos.
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=individuos.xlsx");

        List<Individuo> list = individuoServicio.listaIndividuos();
        Workbook workbook = new XSSFWorkbook();
        Sheet hoja = workbook.createSheet("individuos");

        // Crear encabezados
        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("Nombre");
        header.createCell(1).setCellValue("Apellido");
        header.createCell(2).setCellValue("Edad");
        header.createCell(3).setCellValue("Correo");
        header.createCell(4).setCellValue("Telefono");

        // Llenar datos
        int fila = 1;
        for (Individuo ind : list) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(ind.getNombre());
            row.createCell(1).setCellValue(ind.getApellido());
            row.createCell(2).setCellValue(ind.getEdad());
            row.createCell(3).setCellValue(ind.getCorreo());
            row.createCell(4).setCellValue(ind.getTelefono());
        }

        // Escribir el archivo en la respuesta HTTP
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}