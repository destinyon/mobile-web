package com.server.backend.config;

import com.server.backend.upload.UploadService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

@Component
@ConditionalOnProperty(prefix = "app.oss", name = "sync-demo-assets", havingValue = "true", matchIfMissing = false)
public class OssDemoAssetSync implements ApplicationRunner {
    private final UploadService uploadService;

    public OssDemoAssetSync(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, DemoAsset> assets = new LinkedHashMap<>();
        assets.put("avatar/demo-user.png", new DemoAsset("why", "USER", new Color(129, 176, 94), new Color(45, 88, 122), "PNG"));
        assets.put("avatar/admin.png", new DemoAsset("熊", "ADMIN", new Color(229, 168, 91), new Color(91, 61, 74), "PNG"));
        assets.put("news/all-england.jpg", new DemoAsset("全英公开赛焦点战", "国羽双打攻防节奏提速", new Color(240, 207, 111), new Color(129, 176, 94), "JPG"));
        assets.put("news/racket.jpg", new DemoAsset("高端进攻拍评测", "中杆弹性与连贯性对比", new Color(77, 150, 131), new Color(45, 88, 122), "JPG"));
        assets.put("news/community.jpg", new DemoAsset("周末对抗赛报名", "社区俱乐部分级编排", new Color(177, 195, 222), new Color(94, 137, 180), "JPG"));
        assets.put("news/match-detail.jpg", new DemoAsset("赛后复盘专题", "图文混排详情页展示", new Color(170, 208, 152), new Color(62, 97, 68), "JPG"));

        for (Map.Entry<String, DemoAsset> entry : assets.entrySet()) {
            byte[] bytes = "PNG".equals(entry.getValue().format) ? png(entry.getValue()) : jpg(entry.getValue());
            uploadService.uploadBytes(entry.getKey(), bytes, "PNG".equals(entry.getValue().format) ? "image/png" : "image/jpeg");
        }
    }

    private byte[] jpg(DemoAsset asset) throws Exception {
        BufferedImage image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        paint(g, image.getWidth(), image.getHeight(), asset);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private byte[] png(DemoAsset asset) throws Exception {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        paintAvatar(g, image.getWidth(), image.getHeight(), asset);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private void paint(Graphics2D g, int width, int height, DemoAsset asset) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        GradientPaint bg = new GradientPaint(0, 0, asset.base, width, height, asset.accent);
        g.setPaint(bg);
        g.fillRect(0, 0, width, height);

        g.setStroke(new BasicStroke(10f));
        g.setColor(new Color(255, 255, 255, 120));
        g.drawRoundRect(72, 86, 1056, 570, 48, 48);
        g.drawLine(120, 390, 1080, 390);
        g.drawLine(600, 120, 600, 650);
        g.drawLine(90, 670, 1110, 670);

        g.setStroke(new BasicStroke(18f));
        g.drawOval(840, 190, 180, 260);
        g.drawLine(900, 440, 960, 620);
        g.drawLine(960, 620, 1020, 710);

        g.setColor(new Color(255, 255, 255, 180));
        g.fillOval(180, 140, 76, 76);

        g.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 50));
        g.setColor(new Color(0, 0, 0, 130));
        g.drawString(asset.title, 62, 86);
        g.setColor(Color.WHITE);
        g.drawString(asset.title, 56, 80);

        g.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 26));
        g.setColor(new Color(0, 0, 0, 110));
        g.drawString(asset.subtitle, 62, 146);
        g.setColor(Color.WHITE);
        g.drawString(asset.subtitle, 56, 140);

        g.setColor(new Color(255, 255, 255, 180));
        g.fillRoundRect(56, 708, 312, 58, 28, 28);
        g.setColor(new Color(15, 37, 20));
        g.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 22));
        g.drawString("羽球在线 · OSS素材", 82, 744);
    }

    private void paintAvatar(Graphics2D g, int width, int height, DemoAsset asset) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint bg = new GradientPaint(0, 0, asset.base, width, height, asset.accent);
        g.setPaint(bg);
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(255, 255, 255, 60));
        g.fill(new Ellipse2D.Double(90, 90, 620, 620));
        g.setColor(Color.WHITE);
        g.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 180));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(asset.title);
        g.drawString(asset.title, (width - textWidth) / 2, 470);
        g.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 40));
        String caption = asset.subtitle;
        int capWidth = g.getFontMetrics().stringWidth(caption);
        g.drawString(caption, (width - capWidth) / 2, 570);
    }

    private record DemoAsset(String title, String subtitle, Color base, Color accent, String format) {
    }
}
