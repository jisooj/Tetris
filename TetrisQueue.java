import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;

public class TetrisQueue extends JComponent {
   private static final int LENGTH = 5;
   private static final int SIZE = 10;

   private Queue<Piece> queue;
   private Random rand;
   private Piece[] pieces;

   public TetrisQueue(Piece[] pieces) {
      queue = new LinkedList<Piece>();
      rand = new Random();
      this.pieces = pieces;
      for (int i = 0; i < LENGTH; i++)
         add();
      setPreferredSize(new Dimension(7 * SIZE, 28 * SIZE));
   }

   public void add() {
      int randomIndex = rand.nextInt(pieces.length);
      queue.add(pieces[randomIndex]);
   }

   public Piece remove() {
      return queue.remove();
   }

   public void reset() {
      for (int i = 0; i < LENGTH; i++) {
         queue.remove();
         add();
      }
   }

   public void paintComponent(Graphics g) {
      int curY = 2 * SIZE;
      int middleX = 30;
      for (int i = 0; i < LENGTH; i++) {
         Piece piece = remove();
         Point[] body = piece.getBody();
         for (Point p : body) {
            int x = middleX + p.x * SIZE;
            int y = (piece.getHeight() - p.y) * SIZE + curY;
            g.setColor(piece.getColor());
            g.fillRect(x, y, SIZE, SIZE);
            g.setColor(Color.WHITE);
            g.drawRect(x, y, SIZE, SIZE);
         }
         g.setColor(Color.BLACK);
         g.drawString(i + 1 + "", middleX, 5 * SIZE + curY);
         curY = curY + 5 * SIZE;
         queue.add(piece);
      }
   }

   public Piece advance() {
      Piece piece = remove();
      add();
      repaint();
      return piece;
   }

   /*
   // implement KeyListener to debug queue
   public void keyPressed(KeyEvent e) {
      int keyCode = e.getKeyCode();
      if (keyCode == KeyEvent.VK_ENTER) {
         advance();
         // System.out.println("enter pressed");
      }
   }
   public void keyReleased(KeyEvent e) {}
   public void keyTyped(KeyEvent e) {}
   
   public static void main(String[] args) {
      JFrame frame = new JFrame("tetris queue");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setLocation(500, 50);
      TetrisQueue queue = new TetrisQueue(Piece.getPieces());
      frame.add(queue);
      frame.addKeyListener(queue);
      frame.pack();
      frame.setVisible(true);
   }*/
}
