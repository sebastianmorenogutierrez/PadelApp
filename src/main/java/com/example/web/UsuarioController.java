package com.example.web;

import com.example.domain.Usuario;
import com.example.servicio.UsuarioServicio;
import com.example.servicio.IndividuoServicio;
import com.example.servicio.PerfilServicio;
import com.example.domain.Individuo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
public class UsuarioController {

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private IndividuoServicio individuoServicio;

    @Autowired
    private PerfilServicio perfilServicio;

    @GetMapping("/jugadores-registrados")
    public String mostrarJugadoresRegistrados(Model model) {
        model.addAttribute("jugadores", usuarioServicio.listarTodos());
        return "jugadores"; // Este es el nombre del archivo Thymeleaf: jugadores.html
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        Usuario usuario = new Usuario();
        usuario.setIndividuo(new Individuo());

        model.addAttribute("usuario", usuario);
        model.addAttribute("perfiles", perfilServicio.listarTodos());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@Valid Usuario usuario, Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("perfiles", perfilServicio.listarTodos());
            model.addAttribute("individuos", individuoServicio.listaIndividuos());
            return "registro";
        }

        usuarioServicio.registrarNuevoUsuario(usuario);
        return "redirect:/login";
    }
}
