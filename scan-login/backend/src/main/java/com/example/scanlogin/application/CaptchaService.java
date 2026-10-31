package com.example.scanlogin.application;

import com.example.scanlogin.infrastructure.CaptchaStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/** Issues 4-character image captchas, valid for 3 minutes, verified once. */
@Service
public class CaptchaService {
    /** Digits and letters without visually ambiguous pairs (0/O, 1/I/L). */
    private static final String CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final Duration TTL = Duration.ofMinutes(3);
    private static final Color[] PALETTE = {
            new Color(0x245bea), new Color(0x1aa773), new Color(0xd97706),
            new Color(0x9333ea), new Color(0x0f766e)
    };

    private final CaptchaStore store;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService(CaptchaStore store) {
        this.store = store;
    }

    public CaptchaChallenge issue() {
        StringBuilder code = new StringBuilder(4);
        for (int i = 0; i < 4; i++) code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        String id = UUID.randomUUID().toString().replace("-", "");
        store.save(id, code.toString().toLowerCase(Locale.ROOT), TTL);
        return new CaptchaChallenge(id, render(code.toString()));
    }

    /** Single verification attempt: the stored answer is deleted whether the check passes or not. */
    public void verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入图片验证码");
        String expected = store.consume(captchaId.trim());
        if (expected == null || !expected.equalsIgnoreCase(captchaCode.trim()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误或已过期，请刷新后重试");
    }

    private String render(String code) {
        int width = 150;
        int height = 46;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xf3, 0xf6, 0xfd));
        g.fillRect(0, 0, width, height);

        // Background noise first; characters are drawn last so nothing covers their strokes.
        for (int i = 0; i < 18; i++) {
            g.setColor(new Color(0xd4, 0xde, 0xef));
            g.fillRect(random.nextInt(width), random.nextInt(height), 2, 2);
        }
        for (int i = 0; i < 3; i++) {
            g.setColor(new Color(0xd8, 0xe1, 0xf3));
            g.setStroke(new BasicStroke(1f));
            g.drawLine(random.nextInt(width), random.nextInt(height),
                    random.nextInt(width), random.nextInt(height));
        }

        // Each glyph owns an equal cell and is centred inside it, so neighbours can
        // never touch even at maximum rotation.
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 28);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics(font);
        int slot = width / code.length();
        int ascent = metrics.getAscent();
        int descent = metrics.getDescent();
        for (int i = 0; i < code.length(); i++) {
            String ch = String.valueOf(code.charAt(i));
            int charWidth = metrics.stringWidth(ch);
            int x = i * slot + (slot - charWidth) / 2;
            int baseline = height / 2 + (ascent - descent) / 2;
            g.setColor(PALETTE[random.nextInt(PALETTE.length)]);
            double theta = (random.nextDouble() - 0.5) * 0.44;
            g.rotate(theta, x + charWidth / 2.0, height / 2.0);
            g.drawString(ch, x, baseline);
            g.rotate(-theta, x + charWidth / 2.0, height / 2.0);
        }
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, "png", out)) throw new IllegalStateException("PNG encoder missing");
        } catch (IOException ex) {
            throw new IllegalStateException("Captcha rendering failed", ex);
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    public record CaptchaChallenge(String captchaId, String image) {}
}
