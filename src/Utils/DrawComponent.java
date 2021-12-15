package Utils;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

public class DrawComponent extends JComponent {
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        //根据屏幕分辨率设置框架大小
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        //根据屏幕分辨率设置组件位置
        double leftX = screenWidth / 4 - 100;
        double topY = screenHeight / 4 - 75;
        double width = 200;
        double height = 150;

        //画一个矩形
        Rectangle2D rec = new Rectangle2D.Double(leftX, topY, width, height);
        g2.draw(rec);

        //画一个rec矩形的内切椭圆
        Ellipse2D ellipse = new Ellipse2D.Double();
        ellipse.setFrame(rec);
        g2.draw(ellipse);

        //画rec矩形的对角线
        g2.draw(new Line2D.Double(leftX, topY, leftX + width, topY + height));

        //画一个以rec矩形中心为圆心的圆
        double centerX = rec.getCenterX();
        double centerY = rec.getCenterY();
        double radius = 150;
        Ellipse2D circle = new Ellipse2D.Double();
        circle.setFrameFromCenter(centerX, centerY, centerX + radius, centerY + radius);
        g2.draw(circle);
    }
}