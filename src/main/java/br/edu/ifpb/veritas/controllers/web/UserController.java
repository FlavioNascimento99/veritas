package br.edu.ifpb.veritas.controllers.web;

import br.edu.ifpb.veritas.dto.RegistrationDTO;
import br.edu.ifpb.veritas.services.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final RegistrationService registrationService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDTO", new RegistrationDTO());
        model.addAttribute("pageTitle", "Registro de Usuário");
        model.addAttribute("activePage", "register");
        model.addAttribute("mainContent", "pages/register :: content");
        return "home";
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("registrationDTO") RegistrationDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Registro de Usuário");
            model.addAttribute("activePage", "register");
            model.addAttribute("mainContent", "pages/register :: content");
            return "home";
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error", "As senhas não conferem.");
            model.addAttribute("pageTitle", "Registro de Usuário");
            model.addAttribute("activePage", "register");
            model.addAttribute("mainContent", "pages/register :: content");
            return "home";
        }

        registrationService.registerUser(dto.getFullName(), dto.getEmail(), dto.getPassword(), dto.getConfirmPassword(), dto.getUserType());
        redirectAttributes.addFlashAttribute("successMessage", "Conta criada com sucesso! Faça o login.");
        return "redirect:/login";
    }
}
