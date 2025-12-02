package com.example.web;

import com.example.domain.usuario.Usuario;
import com.example.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EliminarCuentas {

    @Autowired
    private UsuarioServicio usuarioService;

    @PostMapping("/eliminarCuenta")
    public String eliminarCuenta(HttpSession session, RedirectAttributes redirectAttrs) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioActual");
        if (usuario != null) {
            usuarioService.eliminarCuentaPorId(usuario.getId_usuario().longValue());
            session.invalidate(); // Cierra la sesión del usuario
            redirectAttrs.addFlashAttribute("mensaje", "Tu cuenta ha sido marcada como eliminada exitosamente.");
        }
        return "redirect:/";
    }
}
