package Utils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;

public class DrawFrame extends JFrame {
    public DrawFrame() {
        setTitle("DrawTest");
//      setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        int screenWidth =screenSize.width;
        int screenHeight = screenSize.height;
        setSize(screenWidth/2, screenHeight/2);
        setLocationByPlatform(true);

        component = new DrawComponent();
        add(component);
    }

    private Component component;
    public static final int DEFAULT_WIDTH = 400;
    public static final int DEFAULT_HEIGHT = 400;
}
