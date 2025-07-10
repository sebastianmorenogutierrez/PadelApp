package com.example.servicio;

import com.example.dao.UsuarioDao;
import com.example.domain.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsServices implements UserDetailsService {

    @Autowired
    private UsuarioDao usuarioDao;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        Usuario usuario = usuarioDao.findByNombreUsuario(username);
        if (usuario==null)
        {
            throw new UsernameNotFoundException("Usuario NO encontrado");
        }

        String role = usuario.getPerfil().getDescripcion_perfil().toUpperCase();
        return User.builder()
                .username(usuario.getNombreUsuario())
                .password(usuario.getPass_usuario())
                .roles(role)
                .build();
    }
}
