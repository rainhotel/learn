package test7_6;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ProgressBarDemo implements ChangeListener {

    private JLabel statusLabel;
    private JProgressBar progressBar;

    public ProgressBarDemo() {
        int initialValue = 0;

        JFrame frame = new JFrame("JProgressBar进度条演示 — 李林浩 202483290054");
        Container contentPane = frame.getContentPane();

        // 状态标签
        statusLabel = new JLabel("", JLabel.CENTER);
        statusLabel.setToolTipText("当前进度信息");

        // 进度条配置
        progressBar = new JProgressBar();
        progressBar.setOrientation(JProgressBar.HORIZONTAL);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(initialValue);
        progressBar.setStringPainted(true);
        progressBar.addChangeListener(this);
        progressBar.setToolTipText("进度条组件");

        contentPane.add(progressBar, BorderLayout.CENTER);
        contentPane.add(statusLabel, BorderLayout.SOUTH);

        // 窗口配置
        frame.setSize(420, 80);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // 模拟进度更新
        for (int i = 1; i <= 1000000000; i++) {
            if (i % 10000000 == 0) {
                progressBar.setValue(++initialValue);
            }
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        int currentValue = progressBar.getValue();
        statusLabel.setText("当前已完成进度：" + currentValue + "%");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ProgressBarDemo::new);
    }
}
