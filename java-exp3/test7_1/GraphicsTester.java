package test7_1;

import java.awt.*;
import javax.swing.*;

public class GraphicsTester extends JFrame {

    public GraphicsTester() {
        super("Java图形绘制演示 — 李林浩 202483290054");
        setSize(480, 280);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        // 演示不同字体和颜色绘制文字
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(Color.blue);
        g.drawString("SansSerif 粗体 12号 蓝色", 20, 50);

        g.setFont(new Font("Serif", Font.ITALIC, 14));
        g.setColor(new Color(255, 0, 0));
        g.drawString("Serif 斜体 14号 红色", 250, 50);

        // 分隔线
        g.drawLine(20, 60, 460, 60);

        // 矩形
        g.setColor(Color.green);
        g.drawRect(20, 70, 100, 50);
        g.fillRect(130, 70, 100, 50);

        // 圆角矩形
        g.setColor(Color.yellow);
        g.drawRoundRect(240, 70, 100, 50, 50, 50);
        g.fillRoundRect(350, 70, 100, 50, 50, 50);

        // 3D矩形
        g.setColor(Color.cyan);
        g.draw3DRect(20, 130, 100, 50, true);
        g.fill3DRect(130, 130, 100, 50, false);

        // 椭圆
        g.setColor(Color.pink);
        g.drawOval(240, 130, 100, 50);
        g.fillOval(350, 130, 100, 50);

        // 弧形
        g.setColor(new Color(0, 120, 20));
        g.drawArc(20, 190, 100, 50, 0, 90);
        g.fillArc(130, 190, 100, 50, 0, 90);

        // 多边形
        g.setColor(Color.black);
        int[] xVals1 = {250, 280, 290, 300, 330, 310, 320, 290, 260, 270};
        int[] yVals1 = {210, 210, 190, 210, 210, 220, 230, 220, 230, 220};
        g.drawPolygon(xVals1, yVals1, 10);

        int[] xVals2 = {360, 390, 400, 410, 440, 420, 430, 400, 370, 380};
        g.fillPolygon(xVals2, yVals1, 10);
    }

    public static void main(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        GraphicsTester app = new GraphicsTester();
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
