package com.example.web;

import com.example.servicio.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/pago")
public class PagoController {

    @Autowired
    private StripeService stripeService;

    @GetMapping("/partido/{id}")
    public String mostrarFormularioPago(@PathVariable Long id, Model model) {
        model.addAttribute("pagoId", id);
        return "pago_partido";
    }

    @PostMapping("/crear-sesion/{id}")
    public String crearSesion(@PathVariable Long id) throws StripeException {
        // monto de ejemplo: 20 USD = 2000 centavos
        Session session = stripeService.crearSesionPago(id, 2000L);
        return "redirect:" + session.getUrl();
    }

    @GetMapping("/exito")
    public String pagoExitoso() {
        return "pago_exitoso";
    }

    @GetMapping("/cancelado")
    public String pagoCancelado() {
        return "mis_pagos";
    }
}
