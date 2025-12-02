package com.example.servicio;

import com.example.dao.IndividuoDao;
import com.example.domain.Individuo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ⬅️ ¡IMPORTANTE!

import java.util.List;

@Service
public class IndividuoServicioImp implements IndividuoServicio {

    @Autowired
    private IndividuoDao individuoDao;

    @Override
    @Transactional(readOnly = true) // Solo lectura, es opcional pero buena práctica
    public List<Individuo> listaIndividuos() {
        return individuoDao.findAll();
    }

    @Override
    @Transactional //
    public void salvar(Individuo individuo) {
        individuoDao.save(individuo);
    }

    @Override
    @Transactional //
    public void borrar(Individuo individuo) {
        Individuo actual = individuoDao.findById(individuo.getId_individuo()).orElse(null);
        if (actual != null) {
            actual.setEliminado(true);
            individuoDao.save(actual);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Individuo localizarIndividuo(Long idIndividuo) {
        return individuoDao.findById(idIndividuo).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Individuo localizarPorNombreUsuario(String nombreUsuario) {
        return individuoDao.buscarPorNombre(nombreUsuario);
    }
}