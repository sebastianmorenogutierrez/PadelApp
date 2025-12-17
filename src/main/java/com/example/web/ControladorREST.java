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
    // Inyección de servicios (Autowired es correcto en Spring)
    @Autowired
    private IndividuoServicio individuoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private CorreoServicio correoServicio;

    @Autowired
    private PerfilServicio perfilServicio;


    // 🔑 MÉTODOS DE AUTENTICACIÓN Y REGISTRO
    // ---------------------------------------------------

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @GetMapping("/login?rolDesconocido")
    public String mostrarAccesodenegado() {
        return "login";
    }

    // Muestra el formulario de registro
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
            // Si hay errores, volvemos a /registro (GET) para mostrar los mensajes de error
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.usuario", errors);
            redirectAttributes.addFlashAttribute("usuario", usuario); // Mantener datos
            return "redirect:/registro";
        }

        try {
            // Asigna el perfil JUGADOR por defecto (ID 2, asumiendo esta convención)
            usuario.setPerfil(perfilServicio.buscarPorId(2));

            usuario.getIndividuo().setEliminado(false);

            // 🟢 El servicio ahora verifica unicidad y guarda el Individuo primero.
            usuarioServicio.registrarNuevoUsuario(usuario);

            redirectAttributes.addFlashAttribute("mensajeExito", "¡Registro exitoso! Ya puedes iniciar sesión.");
            return "redirect:/login";

            // 🟢 CORRECCIÓN: Manejar la excepción específica de unicidad/lógica del negocio
        } catch (IllegalStateException e) {
            // Captura errores lanzados por el servicio (ej: nombre de usuario ya existe)
            System.err.println("Error de Lógica/Unicidad en el registro: " + e.getMessage());
            e.printStackTrace();

            // Pasa el mensaje de error al formulario de registro
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/registro";

        } catch (Exception e) {
            System.err.println("Error grave al guardar el nuevo usuario: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("mensajeError", "Hubo un error interno al crear la cuenta. Intenta de nuevo.");
            return "redirect:/registro";
        }
    }

    @GetMapping("/redirigir")
    public String redirigirSegunPerfil(Authentication auth, HttpSession session) {

        String username = auth.getName();
        Usuario usuario = usuarioServicio.localizarPorNombreUsuario(username);
        // Guardar el usuario completo en sesión es opcional, pero ayuda a acceder a datos sin consultas repetidas
        session.setAttribute("usuarioActual", usuario);
        String rol = auth.getAuthorities().iterator().next().getAuthority();

        switch (rol) {
            case "ROLE_ADMINISTRADOR":
                return "redirect:/indice"; // Dashboard Admin
            case "ROLE_JUGADOR":
                return "redirect:/indicejugador"; // Dashboard Jugador
            default:
                return "redirect:/error"; // Mejor redirigir a una página de error genérica o login
        }
    }

    @GetMapping("/indicejugador")
    public String mostrarDashboardJugador(Model model, Authentication authentication) {
        // Podrías cargar datos específicos del jugador aquí si es necesario.
        return "indicejugador";
    }

    @GetMapping("/indice")
    public String mostrarIndice(Model model, Authentication authentication) {
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

    // 🔄 MÉTODOS DE PERFIL DE USUARIO
    // ---------------------------------------------------

    @GetMapping("/datos")
    public String mostrarDatos(Model model, Authentication authentication) {
        String nombreUsuario = authentication.getName();
        Usuario usuario = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

        if (usuario != null && usuario.getIndividuo() != null && !usuario.isEliminado() && !usuario.getIndividuo().isEliminado()) {
            model.addAttribute("usuario", usuario);
            // El mensaje de éxito de RedirectAttributes (si existe) se añade automáticamente al Model aquí.
            return "datos";
        } else {
            return "redirect:/login?error";
        }
    }

    @GetMapping("/modificar")
    public String mostrarFormularioDeEdicion(Model model, Authentication auth) {
        String nombreUsuario = auth.getName();
        Usuario usuario = usuarioServicio.obtenerUsuarioActual(nombreUsuario);

        if (usuario != null && usuario.getIndividuo() != null) {
            // Asegura que el modelo esté limpio o que se use el objeto existente para la edición
            if (!model.containsAttribute("usuario")) {
                model.addAttribute("usuario", usuario);
            }
            return "formulariomodificar";
        } else {
            return "redirect:/login?error";
        }
    }

    /**
     * Procesa la modificación del perfil de usuario.
     * Implementa el patrón Post/Redirect/Get (PRG).
     * @param usuario Objeto Usuario con los datos del Individuo actualizados.
     * @param errors Errores de validación de Jakarta Validation.
     * @param redirectAttributes Para pasar mensajes flash al GET de /datos.
     * @return Redirección al perfil si tiene éxito, o al formulario si hay errores.
     */
    @PostMapping("/modificar")
    public String procesarModificacion(@Valid @ModelAttribute("usuario") Usuario usuario, Errors errors, RedirectAttributes redirectAttributes, Authentication auth) {
        if (errors.hasErrors()) {
            System.err.println("Errores de validación al modificar el perfil: " + errors.getAllErrors());
            // Si hay errores, retornamos al formulario (Thymeleaf maneja los mensajes)
            return "formulariomodificar";
        }

        try {
            // 1. Obtener y actualizar el Individuo
            Individuo individuo = usuario.getIndividuo();
            individuo.setEliminado(false); // Asegura que no se marque como eliminado
            individuoServicio.salvar(individuo); // Actualiza los datos en la DB

            // 2. Mensaje de éxito y Redirección
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Tu perfil ha sido actualizado con éxito!");
            return "redirect:/datos"; // <-- Redirige para recargar datos actualizados

        } catch (Exception e) {
            System.err.println("Error al actualizar el individuo: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "Error al guardar los cambios: " + e.getMessage());
            return "redirect:/modificar";
        }
    }

    @GetMapping("/eliminarCuenta")
    public String eliminarCuenta(Authentication auth, HttpSession session) {
        // Realiza la eliminación lógica del Individuo y del Usuario, e invalida la sesión.
        String nombreUsuario = auth.getName();
        Usuario usuario = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

        if (usuario != null) {
            // Eliminación lógica del Individuo (Datos personales)
            Individuo individuo = usuario.getIndividuo();
            if (individuo != null) {
                individuo.setEliminado(true);
                individuoServicio.salvar(individuo);
            }
            // Eliminación del Usuario (Cuenta)
            // Es más seguro hacer eliminación lógica del Usuario también, o usar un servicio transaccional.
            usuarioServicio.eliminarCuentaPorId(Long.valueOf(usuario.getId_usuario()));
            session.invalidate(); // Desloguea al usuario.
        }
        return "redirect:/login?cuentaEliminada";
    }

    // 🌐 MÉTODOS DE GESTIÓN ADMINISTRATIVA
    // ---------------------------------------------------

    @GetMapping("/")
    public String comienzo(Model model) {
        // Podrías redirigir a /indice o /indicejugador si hay una sesión activa,
        // o mostrar una página de inicio pública.
        List<Individuo> individuos = individuoServicio.listaIndividuos();
        model.addAttribute("individuos", individuos);
        return "principal";
    }

    @GetMapping("/jugadores")
    public String verJugadores(Model model) {
        try {
            // Filtra solo usuarios activos (no eliminados) y con individuo activo
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

    // (Mantengo /jugadores-registrados por si se usa en otra parte, aunque /jugadores es mejor)
    @GetMapping("/jugadores-registrados")
    public String mostrarJugadoresRegistrados(Model model) {
        model.addAttribute("jugadores", usuarioServicio.listarTodos());
        return "jugadores";
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

    @GetMapping("/jugadores/editar/{idIndividuo}")
    public String editarJugador(@PathVariable("idIndividuo") Long idIndividuo, Model model) {
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
        if (errors.hasErrors()) {
            return "cambiar";
        }
        individuoServicio.salvar(individuo);
        return "redirect:/jugadores";
    }

    @GetMapping("/borrar/{idIndividuo}")
    public String eliminarJugador(@PathVariable("idIndividuo") Long idIndividuo, RedirectAttributes redirectAttributes) {
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

    // 📧 MÉTODOS DE CORREO Y OTROS
    // ---------------------------------------------------

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

    @GetMapping("/exportarExcel")
    public void exportarExcel(HttpServletResponse response) throws IOException {
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

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}