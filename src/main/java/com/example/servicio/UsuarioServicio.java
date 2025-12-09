package com.example.servicio;

import com.example.domain.usuario.Usuario;
import com.example.domain.Individuo;
import com.example.dao.UsuarioDao;
import com.example.dao.IndividuoDao;
import com.example.dao.PerfilDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

// Importaciones necesarias para obtener el usuario de Spring Security
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@Service
public class UsuarioServicio {

    @Autowired
    private UsuarioDao usuarioDao;

    @Autowired
    private IndividuoDao individuoDao;

    @Autowired
    private PerfilDao perfilDao;

    @Autowired
    private PasswordEncoder passwordEncoder;
    public Usuario obtenerUsuarioActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String nombreUsuario = ((UserDetails) principal).getUsername();
            Usuario usuario = usuarioDao.buscarPorNombre(nombreUsuario);

            if (usuario != null && !usuario.isEliminado()) {
                return usuario;
            }
        }
        return null; // Retorna null si no hay sesión o el usuario no se encuentra/está eliminado
    }
    public boolean esAdministrador(Usuario usuario) {
        if (usuario == null || usuario.getPerfil() == null) {
            return false;
        }
        String nombrePerfil = usuario.getPerfil().getDescripcion_perfil();
        return "ROLE_ADMINISTRADOR".equalsIgnoreCase(nombrePerfil);
    }
    @Transactional
    public void registrarNuevoUsuario(Usuario usuario) {
        guardarUsuario(usuario);
    }

    private void guardarUsuario(Usuario usuario) {
        String passEncriptada = passwordEncoder.encode(usuario.getPass_usuario());
        usuario.setPass_usuario(passEncriptada);
        usuario.setEliminado(false);
        usuarioDao.save(usuario);
    }

    @Transactional
    public void eliminarCuentaPorId(Long idUsuario) {
        if (idUsuario == null) {
            System.out.println("ID de usuario es nulo. No se puede modificar.");
            return;
        }

        Integer idInt = idUsuario.intValue();
        Usuario usuario = usuarioDao.findById(idInt).orElse(null);

        if (usuario != null) {
            System.out.println("Eliminando credenciales del usuario ID: " + usuario.getId_usuario());
            usuario.setNombreUsuario("ELIMINADO_" + idInt);
            usuario.setPass_usuario("SIN_PASS");
            usuario.setEliminado(true);
            usuarioDao.save(usuario);
        } else {
            System.out.println("No se encontró el usuario con ID: " + idInt);
        }
    }

    public Usuario encontrarPorId(Integer idUsuario) {
        return usuarioDao.findById(idUsuario).orElse(null);
    }

    public Usuario localizarPorNombreUsuario(String nombreUsuario) {
        return usuarioDao.buscarPorNombre(nombreUsuario);
    }
    public Usuario obtenerUsuarioActual(String nombreUsuario) {
        Usuario usuario = usuarioDao.buscarPorNombre(nombreUsuario);
        if (usuario != null && !usuario.isEliminado()) {
            return usuario;
        } else {
            return null;
        }
    }

    public List<Usuario> listarTodos() {
        return usuarioDao.findAll();
    }
}