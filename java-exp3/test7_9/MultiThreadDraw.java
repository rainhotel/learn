package test7_9;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MultiThreadDraw extends JFrame {

    // 线程安全的线段端点列表
    private final List<Point> pointList = Collections.synchronizedList(new ArrayList<>());
    private final Random random = new Random();
    private JPanel drawPanel;

    public MultiThreadDraw() {
        setTitle("多线程绘图演示 — 李林浩 202483290054");
        Container container = getContentPane();

        // 自定义绘图面板
        drawPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.RED);
                synchronized (pointList) {
                    for (Point p : pointList) {
                        g.drawLine(0, 0, p.x, p.y);
                    }
                }
            }
        };
        drawPanel.setLayout(new BorderLayout());

        JLabel tipLabel = new JLabel("点击面板添加线段", JLabel.CENTER);
        tipLabel.setOpaque(false);
        drawPanel.add(tipLabel, BorderLayout.CENTER);

        container.add(drawPanel, BorderLayout.CENTER);

        // 底部按钮
        JButton startButton = new JButton("启动新线程");
        startButton.setBorder(BorderFactory.createLineBorder(Color.blue, 5));
        container.add(startButton, BorderLayout.SOUTH);

        // 鼠标点击添加线段
        drawPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pointList.add(new Point(e.getX(), e.getY()));
                drawPanel.repaint();
            }
        });

        // 按钮启动新线程随机画线
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new DrawThread(drawPanel, pointList, random).start();
            }
        });

        setSize(350, 450);
        setLocationRelativeTo(null);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MultiThreadDraw::new);
    }
}

/**
 * 绘图线程：随机生成20条线段，间隔0.3秒
 */
class DrawThread extends Thread {

    private final JPanel panel;
    private final List<Point> points;
    private final Random rand;

    public DrawThread(JPanel panel, List<Point> points, Random rand) {
        this.panel = panel;
        this.points = points;
        this.rand = rand;
    }

    @Override
    public void run() {
        for (int i = 0; i < 20; i++) {
            int x = rand.nextInt(panel.getWidth());
            int y = rand.nextInt(panel.getHeight());
            points.add(new Point(x, y));
            panel.repaint();
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
