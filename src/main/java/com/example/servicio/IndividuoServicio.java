package com.example.servicio;

import com.example.domain.Individuo;
import java.util.List;

public interface IndividuoServicio {

    List<Individuo> listaIndividuos();

    void salvar(Individuo individuo);

    void borrar(Individuo individuo);

    // ✅ CORRECCIÓN: Ahora espera un Long (el ID) en lugar de un objeto Individuo
    Individuo localizarIndividuo(Long idIndividuo);

    Individuo localizarPorNombreUsuario(String nombreUsuario); // método agregado
}