
package com.rishi.venue.controller;

import com.rishi.venue.repository.VenueRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VenueController {

    private final VenueRepository venueRepository;

    public VenueController(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @GetMapping("/venues")
    public String viewVenues(Model model, HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("venues", venueRepository.findAll());

        return "venues";
    }
}
