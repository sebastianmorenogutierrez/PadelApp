package com.example.servicio;

import com.example.dao.IndividuoDao;
import com.example.domain.Individuo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndividuoServicioImp implements IndividuoServicio {

    @Autowired
    private IndividuoDao individuoDao;

    @Override
    public List<Individuo> listaIndividuos() {
        return individuoDao.findAll();
    }

    @Override
    public void salvar(Individuo individuo) {
        individuoDao.save(individuo);
    }

    @Override
    public void borrar(Individuo individuo) {
        Individuo actual = individuoDao.findById(individuo.getId_individuo()).orElse(null);
        if (actual != null) {
            actual.setEliminado(true);
            individuoDao.save(actual);
        }
    }

    @Override
    public Individuo localizarIndividuo(Individuo individuo) {
        return individuoDao.findById(individuo.getId_individuo()).orElse(null);
    }

    @Override
    public Individuo localizarPorNombreUsuario(String nombreUsuario) {
        return individuoDao.buscarPorNombre(nombreUsuario);
    }
}
