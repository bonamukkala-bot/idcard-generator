package com.charan.idcard.controller;

import com.charan.idcard.model.Student;
import com.charan.idcard.repository.StudentRepository;
import com.charan.idcard.service.CardImageService;
import com.charan.idcard.service.CardPdfService;
import com.charan.idcard.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Controller
@RequestMapping("/students")
public class StudentController {

    @Autowired private StudentService studentService;
    @Autowired private StudentRepository studentRepository;
    @Autowired private CardImageService cardImageService;
    @Autowired private CardPdfService cardPdfService;

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("student", new Student());
        return "register";
    }

    @PostMapping
    public String create(@ModelAttribute Student student,
                          @RequestParam("photo") MultipartFile photo) throws IOException {
        studentService.saveStudent(student, photo);
        return "redirect:/students";
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("students", studentRepository.findAll());
        return "list";
    }

    @GetMapping("/{id}/card.png")
    public void downloadImage(@PathVariable Long id, HttpServletResponse response)
            throws IOException {
        Student student = studentService.getById(id);
        BufferedImage card = cardImageService.renderCard(student);
        response.setContentType("image/png");
        response.setHeader("Content-Disposition",
                "attachment; filename=id_card_" + id + ".png");
        ImageIO.write(card, "png", response.getOutputStream());
    }

    @GetMapping("/{id}/card.pdf")
    public void downloadPdf(@PathVariable Long id, HttpServletResponse response)
            throws Exception {
        Student student = studentService.getById(id);
        byte[] pdf = cardPdfService.renderCardPdf(student);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=id_card_" + id + ".pdf");
        response.getOutputStream().write(pdf);
    }
}
