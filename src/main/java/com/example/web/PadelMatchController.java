package com.example.web;

import com.example.dao.PadelMatchRepository;
import com.example.domain.PadelMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PadelMatchController {

    @Autowired
    private PadelMatchRepository padelMatchRepository;

    @PostMapping("/createMatch")
    public ModelAndView createMatch(@ModelAttribute PadelMatch padelMatch) {
        var save = padelMatchRepository.save(padelMatch);
        ModelAndView mav = new ModelAndView("success"); // Assuming you have a success.html template
        mav.addObject("message", "Partido de Pádel creado exitosamente!");
        return mav;
    }

}