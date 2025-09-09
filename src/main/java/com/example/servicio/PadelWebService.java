package com.example.servicio;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;

@WebService
public class PadelWebService {

    @WebMethod
    public String saludar(String nombre) {
        return "Hola " + nombre + ", bienvenido a Padel App!";
    }

    @WebMethod
    public int calcularRanking(int ganados, int jugados) {
        return jugados == 0 ? 0 : (ganados * 100) / jugados;
    }
}
