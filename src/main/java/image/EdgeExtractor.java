package image;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.Color;


public class EdgeExtractor {
    public static BufferedImage extractEdges(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int centerGray = getGray(img, x, y);
                int rightGray = getGray(img, x + 1, y);
                int bottomGray = getGray(img, x, y + 1);

                int dx = Math.abs(centerGray - rightGray);
                int dy = Math.abs(centerGray - bottomGray);

                int edgeStrength = Math.min(255, dx + dy);
                int rgb = new Color(edgeStrength, edgeStrength, edgeStrength).getRGB();
                out.setRGB(x, y, rgb);
            }
        }
        return out;
    }

    private static int getGray(BufferedImage img, int x, int y) {
        Color color = new Color(img.getRGB(x, y));
        return (int) (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
    }


    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("input.png"));
        BufferedImage output = EdgeExtractor.extractEdges(img);

        ImageIO.write(output, "png", new File("output.png"));
        ImageViewer.showImage(output, "Extracted Edges");
    }


}

