package com.example.servicio;

import com.example.dao.PerfilDao;
import com.example.domain.Perfil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PerfilServicioImp implements PerfilServicio {

    @Autowired
    private PerfilDao perfilDao;

    @Override
    public List<Perfil> listarTodos() {
        return perfilDao.findAll();
    }

    @Override
    public Perfil buscarPorId(Integer id) {
        Optional<Perfil> perfilOptional = perfilDao.findById(id);

        return perfilOptional.orElse(null);
    }
}