package com.example.servicio;

import com.example.domain.usuario.Usuario;
import com.example.domain.Individuo;
import com.example.dao.UsuarioDao;
import com.example.dao.IndividuoDao;
import com.example.dao.PerfilDao; // Asumo que es necesaria
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Async;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * =======================================================================
 * ARCHIVO 1: UsuarioServicio.java
 * Servicio para la gestión de usuarios, credenciales y datos personales (Individuo).
 * =======================================================================
 */
@Service
public class UsuarioServicio {

    @Autowired
    private UsuarioDao usuarioDao;

    @Autowired
    private IndividuoDao individuoDao;

    @Autowired
    private PerfilDao perfilDao; // Se mantiene por si es necesario para roles/perfiles

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void salvar(Usuario usuario) {
        if (usuario.getIndividuo() != null) {
            Individuo individuo = usuario.getIndividuo();
            individuoDao.save(individuo);
        }

        if (usuario.getPass_usuario() != null) {
            String passEncriptada = passwordEncoder.encode(usuario.getPass_usuario());
            usuario.setPass_usuario(passEncriptada);
        }

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
            usuario.setNombreUsuario("ELIMINADO_" + idInt);
            usuario.setPass_usuario("SIN_PASS");
            usuario.setEliminado(true);
            usuarioDao.save(usuario);
        } else {
            System.out.println("No se encontró el usuario con ID: " + idInt);
        }
    }

    // --- MÉTODOS DE BÚSQUEDA Y LISTADO ---

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

        @Service
        class CorreoServicio {
            // Usamos 'class CorreoServicio' en lugar de public, ya que Java no permite dos public
            // classes en un archivo. Además, debe estar fuera de cualquier método.

            /**
             * Envía correos masivos de forma asíncrona usando CompletableFuture.
             */
            @Async
            public CompletableFuture<Void> enviarCorreoMasivo(List<Usuario> usuarios,
                                                              String asunto,
                                                              String mensaje,
                                                              String tipoEvento) {
                // Lógica de simulación:
                System.out.println("Iniciando envío asíncrono de correos a " + usuarios.size() + " usuarios.");

                // Aquí iría la implementación real que itera sobre 'usuarios' y usa JavaMailSender.

                return CompletableFuture.completedFuture(null);
            }
        }
    }
}