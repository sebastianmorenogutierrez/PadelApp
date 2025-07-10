package com.example.servicio;

import com.example.domain.Individuo;
import java.util.List;

public interface IndividuoServicio {

    List<Individuo> listaIndividuos();

    void salvar(Individuo individuo);

    void borrar(Individuo individuo);

    Individuo localizarIndividuo(Individuo individuo);

    Individuo localizarPorNombreUsuario(String nombreUsuario); // método agregado
}
