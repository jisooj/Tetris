import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;

public class Hold extends JComponent {
   private static final int SIZE = 10;

   private Piece currentPiece;

   public Hold() {
      currentPiece = null;
   }

   public void setPiece(Piece piece) {
      currentPiece = piece;
      repaint();
   }

   public void paintComponent(Graphics g) {
      if (currentPiece == null) {
         // blank - clear this compoenent
         // blank before game or whenever game restarts
         // clearRect(x, y, width, height);
      } else {
         int middleX = 25;
         Point[] body = currentPiece.getBody();
         for (Point p : body) {
            int x = middleX + p.x * SIZE;
            int y = (currentPiece.getHeight() - p.y) * SIZE;
            g.setColor(currentPiece.getColor());
            g.fillRect(x, y, SIZE, SIZE);
            g.setColor(Color.WHITE);
            g.drawRect(x, y, SIZE, SIZE);
         }
      }
      g.setColor(Color.BLACK);
      g.drawRect(SIZE, 0, 5 * SIZE, 5 * SIZE);
   }

   public static void main(String[] args) {
      JFrame frame = new JFrame("hold");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setLocation(500, 50);
      frame.setSize(200, 200);
      Hold h = new Hold();
      Piece[] p = Piece.getPieces();
      h.setPiece(p[0]);
      frame.add(h);
      frame.pack();
      frame.setVisible(true);
   }
}
