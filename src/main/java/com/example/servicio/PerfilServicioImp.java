package com.example.servicio;

import com.example.domain.Perfil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PerfilServicioImp implements PerfilServicio {

    @Autowired
    private PerfilRepositorio perfilRepositorio;

    @Override
    public List<Perfil> listarTodos() {
        return perfilRepositorio.findAll();
    }

    @Override
    public Perfil buscarPorId(Integer id) {
        return perfilRepositorio.findById(id).orElse(null);
    }
}
