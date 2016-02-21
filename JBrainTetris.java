import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
Tetris with Brain feature that auto plays pieces.
*/

public class JBrainTetris extends JTetris {
	private static final boolean BRAIN_DEBUG = false;

	private Brain brain;
	private boolean isBrainLoaded;

	private Brain.Move currentMove;
	private int prevCount;

	// constructs initial setup for tetris with the given width and height.
	// Sample width and height calculation : 
	// int pixels = 16;
    // new JBrainTetris(WIDTH * pixels + 2, (HEIGHT + TOP_SPACE) * pixels + 2);
	public JBrainTetris(int width, int height) {
		super(width, height);
		brain = new BetterBrain();
		isBrainLoaded = false;
		prevCount = count;
	}

	// Returns control panel used for tetris with an added AI feature 
	public java.awt.Container createControlPanel() {
		java.awt.Container panel = super.createControlPanel();

	    JCheckBox brain = new JCheckBox("Brain Active");
	    brain.setHorizontalAlignment(SwingConstants.CENTER);
	    brain.setFocusable(false);
	    brain.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	isBrainLoaded = !isBrainLoaded;
		    }
	    });
	    container.add(brain);

		return panel;
	}

	// before tick(TICK_DOWN) happens, brain makes its move
	// Need to keep track of prevCount to check if it's equal to current count
	// Brain.Move has target x, y, piece, score - lower the better
	public void tick(int verb) {
		if (isBrainLoaded && verb == TICK_DOWN) {
			if (BRAIN_DEBUG) 
				System.out.println("if statement in tick");
			int move = getVerb();
			if (move != -1) {
				super.tick(move);
			}
		}
		super.tick(verb);
	}

	// preferrably rotate before translating (or alternate) since 
	// there's no wall kicking right now
	// 
	// there are target x, y, piece and current x, y, piece
	// strategy (draft) : perform an operation with maximum operation-distance 
	// 		also consider that tick(TICK_DOWN) is performed every DELAY
	//
	// MAYBE an object that has a count down that performs operations 
	// based on which one has the highest priority. it contains information about
	// current and target move	
	public int getVerb() {
		if (prevCount != count || currentMove == null) {
			if (BRAIN_DEBUG)
				System.out.println("prevCount " + prevCount + "  currentMove " + currentMove);
			currentMove = brain.bestMove(board, currentPiece, HEIGHT, currentMove);
			prevCount = count;
		}
		// e.g. when the game is over
		if (currentMove == null) {
			return -1;
		}
		int dx = currentX - currentMove.x;
		// int dy = currentY - currentMove.y; // perhaps used for better AI that uses soft drop
		int dRotation = rotationCount(currentPiece, currentMove.piece);
		if (BRAIN_DEBUG)
			System.out.println("after rotationCount method");
		if (dRotation < 0) {
			return ROTATE_LEFT;
		} else if (dRotation > 0) {
			return ROTATE_RIGHT;
		} else if (dx < 0) { // counter clockwise once is +1, twice is +2, three times is -1
			return RIGHT;
		} else if (dx > 0) {
			return LEFT;
		} else { 
			return DROP;
		}
	}

	// Returns the number of rotations needed to get current piece to target piece.
	// [counter clockwise once is +1, twice is +2, three times is -1 to 
	// minimize rotations to get to target piece]
	public int rotationCount(Piece current, Piece target) {
		int rotationCount = 0;
		while (!current.equals(target)) {
			rotationCount++;
			current = current.rightRotation();
		}
		if (rotationCount == 3)
			rotationCount = -1;
		return rotationCount;
	}

   /*
   Creates a Window,
   installs the JTetris or JBrainTetris,
   checks the testMode state,
   install the controls in the WEST.
   */
   public static void main(String[] args) {
      JFrame frame = new JFrame("Tetris 2000");
      JComponent container = (JComponent)frame.getContentPane();
      container.setLayout(new BorderLayout());
      // Set the metal look and feel
      try {
         UIManager.setLookAndFeel(
         UIManager.getCrossPlatformLookAndFeelClassName() );
      } catch (Exception ignored) {}
      
      // Could create a JTetris or JBrainTetris here
      final int pixels = 16;
      JTetris tetris = new JTetris(WIDTH * pixels + 2, (HEIGHT + TOP_SPACE) * pixels + 2);
      container.add(tetris, BorderLayout.CENTER);
      
      if (args.length != 0 && args[0].equals("test")) {
         tetris.testMode = true;
      }
      
      Container panel = tetris.createControlPanel();
      
      Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
      container.add(panel, BorderLayout.WEST);
      container.add(tetris.getQueue(), BorderLayout.EAST);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      // before frame.pack(), w and h of frame is zero
      int x = center.x - frame.getWidth() / 2;
      int y = center.y - frame.getHeight() / 2;
      frame.setLocation(x, y);
      frame.setVisible(true);
   }	
}