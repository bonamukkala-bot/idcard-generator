package com.charan.idcard.service;

import com.charan.idcard.model.Student;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class CardImageService {

    public BufferedImage renderCard(Student student) throws IOException {
        BufferedImage photo = null;
        if (student.getPhotoPath() != null) {
            File photoFile = new File(student.getPhotoPath());
            if (photoFile.exists()) {
                photo = ImageIO.read(photoFile);
            }
        }

        BufferedImage qr;
        try {
            qr = QrCodeGenerator.generate("STUDENT-ID:" + student.getId(), 300, 300);
        } catch (Exception e) {
            qr = null; // never let a QR failure block the whole card
        }

        BufferedImage card = new BufferedImage(
                CardLayoutRenderer.WIDTH, CardLayoutRenderer.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = card.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        CardLayoutRenderer.draw(g, student, photo, qr);
        g.dispose();

        return card;
    }
}
