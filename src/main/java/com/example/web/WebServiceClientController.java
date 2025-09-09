package com.example.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebServiceClientController {

    @GetMapping("/saludo")
    @ResponseBody
    public String obtenerSaludo() {
        // Aquí iría la lógica para consumir el Web Service
        return "Respuesta del servicio SOAP";
    }
}
