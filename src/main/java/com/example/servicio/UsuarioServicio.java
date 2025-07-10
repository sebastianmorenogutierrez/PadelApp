package com.example.servicio;

import com.example.domain.Usuario;
import com.example.domain.Individuo;

import com.example.dao.UsuarioDao;
import com.example.dao.IndividuoDao;
import com.example.dao.PerfilDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.transaction.Transactional;

import java.util.List;

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

    public void registrarNuevoUsuario(Usuario usuario) {
        Individuo nuevoIndividuo = usuario.getIndividuo();
        individuoDao.save(nuevoIndividuo);
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
