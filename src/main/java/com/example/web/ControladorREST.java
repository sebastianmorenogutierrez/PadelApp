    package com.example.web;

    import com.example.domain.Individuo;
    import com.example.servicio.UsuarioServicio;
    import com.example.domain.Usuario;
    import com.example.servicio.IndividuoServicio;
    import jakarta.servlet.http.HttpServletResponse;
    import jakarta.servlet.http.HttpSession;
    import jakarta.validation.Valid;
    import org.apache.poi.ss.usermodel.Row;
    import org.apache.poi.ss.usermodel.Sheet;
    import org.apache.poi.ss.usermodel.Workbook;
    import org.apache.poi.xssf.usermodel.XSSFWorkbook;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.CommandLineRunner;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Component;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.validation.Errors;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PostMapping;



    import java.io.IOException;
    import java.util.List;

    @Controller
    public class ControladorREST {

        @Autowired
        private IndividuoServicio individuoServicio;

        @Autowired
        private UsuarioServicio usuarioServicio;

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
                return "cambiar";
            }
            individuoServicio.salvar(individuo);
            return "redirect:/";
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

        @GetMapping("/cambiar/{id_individuo}")
        public String cambiar(Individuo individuo, Model model) {
            individuo = individuoServicio.localizarIndividuo(individuo);
            model.addAttribute("individuo", individuo);
            return "cambiar";
        }

        @GetMapping("/borrar/{id_individuo}")
        public String borrar(Individuo individuo) {
            Individuo actual = individuoServicio.localizarIndividuo(individuo);
            if (actual != null) {
                actual.setEliminado(true);
                individuoServicio.salvar(actual);
                System.out.println("Individuo marcado como eliminado: " + actual.getNombre());
            }
            return "redirect:/";
        }

        @GetMapping("/indice")
        public String mostrarIndice() {
            return "indice";
        }


        @GetMapping("/torneo")
        public String mostrarTorneos() {
            return "torneo";
        }

        @GetMapping("/equipo")
        public String mostrarEquipo() {
            return "equipo";
        }

        @GetMapping("/redirigir")
        public String redirigirSegunPerfil(Authentication auth, HttpSession session) {
            String username = auth.getName();
            Usuario usuario = usuarioServicio.localizarPorNombreUsuario(username);
            session.setAttribute("usuarioActual", usuario);
            session.setAttribute("individuoActual", usuario.getIndividuo());

            String rol = auth.getAuthorities().iterator().next().getAuthority();

            if ("ROLE_ADMINISTRACION".equals(rol)) {
                return "redirect:/indice";
            } else if ("ROLE_JUGADOR".equals(rol)) {
                return "redirect:/secretaria";
            } else {
                return "redirect:/acceso-denegado";
            }
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

            model.addAttribute("mensajeExito", "✅ Cambios guardados correctamente.");
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

        @GetMapping("/secretaria")
        public String mostrarsecretaria() {
            return "secretaria";
        }


        @GetMapping("/partido")
        public String mostrarpartido() {
            return "partido";
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

