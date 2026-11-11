package com.kumao.live.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.kumao.common.exception.BizException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * 服务端二维码生成：户外直播时主播用手机推流 App 扫码即可拿到推流地址。
 */
public final class QrCodeUtils {

    private QrCodeUtils() {
    }

    public static String toDataUrl(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(image, "PNG", out);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
            }
        } catch (Exception e) {
            throw new BizException("生成推流二维码失败：" + e.getMessage());
        }
    }
}
