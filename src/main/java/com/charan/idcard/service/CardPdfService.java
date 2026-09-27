package com.charan.idcard.service;

import com.charan.idcard.model.Student;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Reuses CardImageService's rendered PNG and embeds it into a PDF page
 * sized to match the card exactly - no layout logic is duplicated here.
 */
@Service
public class CardPdfService {

    @Autowired
    private CardImageService cardImageService;

    public byte[] renderCardPdf(Student student) throws Exception {
        BufferedImage card = cardImageService.renderCard(student);

        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ImageIO.write(card, "png", imageBytes);

        Document document = new Document(
                new Rectangle(CardLayoutRenderer.WIDTH, CardLayoutRenderer.HEIGHT));
        ByteArrayOutputStream pdfBytes = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, pdfBytes);

        document.open();
        Image pdfImage = Image.getInstance(imageBytes.toByteArray());
        pdfImage.setAbsolutePosition(0, 0);
        document.add(pdfImage);
        document.close();

        return pdfBytes.toByteArray();
    }
}
