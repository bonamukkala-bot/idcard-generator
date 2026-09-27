package com.charan.idcard.service;

import com.charan.idcard.model.Student;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Premium institutional ID card renderer.
 * Renders a modern, security-grade identity card using pure Java2D vector graphics.
 * Maintained centrally so both CardImageService (PNG) and CardPdfService (PDF)
 * produce identical high-fidelity output.
 */
public class CardLayoutRenderer {

    public static final int WIDTH = 638;
    public static final int HEIGHT = 1013;

    // Card boundary constants
    private static final int CARD_PAD = 14;
    private static final int CARD_X = CARD_PAD;
    private static final int CARD_Y = CARD_PAD;
    private static final int CARD_W = WIDTH - (CARD_PAD * 2);
    private static final int CARD_H = HEIGHT - (CARD_PAD * 2);
    private static final int CARD_RADIUS = 34;

    public static void draw(Graphics2D g, Student student, BufferedImage photo, BufferedImage qr) {
        // 1. Enable ultra-high-quality rendering hints
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // 2. Clear canvas with a soft neutral background backdrop
        g.setColor(new Color(238, 242, 246));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 3. Draw multi-layered soft drop shadow for the main card
        for (int i = 6; i >= 1; i--) {
            int alpha = (int) (14.0 / i);
            g.setColor(new Color(15, 23, 42, Math.max(alpha, 2)));
            g.fill(new RoundRectangle2D.Double(
                    CARD_X - i, CARD_Y - (i / 2) + 3, CARD_W + (i * 2), CARD_H + (i * 2),
                    CARD_RADIUS + (i * 2), CARD_RADIUS + (i * 2)));
        }

        // Determine dynamic department accent color
        Color deptColor = getDepartmentColor(student != null ? student.getDepartment() : null);

        // Create clipped graphics context for card contents
        Graphics2D gCard = (Graphics2D) g.create();
        RoundRectangle2D.Double cardShape = new RoundRectangle2D.Double(
                CARD_X, CARD_Y, CARD_W, CARD_H, CARD_RADIUS, CARD_RADIUS);
        gCard.setClip(cardShape);

        // 4. Fill clean card body background
        gCard.setColor(Color.WHITE);
        gCard.fill(cardShape);

        // 5. Header Banner (~180px) with smooth vertical gradient & diagonal textured stripes
        int headerH = 178;
        drawHeaderBanner(gCard, deptColor, headerH);

        // 6. Header Branding & Monogram Logo
        drawHeaderBranding(gCard);

        // 7. Department Corner Ribbon & Accent Bar
        drawDepartmentRibbon(gCard, deptColor, student != null ? student.getDepartment() : null);

        // 8. Faint repeating anti-forgery watermark across the body
        drawSecurityWatermark(gCard, student);

        // 9. Footer Band & Security Details (~240px)
        int footerY = CARD_Y + CARD_H - 240;
        int footerH = 240;
        drawFooterBand(gCard, qr, deptColor, footerY, footerH);

        // 10. Floating Photo Box (Overlapping header banner for 3D depth)
        int photoW = 186;
        int photoH = 226;
        int photoX = (WIDTH - photoW) / 2;
        int photoY = CARD_Y + 86;
        drawPhotoBox(gCard, photo, photoX, photoY, photoW, photoH);

        // 11. Student Name & Typography Hierarchy
        int detailsStartY = photoY + photoH + 34;
        drawStudentDetails(gCard, student, deptColor, detailsStartY);

        // 12. Decorative Security Stripe at bottom edge
        drawSecurityStripe(gCard, deptColor);

        // Dispose card clip graphics
        gCard.dispose();

        // 13. Outer card fine border outline for crisp definition
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(203, 213, 225, 220));
        g.draw(cardShape);
    }

    /**
     * Computes the accent color dynamically based on the student's department:
     * - Computer Science / IT = Royal Blue
     * - Electronics / Electrical = Purple
     * - Mechanical / Aero = Orange
     * - Civil / Biotech / Chemistry = Teal
     * - Management / Business = Amber Gold
     */
    private static Color getDepartmentColor(String dept) {
        if (dept == null || dept.isBlank()) {
            return new Color(37, 99, 235); // Default Royal Blue
        }
        String d = dept.trim().toUpperCase();
        if (d.contains("CS") || d.contains("COMP") || d.contains("IT") || d.contains("INFO") || d.contains("AI") || d.contains("DATA")) {
            return new Color(37, 99, 235); // CS / IT = Vibrant Royal Blue
        } else if (d.contains("EC") || d.contains("EE") || d.contains("ELECT") || d.contains("COMM")) {
            return new Color(124, 58, 237); // ECE / EEE = Rich Purple
        } else if (d.contains("MECH") || d.contains("AUTO") || d.contains("AERO") || d.contains("MANUF")) {
            return new Color(234, 88, 12); // Mechanical = High-energy Orange
        } else if (d.contains("CIVIL") || d.contains("BIO") || d.contains("CHEM") || d.contains("ENV")) {
            return new Color(13, 148, 136); // Civil / Chemical / Biotech = Emerald Teal
        } else if (d.contains("BUS") || d.contains("MBA") || d.contains("MGMT") || d.contains("COMMERCE") || d.contains("FIN")) {
            return new Color(217, 119, 6); // Management / Commerce = Deep Amber Gold
        }
        return new Color(37, 99, 235); // Fallback
    }

    /**
     * Draws a subtle repeating security watermark across the card.
     */
    private static void drawSecurityWatermark(Graphics2D g, Student student) {
        Graphics2D gw = (Graphics2D) g.create();
        gw.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gw.setColor(new Color(20, 40, 80, 8)); // Ultra-faint ~8% opacity
        gw.setFont(new Font("SansSerif", Font.BOLD, 11));

        AffineTransform orig = gw.getTransform();
        gw.rotate(Math.toRadians(-25), WIDTH / 2.0, HEIGHT / 2.0);

        String idText = (student != null && student.getRollNumber() != null && !student.getRollNumber().isBlank())
                ? " / " + student.getRollNumber().toUpperCase()
                : "";
        String text = "OFFICIAL IDENTITY CARD / VERIFIED STUDENT" + idText + " / INSTITUTION SECURED / ";

        for (int y = -200; y < HEIGHT + 400; y += 40) {
            for (int x = -300; x < WIDTH + 400; x += 440) {
                gw.drawString(text, x, y);
            }
        }
        gw.setTransform(orig);
        gw.dispose();
    }

    /**
     * Draws the rich gradient header banner with textured geometric stripes.
     */
    private static void drawHeaderBanner(Graphics2D g, Color deptColor, int headerH) {
        // Deep Navy to Indigo smooth vertical gradient
        GradientPaint headerGradient = new GradientPaint(
                0, CARD_Y, new Color(20, 30, 60),
                0, CARD_Y + headerH, new Color(45, 65, 110));
        g.setPaint(headerGradient);
        g.fillRect(CARD_X, CARD_Y, CARD_W, headerH);

        // Texture: Low-opacity overlapping diagonal geometric lines
        Graphics2D gt = (Graphics2D) g.create();
        gt.setStroke(new BasicStroke(1.2f));

        // Primary diagonal stripes
        gt.setColor(new Color(255, 255, 255, 18));
        for (int x = -100; x < CARD_W + 200; x += 22) {
            gt.draw(new Line2D.Double(CARD_X + x, CARD_Y, CARD_X + x + 90, CARD_Y + headerH));
        }

        // Secondary counter-angle faint grid lines
        gt.setColor(new Color(255, 255, 255, 8));
        for (int x = -100; x < CARD_W + 200; x += 36) {
            gt.draw(new Line2D.Double(CARD_X + x, CARD_Y + headerH, CARD_X + x + 60, CARD_Y));
        }

        // Decorative background geometric circles in header
        gt.setColor(new Color(255, 255, 255, 7));
        gt.fill(new Ellipse2D.Double(CARD_X + CARD_W - 90, CARD_Y - 30, 160, 160));
        gt.setColor(new Color(255, 255, 255, 10));
        gt.setStroke(new BasicStroke(2.0f));
        gt.draw(new Ellipse2D.Double(CARD_X + CARD_W - 90, CARD_Y - 30, 160, 160));

        gt.dispose();

        // Horizontal accent line beneath header
        int barY = CARD_Y + headerH;
        g.setColor(deptColor);
        g.fillRect(CARD_X, barY - 4, CARD_W, 4);

        // Gold subtle highlight stripe
        g.setColor(new Color(245, 197, 24, 230));
        g.fillRect(CARD_X, barY - 6, CARD_W, 2);
    }

    /**
     * Draws the institutional monogram logo, crest and typography branding.
     */
    private static void drawHeaderBranding(Graphics2D g) {
        int logoX = CARD_X + 20;
        int logoY = CARD_Y + 18;
        int logoSize = 48;

        // Monogram outer glow/ring
        g.setColor(new Color(245, 197, 24, 200)); // Gold accent ring
        g.setStroke(new BasicStroke(2.0f));
        g.draw(new Ellipse2D.Double(logoX, logoY, logoSize, logoSize));

        // Monogram inner circle
        GradientPaint logoBg = new GradientPaint(
                logoX, logoY, new Color(30, 58, 105),
                logoX + logoSize, logoY + logoSize, new Color(15, 23, 42));
        g.setPaint(logoBg);
        g.fill(new Ellipse2D.Double(logoX + 2, logoY + 2, logoSize - 4, logoSize - 4));

        // Monogram inner emblem initials ("ID")
        g.setColor(new Color(255, 255, 255, 240));
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        FontMetrics fmLogo = g.getFontMetrics();
        String emblemText = "ID";
        int textX = logoX + (logoSize - fmLogo.stringWidth(emblemText)) / 2;
        int textY = logoY + ((logoSize - fmLogo.getHeight()) / 2) + fmLogo.getAscent();
        g.drawString(emblemText, textX, textY);

        // Header Title and Subtitle
        int titleX = logoX + logoSize + 14;

        // Subtitle
        g.setColor(new Color(191, 219, 254));
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString("INSTITUTE OF TECHNOLOGY & SCIENCE", titleX, logoY + 16);

        // Main Header Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.drawString("COLLEGE ID CARD", titleX, logoY + 38);

        // Top-right Security Badge
        int badgeW = 96;
        int badgeH = 22;
        int badgeX = CARD_X + CARD_W - badgeW - 18;
        int badgeY = logoY + 10;

        g.setColor(new Color(255, 255, 255, 25));
        g.fill(new RoundRectangle2D.Double(badgeX, badgeY, badgeW, badgeH, 11, 11));
        g.setColor(new Color(245, 197, 24, 180)); // Gold outline
        g.setStroke(new BasicStroke(1.0f));
        g.draw(new RoundRectangle2D.Double(badgeX, badgeY, badgeW, badgeH, 11, 11));

        // Security star icon
        drawStar(g, badgeX + 11, badgeY + 11, 5, 3.5, 1.8, new Color(245, 197, 24));

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString("STUDENT", badgeX + 22, badgeY + 15);
    }

    /**
     * Draws the dynamic department vertical ribbon on the left side of the card.
     */
    private static void drawDepartmentRibbon(Graphics2D g, Color deptColor, String deptName) {
        // Left vertical accent bar
        g.setColor(deptColor);
        g.fillRect(CARD_X, CARD_Y, 6, CARD_H);
    }

    /**
     * Draws the floating student photo with 3D drop shadow, rounded corners,
     * crisp 3px white border, or a vector silhouette placeholder.
     */
    private static void drawPhotoBox(Graphics2D g, BufferedImage photo, int x, int y, int w, int h) {
        int radius = 20;

        // Multi-level soft drop shadow under photo box
        for (int i = 5; i >= 1; i--) {
            g.setColor(new Color(15, 23, 42, 12 - (i * 2)));
            g.fill(new RoundRectangle2D.Double(x - i, y - (i / 2) + 3, w + (i * 2), h + (i * 2), radius + (i * 2), radius + (i * 2)));
        }

        RoundRectangle2D.Double photoBounds = new RoundRectangle2D.Double(x, y, w, h, radius, radius);

        Graphics2D gp = (Graphics2D) g.create();
        gp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gp.setClip(photoBounds);

        if (photo != null) {
            // Draw uploaded photo scaled nicely to fill photo container
            gp.drawImage(photo, x, y, w, h, null);
        } else {
            // High-grade vector silhouette on soft gradient background
            GradientPaint silBg = new GradientPaint(
                    x, y, new Color(241, 245, 249),
                    x, y + h, new Color(203, 213, 225));
            gp.setPaint(silBg);
            gp.fill(photoBounds);

            // Subtle background decorative pattern in photo frame
            gp.setColor(new Color(255, 255, 255, 60));
            gp.fill(new Ellipse2D.Double(x + (w / 2) - 75, y - 15, 150, 150));

            // Person Silhouette: Circle Head + Arc Shoulders
            int headD = 62;
            int headX = x + (w - headD) / 2;
            int headY = y + 42;

            gp.setColor(new Color(148, 163, 184)); // Slate 400
            gp.fill(new Ellipse2D.Double(headX, headY, headD, headD));

            // Shoulders
            int shoulderW = w - 36;
            int shoulderH = 105;
            int shoulderX = x + 18;
            int shoulderY = headY + headD + 10;
            gp.fill(new Arc2D.Double(shoulderX, shoulderY, shoulderW, shoulderH, 0, 180, Arc2D.CHORD));

            // Subtle camera icon badge at bottom of silhouette
            int camW = 26;
            int camH = 18;
            int camX = x + (w - camW) / 2;
            int camY = y + h - 30;

            gp.setColor(new Color(255, 255, 255, 180));
            gp.fill(new RoundRectangle2D.Double(camX, camY, camW, camH, 5, 5));
            gp.setColor(new Color(100, 116, 139));
            gp.setStroke(new BasicStroke(1.4f));
            gp.draw(new RoundRectangle2D.Double(camX, camY, camW, camH, 5, 5));
            gp.draw(new Ellipse2D.Double(camX + (camW / 2) - 3.5, camY + (camH / 2) - 3.5, 7, 7));
        }

        gp.dispose();

        // 3px Crisp Pure White Floating Border
        g.setStroke(new BasicStroke(3.0f));
        g.setColor(Color.WHITE);
        g.draw(photoBounds);

        // Subtle 1px outer rim for contrast
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(203, 213, 225, 160));
        g.draw(photoBounds);
    }

    /**
     * Draws the student typography hierarchy:
     * - Full Name (Bold 26pt)
     * - Thin Divider Line with center accent diamond
     * - Department Tag Pill
     * - Label-above-value data grid with custom Java2D vector icon glyphs
     */
    private static void drawStudentDetails(Graphics2D g, Student student, Color deptColor, int startY) {
        String fullName = safe(student != null ? student.getFullName() : "-");
        String rollNo = safe(student != null ? student.getRollNumber() : "-");
        String dept = safe(student != null ? student.getDepartment() : "-");
        String course = safe(student != null ? student.getCourse() : "-");
        String validTill = safe(student != null ? student.getValidTill() : "-");

        // 1. Full Name in bold 27pt
        g.setFont(new Font("SansSerif", Font.BOLD, 27));
        g.setColor(new Color(15, 23, 42)); // Deep Slate 900
        FontMetrics fmName = g.getFontMetrics();
        int nameX = (WIDTH - fmName.stringWidth(fullName)) / 2;
        g.drawString(fullName, nameX, startY);

        // 2. Thin Horizontal Divider line under name
        int divY = startY + 14;
        int divW = CARD_W - 80;
        int divX = CARD_X + 40;

        g.setColor(new Color(226, 232, 240));
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(divX, divY, divX + divW, divY);

        // Center pip
        g.setColor(deptColor);
        g.fill(new RoundRectangle2D.Double((WIDTH / 2) - 16, divY - 2, 32, 4, 2, 2));

        // 3. Label-Above-Value 2-Column Grid with Vector Icon Glyphs
        int col1X = CARD_X + 38;
        int col2X = CARD_X + (CARD_W / 2) + 12;

        int row1Y = divY + 38;
        int row2Y = row1Y + 68;

        // Row 1, Col 1: ROLL NUMBER
        drawField(g, "ROLL NUMBER", rollNo, col1X, row1Y, deptColor, IconType.ID_BADGE);

        // Row 1, Col 2: DEPARTMENT
        drawField(g, "DEPARTMENT", dept, col2X, row1Y, deptColor, IconType.DEPARTMENT);

        // Row 2, Col 1: COURSE / PROGRAM
        drawField(g, "COURSE / PROGRAM", course, col1X, row2Y, deptColor, IconType.COURSE);

        // Row 2, Col 2: VALID TILL
        drawField(g, "VALID TILL", validTill, col2X, row2Y, deptColor, IconType.CALENDAR);
    }

    /**
     * Renders a single field as a label-above-value block with an icon.
     */
    private static void drawField(Graphics2D g, String label, String value, int x, int y, Color accentColor, IconType iconType) {
        int iconSize = 24;
        int iconX = x;
        int iconY = y - 14;

        // Draw Icon background chip
        g.setColor(new Color(241, 245, 249));
        g.fill(new RoundRectangle2D.Double(iconX, iconY, iconSize, iconSize, 6, 6));
        g.setColor(new Color(226, 232, 240));
        g.setStroke(new BasicStroke(1.0f));
        g.draw(new RoundRectangle2D.Double(iconX, iconY, iconSize, iconSize, 6, 6));

        // Draw vector glyph
        drawIconGlyph(g, iconX, iconY, iconSize, accentColor, iconType);

        int textX = x + iconSize + 10;

        // Label in lighter gray (Color(120, 120, 120)) at 12pt
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(120, 120, 120));
        g.drawString(label.toUpperCase(), textX, y - 2);

        // Value in dark gray at 16pt bold
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(30, 41, 59));
        g.drawString(value, textX, y + 19);
    }

    private enum IconType {
        ID_BADGE, DEPARTMENT, COURSE, CALENDAR
    }

    /**
     * Draws lightweight vector icon glyphs using Java2D primitives.
     */
    private static void drawIconGlyph(Graphics2D g, int x, int y, int size, Color color, IconType type) {
        Graphics2D gi = (Graphics2D) g.create();
        gi.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gi.setColor(color);
        gi.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int cx = x + (size / 2);
        int cy = y + (size / 2);

        switch (type) {
            case ID_BADGE:
                gi.draw(new RoundRectangle2D.Double(cx - 6, cy - 7, 12, 14, 2, 2));
                gi.draw(new Line2D.Double(cx - 3, cy - 4, cx - 1, cy - 4));
                gi.draw(new Line2D.Double(cx - 3, cy, cx + 3, cy));
                gi.draw(new Line2D.Double(cx - 3, cy + 3, cx + 1, cy + 3));
                break;

            case DEPARTMENT:
                gi.draw(new Line2D.Double(cx - 6, cy + 6, cx + 6, cy + 6)); // base
                gi.draw(new Line2D.Double(cx - 6, cy - 2, cx + 6, cy - 2)); // pediment base
                Path2D.Double roof = new Path2D.Double();
                roof.moveTo(cx - 6, cy - 2);
                roof.lineTo(cx, cy - 7);
                roof.lineTo(cx + 6, cy - 2);
                gi.draw(roof);
                gi.draw(new Line2D.Double(cx - 4, cy - 2, cx - 4, cy + 6));
                gi.draw(new Line2D.Double(cx, cy - 2, cx, cy + 6));
                gi.draw(new Line2D.Double(cx + 4, cy - 2, cx + 4, cy + 6));
                break;

            case COURSE:
                Path2D.Double cap = new Path2D.Double();
                cap.moveTo(cx, cy - 6);
                cap.lineTo(cx + 7, cy - 2);
                cap.lineTo(cx, cy + 2);
                cap.lineTo(cx - 7, cy - 2);
                cap.closePath();
                gi.draw(cap);
                gi.draw(new Arc2D.Double(cx - 4, cy - 1, 8, 5, 180, 180, Arc2D.OPEN));
                gi.draw(new Line2D.Double(cx + 7, cy - 2, cx + 7, cy + 4));
                break;

            case CALENDAR:
                gi.draw(new RoundRectangle2D.Double(cx - 6, cy - 5, 12, 12, 2, 2));
                gi.draw(new Line2D.Double(cx - 6, cy - 1, cx + 6, cy - 1));
                gi.draw(new Line2D.Double(cx - 3, cy - 7, cx - 3, cy - 4));
                gi.draw(new Line2D.Double(cx + 3, cy - 7, cx + 3, cy - 4));
                gi.fill(new Ellipse2D.Double(cx - 1, cy + 2, 2, 2));
                break;
        }

        gi.dispose();
    }

    /**
     * Draws the modern light gray footer band housing the framed QR code,
     * verification badge, security status, and digital authorized signature.
     */
    private static void drawFooterBand(Graphics2D g, BufferedImage qr, Color deptColor, int footerY, int footerH) {
        // Footer band background: light gray (~Color(245, 247, 250))
        g.setColor(new Color(245, 247, 250));
        g.fillRect(CARD_X, footerY, CARD_W, footerH);

        // Top separator line
        g.setColor(new Color(226, 232, 240));
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(CARD_X, footerY, CARD_X + CARD_W, footerY);

        // Framed QR Code card with subtle shadow
        int qrCardW = 154;
        int qrCardH = 154;
        int qrCardX = CARD_X + 28;
        int qrCardY = footerY + 28;

        // QR box shadow
        for (int i = 3; i >= 1; i--) {
            g.setColor(new Color(15, 23, 42, 10 - (i * 2)));
            g.fill(new RoundRectangle2D.Double(qrCardX - i, qrCardY - (i / 2) + 2, qrCardW + (i * 2), qrCardH + (i * 2), 16, 16));
        }

        // QR box white container
        RoundRectangle2D.Double qrShape = new RoundRectangle2D.Double(qrCardX, qrCardY, qrCardW, qrCardH, 14, 14);
        g.setColor(Color.WHITE);
        g.fill(qrShape);
        g.setColor(new Color(226, 232, 240));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(qrShape);

        // QR Image rendering
        int qrPad = 12;
        int qrImageW = qrCardW - (qrPad * 2);
        int qrImageH = qrCardH - (qrPad * 2);

        if (qr != null) {
            g.drawImage(qr, qrCardX + qrPad, qrCardY + qrPad, qrImageW, qrImageH, null);
        } else {
            // High-fidelity fallback QR code placeholder
            drawQrFallback(g, qrCardX + qrPad, qrCardY + qrPad, qrImageW, qrImageH);
        }

        // Right side footer text & authorization
        int infoX = qrCardX + qrCardW + 24;

        // 1. "SCAN TO VERIFY" badge pill
        int scanPillW = 120;
        int scanPillH = 22;
        int scanPillY = qrCardY + 4;
        g.setColor(new Color(16, 185, 129, 25)); // Emerald light tint
        g.fill(new RoundRectangle2D.Double(infoX, scanPillY, scanPillW, scanPillH, 11, 11));
        g.setColor(new Color(16, 185, 129)); // Emerald Green
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(infoX, scanPillY, scanPillW, scanPillH, 11, 11));

        // Checkmark glyph
        g.draw(new Line2D.Double(infoX + 10, scanPillY + 11, infoX + 14, scanPillY + 15));
        g.draw(new Line2D.Double(infoX + 14, scanPillY + 15, infoX + 20, scanPillY + 7));

        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString("SCAN TO VERIFY", infoX + 26, scanPillY + 15);

        // 2. Explanatory subtitle
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(100, 116, 139));
        g.drawString("Official digitally encrypted", infoX, scanPillY + 40);
        g.drawString("identity credential.", infoX, scanPillY + 56);

        // 3. Authorized Signature Section
        int sigY = scanPillY + 80;

        // Simulated elegant authorized signature curve
        Graphics2D gSig = (Graphics2D) g.create();
        gSig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gSig.setColor(new Color(30, 41, 59, 210));
        gSig.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        CubicCurve2D.Double sigCurve1 = new CubicCurve2D.Double(
                infoX + 5, sigY + 16,
                infoX + 30, sigY - 8,
                infoX + 55, sigY + 26,
                infoX + 90, sigY + 6);
        gSig.draw(sigCurve1);

        CubicCurve2D.Double sigCurve2 = new CubicCurve2D.Double(
                infoX + 85, sigY + 6,
                infoX + 110, sigY - 6,
                infoX + 130, sigY + 20,
                infoX + 165, sigY + 4);
        gSig.draw(sigCurve2);
        gSig.dispose();

        // Signature line
        g.setColor(new Color(203, 213, 225));
        g.setStroke(new BasicStroke(1.0f));
        g.drawLine(infoX, sigY + 22, infoX + 185, sigY + 22);

        // Signature title
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(new Color(100, 116, 139));
        g.drawString("AUTHORIZED REGISTRAR", infoX, sigY + 36);
    }

    /**
     * Draws fallback QR vector squares if QR is unavailable.
     */
    private static void drawQrFallback(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(30, 41, 59));
        // Corner squares (standard QR markers)
        int markerSize = 32;
        drawQrCornerMarker(g, x, y, markerSize);
        drawQrCornerMarker(g, x + w - markerSize, y, markerSize);
        drawQrCornerMarker(g, x, y + h - markerSize, markerSize);

        // Center randomized decorative bits
        g.fillRect(x + markerSize + 6, y + 8, 10, 10);
        g.fillRect(x + markerSize + 20, y + 14, 12, 12);
        g.fillRect(x + 8, y + markerSize + 6, 12, 12);
        g.fillRect(x + 28, y + markerSize + 16, 14, 10);
        g.fillRect(x + markerSize + 10, y + markerSize + 10, 22, 22);
        g.fillRect(x + w - markerSize - 18, y + markerSize + 8, 12, 12);
        g.fillRect(x + markerSize + 8, y + h - markerSize - 14, 18, 10);
    }

    private static void drawQrCornerMarker(Graphics2D g, int x, int y, int size) {
        g.fillRect(x, y, size, size);
        g.setColor(Color.WHITE);
        g.fillRect(x + 4, y + 4, size - 8, size - 8);
        g.setColor(new Color(30, 41, 59));
        g.fillRect(x + 8, y + 8, size - 16, size - 16);
    }

    /**
     * Draws a decorative multi-color security stripe at the bottom edge.
     */
    private static void drawSecurityStripe(Graphics2D g, Color deptColor) {
        int bottomY = CARD_Y + CARD_H;

        // Stripe 1 (top): Gold accent (3px)
        g.setColor(new Color(245, 197, 24));
        g.fillRect(CARD_X, bottomY - 12, CARD_W, 3);

        // Stripe 2 (middle): Department accent color (4px)
        g.setColor(deptColor);
        g.fillRect(CARD_X, bottomY - 9, CARD_W, 4);

        // Stripe 3 (bottom): Deep Navy security baseline (5px)
        g.setColor(new Color(20, 30, 60));
        g.fillRect(CARD_X, bottomY - 5, CARD_W, 5);
    }

    /**
     * Utility method to draw a geometric star for badges.
     */
    private static void drawStar(Graphics2D g, int cx, int cy, int arms, double rOuter, double rInner, Color color) {
        Path2D.Double path = new Path2D.Double();
        double angleStep = Math.PI / arms;
        for (int i = 0; i < arms * 2; i++) {
            double r = (i % 2 == 0) ? rOuter : rInner;
            double a = (i * angleStep) - (Math.PI / 2);
            double px = cx + (r * Math.cos(a));
            double py = cy + (r * Math.sin(a));
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        path.closePath();
        g.setColor(color);
        g.fill(path);
    }

    // Guards against null/overlong values so the layout never breaks.
    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.length() > 24 ? value.substring(0, 24) + "..." : value;
    }
}
