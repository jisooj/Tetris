import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class Tetris extends JApplet {
	// tetris was stored as a field
	public Tetris() {
		JComponent container = (JComponent) this.getContentPane();
		container.setLayout(new BorderLayout());
      // Set the system look and feel
      try {
          UIManager.setLookAndFeel(
              UIManager.getSystemLookAndFeelClassName());
      }
      catch (Exception ignored) {}

      try {
         UIManager.setLookAndFeel("javax.swing.plaf.mac.MacLookAndFeel");
      }
      catch (Exception ignored) {}
		// Could create a JTetris or JBrainTetris here
		final int pixels = 16;
		JTetris tetris = new JBrainTetris(JTetris.WIDTH * pixels + 2,
				(JTetris.HEIGHT + JTetris.TOP_SPACE) * pixels + 2);
		container.add(tetris, BorderLayout.CENTER);

		Container panel = tetris.createControlPanel();

		container.add(panel, BorderLayout.WEST);

      JPanel east = new JPanel();
      east.setLayout(new BoxLayout(east, BoxLayout.Y_AXIS));
      east.add(tetris.getQueue());
      east.add(tetris.getHold());
     	container.add(east, BorderLayout.EAST);
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new Tetris());
		frame.pack();
		// before frame.pack(), w and h of frame is zero
		Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		int x = center.x - frame.getWidth() / 2;
		int y = center.y - frame.getHeight() / 2;
		frame.setLocation(x, y);
		frame.setVisible(true);
	}
}
