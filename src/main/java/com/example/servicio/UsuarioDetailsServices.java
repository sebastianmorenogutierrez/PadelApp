package com.example.servicio;

// Importamos la interfaz correcta del repositorio (asumiendo que está en el paquete dao)
import com.example.domain.usuario.UsuarioRepository;
import com.example.domain.usuario.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsServices implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuario(username);

        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario NO encontrado: " + username);
        }

        // Verificar si el usuario está eliminado (soft delete)
        if (usuario.isEliminado()) {
            throw new UsernameNotFoundException("Usuario deshabilitado: " + username);
        }

        // Verificar que tenga perfil asignado
        if (usuario.getPerfil() == null) {
            throw new UsernameNotFoundException("Usuario sin perfil asignado: " + username);
        }

        // Obtener el rol del perfil
        String role = usuario.getPerfil().getDescripcion_perfil().toUpperCase();

        // Remover espacios y caracteres especiales si los hay
        role = role.trim().replace(" ", "_");

        return User.builder()
                .username(usuario.getNombreUsuario())
                .password(usuario.getPass_usuario())
                .roles(role) // Spring Security agrega automáticamente el prefijo "ROLE_"
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(usuario.isEliminado()) // Usar el campo eliminado para deshabilitar
                .build();
    }
}