import java.awt.event.*;
import javax.swing.*;

public class KeyTest extends JComponent implements KeyListener {
   public static void main(String[] args) {
      JFrame frame = new JFrame("Key listener testing");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(200, 200);
      frame.setLocationRelativeTo(null);
      KeyTest k = new KeyTest();
      frame.add(k);
      frame.addKeyListener(k);
      frame.setVisible(true);

   }
   public void keyPressed(KeyEvent e) {
      System.out.println(e);
   }
   public void keyReleased(KeyEvent e) {}
   public void keyTyped(KeyEvent e) {}
}
