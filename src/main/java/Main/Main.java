package Main;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        JFrame window = new JFrame("Reflective Scripting");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        window.add(new Application());

        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}