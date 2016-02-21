/**
 A joke implementation based on LameBrain --
 plays very badly.
*/
public class BadBrain extends LameBrain {

	public double rateBoard(Board board) {
		double score = super.rateBoard(board);
		return( 10000 - score);
	}
}
public class BetterBrain extends LameBrain {
	public double rateBoard(Board board) {
		final int width = board.getWidth();
		final int maxHeight = board.getMaxHeight();
		
		int sumHeight = 0;
		int holes = 0;
		int heightRange = 0;
		// Count the holes, and sum up the heights
		for (int x = 0; x < width; x++) {
			final int colHeight = board.getColumnHeight(x);
			sumHeight += colHeight;

			int y = colHeight - 2;	// addr of first possible hole
			
			while (y >= 0) {
				if (!board.getGrid(x,y)) {
					holes++;
				}
				y--;
			}
		}
		
		double avgHeight = ((double) sumHeight) / width;
		double score = weightScore(maxHeight, holes, avgHeight, board.getHeight());
		
		return score;
	}

	public double weightScore(int maxHeight, int holes, double avgHeight, int height) {
		double score = 0;
		score += maxHeight * 25;
		score += 5 * holes;
		score += (maxHeight - avgHeight) * 15;
		score += 15 * avgHeight;

		return score;
	}
}
import java.awt.*;
import java.util.*;

/**
 Represents a Tetris board -- essentially a 2-d grid
 of booleans. Supports tetris pieces and row clearning.
 Has an "undo" feature that allows clients to add and remove pieces efficiently.
 Does not do any drawing or have any idea of pixels. Intead,
 just represents the abtsract 2-d board.
*/
public final class Board  {
   private static Color GHOST_COLOR = Color.LIGHT_GRAY;

   private int width;
   private int height;

   // [row][col]
   private Color[][] grid;
   private int[] widths;
   private int[] heights;
   private int maxHeight;

   // backup structures
   private Color[][] backupGrid;
   private int[] backupWidths;
   private int[] backupHeights;
   private int backupMaxHeight;

   private boolean DEBUG = false;
   private boolean committed;

   private int totalLineCleared;

   /**
    Creates an empty board of the given width and height
    measured in blocks.
   */
   public Board(int width, int height) {
      this.width = width;
      this.height = height;
   
      grid = new Color[height][width];
      widths = new int[height]; // height many rows that represents widths
      heights = new int[width]; // width many columns that represents heights
      maxHeight = 0;
   
      backupGrid = new Color[height][width];
      backupWidths = new int[height];
      backupHeights = new int[width];
      backupMaxHeight = 0;
   
      committed = true;
      totalLineCleared = 0;
   }

   /**
    Returns the width of the board in blocks.
   */
   public int getWidth() {
      return width;
   }

   /**
    Returns the height of the board in blocks.
   */
   public int getHeight() {
      return height;
   }

   /**
    Returns the max column height present in the board.
    For an empty board this is 0.

    has to be constant run time
   */
   public int getMaxHeight() {
      return maxHeight;
   }

   // Returns total lines cleared in current round
   public int getTotalLineCleared() {
      return totalLineCleared;
   }

   /**
    Checks the board for internal consistency -- used
    for debugging. Checks are calculated from current state of grid
   */
   public void sanityCheck() {
      if (DEBUG) {
         int[] widthCheck = new int[widths.length];
         int[] heightCheck = new int[heights.length];
         int maxHeightCheck = 0;
         for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
               if (grid[row][col] != null) {
                  widthCheck[row]++;
                  heightCheck[col] = row + 1;
               }
            }
         }
      
         for (int row = heightCheck.length - 1; row >= 0; row--)
            if (heightCheck[row] != 0)
               maxHeightCheck = Math.max(maxHeightCheck, heightCheck[row]);
      
         if (maxHeightCheck != maxHeight)
            throw new RuntimeException("Sanity check: maxHeight not matching\nExpected: " +
                  maxHeightCheck + "\tActual: " + maxHeight);
         for (int i = 0; i < widths.length; i++)
            if (widths[i] != widthCheck[i])
               throw new RuntimeException("Sanity check: widths not matching\nExpctd: " +
                     Arrays.toString(widthCheck) + "\nActual: "  + Arrays.toString(widths));
         for (int i = 0; i < heights.length; i++)
            if (heights[i] != heightCheck[i])
               throw new RuntimeException("Sanity check: heights not matching\nExpected" +
                     Arrays.toString(heightCheck) + "\tActual: " + Arrays.toString(heights));
      }
   }

   /**
    Given a piece and an x, returns the y
    value where the piece would come to rest
    if it were dropped straight down at that x.

    <p>
    Implementation: use the skirt and the col heights
    to compute this fast -- O(skirt length).

    returns value represents height index where (0,0)
    will be placed. e.g. 4 means index that can be used
    in array and it is zero based.

    Implementation details -
      height is positive factor
      skirt is negative factor
      goal is to find a column with the greatest net height,
      which turns out to be drop height
      drop height = height - skirt

   */
   public int dropHeight(Piece piece, int x) {
      int dropHeight = 0;
      int[] skirt = piece.getSkirt();
      for (int i = 0; i < skirt.length; i++) {
         if (heights[x + i] - skirt[i] > dropHeight) {
            dropHeight = heights[x + i] - skirt[i];
         }
      }
      return dropHeight;
   }

   // Prints current state of grid.
   // 0 represents empty space
   // 1 represents filled space
   public void printGrid() {
      for (int row = grid.length - 1; row >= 0; row--) {
         System.out.print(widths[row] + "|  ");
         for (int col = 0; col < grid[0].length; col++) {
            System.out.print(new Boolean(grid[row][col] != null).compareTo(false) + "    ");
         }
         System.out.println();
      }
      for (int i = 0; i < width; i++)
         System.out.print("=====");
      System.out.println();
      System.out.print("W/H ");
      for (int i : heights)
         System.out.print(i + "    ");
      System.out.println();
   }

   // returns the y coordinate of ghost piece. (where the piece will land)
   public int ghostHeight(Piece piece, int x, int y) {
      int[] skirt = piece.getSkirt();
      for (int row = y - 1; row >= 0; row--) {
         for (int col = 0; col < skirt.length; col++) {
            Color target = grid[row + skirt[col]][x + col];
            if (target != null) {
               return row + 1;
            }
         }
      }
      return 0;
   }

   /**
    Returns the height of the given column --
    i.e. the y value of the highest block + 1.
    The height is 0 if the column contains no blocks.
   */
   public int getColumnHeight(int x) {
      return heights[x];
   }


   /**
    Returns the number of filled blocks in
    the given row.
   */
   public int getRowWidth(int y) {
      return widths[y];
   }

   // Returns color of given coordinates
   public Color getColor(int x, int y) {
      return grid[y][x];
   }

   /**
    Returns true if the given block is filled in the board.
    Blocks outside of the valid width/height area
    always return true.
   */
   public final boolean getGrid(int x, int y) {
      return (x < 0 || x >= width) || (y < 0 || y >= height) || (grid[y][x] != null);
   }


   public static final int PLACE_OK = 0;
   public static final int PLACE_ROW_FILLED = 1;
   public static final int PLACE_OUT_BOUNDS = 2;
   public static final int PLACE_BAD = 3;

   /**
    Attempts to add the body of a piece to the board.
    Copies the piece blocks into the board grid.
    Returns PLACE_OK for a regular placement, or PLACE_ROW_FILLED
    for a regular placement that causes at least one row to be filled.

    <p>Error cases:
    If part of the piece would fall out of bounds, the placement
    does not change the board at all, and PLACE_OUT_BOUNDS is returned.
    If the placement is "bad" --interfering with existing blocks in the grid --
    then the placement is halted partially complete and PLACE_BAD is returned.
    An undo() will remove the bad placement.
   */
   public int place(Piece piece, int x, int y) {
      if (!committed)
         return PLACE_BAD;
   
      committed = false;
      setBackups();
   
      if (!isValidBound(piece, x, y))
         return PLACE_OUT_BOUNDS;
      if (isBad(piece, x, y))
         return PLACE_BAD;
   
      // change current state by putting a piece
      // at the given position and return constants
      Point[] body = piece.getBody();
      int ghostHeight = ghostHeight(piece, x, y);
      //System.out.println("ghostHeight: " + ghostHeight + ":: x = " + x + " y = " + y);
      for (int i = 0; i < body.length; i++) {
         int fillX = x + body[i].x;
         int fillY = y + body[i].y;
         // fill in ghost piece first to override colors
         // when the piece is at landing location
         //System.out.println(dropHeight(piece, x) + body[i].y + ", " + fillX);
         int ghostY = ghostHeight + body[i].y;
      
         // this comparison is to prevent ghost from being dominant 
         // when it's overlapped with actual block
         if (grid[ghostY][fillX] == null)
            grid[ghostY][fillX] = GHOST_COLOR;
         grid[fillY][fillX] = piece.getColor();
      }
      setWidths();
      setHeights();
      if (isCleared())
         return PLACE_ROW_FILLED;
      sanityCheck();
      return PLACE_OK;
   }

   // sets backup for all the necessary parts of the board
   private void setBackups() {
      // src, srcpos, dest, destpos, length
      System.arraycopy(widths, 0, backupWidths, 0, widths.length);
      System.arraycopy(heights, 0, backupHeights, 0, heights.length);
      backupMaxHeight = maxHeight;
      for (int i = 0; i < height; i++) {
         System.arraycopy(grid[i], 0, backupGrid[i], 0, width);
      }
   }

   // after a piece was dropped, checks whether the place did/didn't
   // overlap with blocks already filled in.
   private boolean isBad(Piece piece, int xOrigin, int yOrigin) {
      Point[] body = piece.getBody();
      for (int i = 0; i < body.length; i++) {
         int x = body[i].x + xOrigin;
         int y = body[i].y + yOrigin;
         if (getGrid(x, y)) {
            return true;
         }
      }
      return false;
   }

   // later, double check to see upper bound checking for y is needed
   // e.g. when game is over
   // Because this method checks validity of the given piece at the
   // given x, y coordinates, upper bounds for width and height are
   // inclusive. e.g. I shape is at x=9, y=0. width of piece is 1
   // which is valid at (9,0)
   private boolean isValidBound(Piece piece, int x, int y) {
      return (x >= 0) && ((x + piece.getWidth()) <= width) &&
             (y >= 0) && ((y + piece.getHeight()) <= height);
   }

   // Returns true if the board currently contains
   // any filled rows. Returns false otherwise.
   private boolean isCleared() {
      for (int i = 0; i < maxHeight; i++)
         if (widths[i] == width)
            return true;
      return false;
   }

   /**
    Deletes rows that are filled all the way across, moving
    things above down. Returns true if any row clearing happened.

    <p>Implementation: This is complicated.
    Ideally, you want to copy each row down
    to its correct location in one pass.
    Note that more than one row may be filled.
   */
   public boolean clearRows() {
      boolean isCleared = false;
      int nextSpot = 0; // nextRow
      for (int i = 0; i < maxHeight; i++) {
         if (widths[i] == width && !isCleared) {
            nextSpot = i;
            isCleared = true;
         } else if (widths[i] != width && isCleared) {
            for (int col = 0; col < width; col++)
               grid[nextSpot][col] = grid[i][col];
            nextSpot++;
         }
      }
      // to stop unnecessary modification to grid
      if (!isCleared)
         return isCleared;
   
      for (int row = nextSpot; row < maxHeight; row++)
         for (int col = 0; col < width; col++)
            grid[row][col] = null;
      //int lineCleared = maxHeight - nextSpot;
      //maxHeight = maxHeight - lineCleared;
      //maxHeight = maxHeight - (maxHeight - nextSpot)
      //          = nextSpot
      int lineCleared = maxHeight - nextSpot;
      totalLineCleared += lineCleared;
      shiftWidths();
      // maxHeight = nextSpot; or in current place
      setHeights();
      committed = false;
      //printGrid();
      sanityCheck();
      return isCleared;
   }

   // opt - setWidths(int maxHeight), setHeights(int maxHeight)
   // setWidths(piece, x, y), setHeights(piece, x, y)

   // sets width (called from place)
   private void setWidths() {
      for (int i = 0; i < height; i++) {
         int rowCount = 0;
         for (int j = 0; j < width; j++) {
            if (grid[i][j] != null)
               rowCount++;
         }
         widths[i] = rowCount;
      }
   }

   // sets height and max height so that internal structures
   // have share the same state
   private void setHeights() {
      maxHeight = 0;
      for(int i = 0; i < width; i++) {
         int colHeight = 0;
         for (int j = height - 1; j >= 0; j--) {
            if (grid[j][i] != null) {
               colHeight = j + 1;
               if (colHeight > maxHeight) {
                  maxHeight = colHeight;
               }
               heights[i] = colHeight;
               break;
            }
            if (j == 0)
               heights[i] = 0;
         }
      
      }
   }

   // called from clearRows - shifting down, same algorithm as clearRows
   // setting max h = single statement
   private void shiftWidths() {
      boolean isCleared = false;
      int nextSpot = 0;
      for (int i = 0; i < maxHeight; i++) {
         if (widths[i] == width && !isCleared) {
            nextSpot = i;
            isCleared = true;
         } else if (widths[i] != width && isCleared) {
            widths[nextSpot] = widths[i];
            nextSpot++;
         }
      }
      if (isCleared) {
         for (int row = nextSpot; row < maxHeight; row++)
            widths[row] = 0;
         maxHeight = nextSpot;
      }
   }

   // called from clearRow - h -= linecleard
   // setting max h = O(width)
   // height doesnt set properly in case where cleared row is above
   // line that contains holes
   private void shiftHeights(int lineCleared) {
      for (int i = 0; i < width; i++) {
         heights[i] -= lineCleared;
         // maxHeight = Math.max(maxHeight, heights[i]);
      }
   }

   /**
    If a place() happens, optionally followed by a clearRows(),
    a subsequent undo() reverts the board to its state before
    the place(). If the conditions for undo() are not met, such as
    calling undo() twice in a row, then the second undo() does nothing.
    See the overview docs.
   */
   public void undo() {
      if (!committed) {
         int tempMaxHeight = maxHeight;
         maxHeight = backupMaxHeight;
         backupMaxHeight = tempMaxHeight;
      
         int[] tempWidth = widths;
         widths = backupWidths;
         backupWidths = tempWidth;
      
         int[] tempHeight = heights;
         heights = backupHeights;
         backupHeights = tempHeight;
      
         Color[][] tempGrid = grid;
         grid = backupGrid;
         backupGrid = tempGrid;
      
         committed = true;
      }
      sanityCheck();
   }


   /**
    Puts the board in the committed state.
      - place(), clearRows() ---> committed = false
      - undo(), commit() ---> committed = true
   */
   public void commit() {
      committed = true;
   }

   public Color[][] returnGrid() { 
      return grid; 
   }
   
   public int[] returnHeights() { 
      return heights; 
   }
   
   public int[] returnWidths() { 
      return widths; 
   }
}
public interface Brain {
	// Move is used as a struct to store a single Move
	// ("static" here means it does not have a pointer to an
	// enclosing Brain object, it's just in the Brain namespace.)
	public static class Move {
		public int x;
		public int y;
		public Piece piece;
		public double score;	// lower scores are better
	}
	
	/**
	 Given a piece and a board, returns a move object that represents
	 the best play for that piece, or returns null if no play is possible.
	 The board should be in the committed state when this is called.
	 "limitHeight" is the bottom section of the board that where pieces must
	  come to rest -- typically 20.
	 If the passed in move is non-null, it is used to hold the result
	 (just to save the memory allocation).
	*/
	public Brain.Move bestMove(Board board, Piece piece, int limitHeight, Brain.Move move);
}
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
import java.awt.*;
import javax.swing.*;

public class ImageFrame extends JFrame {
	private Image img;

	public ImageFrame(Image img) {
		this.img = img;
		Dimension size = new Dimension(img.getWidth(null),
			img.getHeight(null));
		//setSize(size);
		//setPreferredSize(size);
		//setMinimumSize(size);
		setMaximumSize(size);
		setLayout(new FlowLayout());
	}

	public void paintComponent(Graphics g) {
		g.drawImage(img, 0, 0, null);
	}
}import java.awt.*;
import javax.swing.*;

public class ImagePanel extends JPanel {
	private Image img;

	public ImagePanel(Image img) {
		this.img = img;
		Dimension size = new Dimension(img.getWidth(null),
			img.getHeight(null));
		setSize(size);
		//setPreferredSize(size);
		//setMinimumSize(size);
		setMaximumSize(size);
		setLayout(new FlowLayout());
	}

	public void paintComponent(Graphics g) {
		g.drawImage(img, 0, 0, null);
	}
}import java.awt.*;
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
}import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.event.*;

/**
JTetris presents a tetris game in a window.
It handles the GUI and the animation.
The Piece and Board classes handle the
lower-level computations.
*/

public class JTetris extends JComponent {
   // size of the board in blocks
   public static final int WIDTH = 10;
   public static final int HEIGHT = 20;
   
   // Extra blocks at the top for pieces to start.
   // If a piece is sticking up into this area
   // when it has landed -- game over!
   public static final int TOP_SPACE = 4;   
   
   // When this is true, plays a fixed sequence of 100 pieces
   protected boolean testMode = false;
   public final int TEST_LIMIT = 100;
   
   // Is drawing optimized
   // with ghost piece, it needs to be false
   protected boolean DRAW_OPTIMIZE = false;
   
   // Board data structures
   protected Board board;
   protected Piece[] pieces;
   
   // The current piece in play or null
   protected Piece currentPiece;
   protected int currentX;
   protected int currentY;
   protected boolean moved;	// did the player move the piece

   // when hold(), holdPressed becomes true, heldPiece = currentPiece.
   // when holdPressed, next call to hold does nothing 
   // holdPressed becomes false when new piece is loaded
   protected boolean holdPressed;
   protected Piece heldPiece;
   protected Hold hold;

   // The piece we're thinking about playing
   // -- set by computeNewPosition
   protected Piece newPiece;
   protected int newX;
   protected int newY;
   
   // State of the game
   protected boolean gameOn;  // true if we are playing
   protected int count;		   // how many pieces played so far - used for JBrainTetris
   protected TetrisQueue queue;
   
   // Controls
   protected TimeComponent timeLabel;
   protected JButton startButton;
   protected JButton stopButton;
   protected javax.swing.Timer timer;
   protected JSlider speed;
   protected JLabel lineCleared;
   protected JPanel container;

   // handles key controls
   protected KeyInput input;

   public final int DELAY = 1000;	// milliseconds per tick


   public JTetris(int width, int height) {
      super();
      setPreferredSize(new Dimension(width, height));
      gameOn = false;
      holdPressed = false;
      pieces = Piece.getPieces();
      queue = new TetrisQueue(pieces);
      board = new Board(WIDTH, HEIGHT + TOP_SPACE);
      hold = new Hold();

      // Create the Timer object and have it send
      // tick(DOWN) periodically
      timer = new javax.swing.Timer(DELAY, new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            tick(TICK_DOWN);
         }
      });
      
      input = new KeyInput(this, timer);
      addKeyListener(input);
      setFocusable(true);
   }
   
   // Sets the internal state and 
   // starts the timer so the game is happening.
   public void startGame() {
      // cheap way to reset the board state
      board = new Board(WIDTH, HEIGHT + TOP_SPACE);
      queue.reset();
      // draw the new board state once
      repaint();
      
      count = 0;
      gameOn = true;
      hold.setPiece(null);

      lineCleared.setText("Line cleared: 0");
      enableButtons();
      input.setEnabled(true);
      addNewPiece();
      //board.printGrid();
      timer.start();
      timeLabel.start();
   }
   
   // Sets the enabling of the start/stop buttons
   // based on the gameOn state.
   public void enableButtons() {
      startButton.setEnabled(!gameOn);
      stopButton.setEnabled(gameOn);
   }
   
   // Stops the game.
   public void stopGame() {
      gameOn = false;
      input.setEnabled(false);
      enableButtons();
      timer.stop();
      timeLabel.stop();
   }
   
   /*
   Given a piece, tries to install that piece
   into the board and set it to be the current piece.
   Does the necessary repaints.
   If the placement is not possible, then the placement
   is undone, and the board is not changed. The board
   should be in the committed state when this is called.
   Returns the same error code as Board.place().
   */
   public int setCurrent(Piece piece, int x, int y) {
      int result = board.place(piece, x, y);
      if (result <= Board.PLACE_ROW_FILLED) {	// SUCESS
         // repaint the rect where it used to be
         if (currentPiece != null) 
            repaintPiece(currentPiece, currentX, currentY);
         currentPiece = piece;
         currentX = x;
         currentY = y;
         // repaint the rect where it is now
         repaintPiece(currentPiece, currentX, currentY);
      } else {
         board.undo();
      }
      return result;
   }
   
   // Selects the next piece to use using the random generator
   // set in startGame().
   public Piece pickNextPiece() {
      return queue.advance();
   }
   
   public TetrisQueue getQueue() {
      return queue;
   }
   
   public JButton getStopButton() {
      return stopButton;
   }

   public void addNewPiece(Piece holdTemp) {
      count++;
      
      if (testMode && count == TEST_LIMIT + 1) {
         stopGame();
         return;
      }
      
      Piece piece = holdTemp;
      if (piece == null) {
         piece = pickNextPiece();
      }
      
      // Center it up at the top
      int px = (board.getWidth() - piece.getWidth()) / 2;
      int py = board.getHeight() - piece.getHeight();
      
      // commit things the way they are
      board.commit();
      currentPiece = null;
      
      // add the new piece to be in play
      int result = setCurrent(piece, px, py);
      
      // This probably never happens, since
      // the blocks at the top allow space
      // for new pieces to at least be added.
      if (result > Board.PLACE_ROW_FILLED) {
         stopGame();
      }
      tick(TICK_DOWN);
   }

   // Tries to add a new random at the top of the board.
   // Ends the game if it's not possible.
   public void addNewPiece() {
      addNewPiece(null);
   }
 
   public Hold getHold() {
      return hold;
   }

   /*
   Figures a new position for the current piece
   based on the given verb (LEFT, RIGHT, ...).
   The board should be in the committed state --
   i.e. the piece should not be in the board at the moment.
   This is necessary so dropHeight() may be called without
   the piece "hitting itself" on the way down.
   
   Sets the ivars newX, newY, and newPiece to hold
   what it thinks the new piece position should be.
   (Storing an intermediate result like that in
   ivars is a little tacky.)
   */
   public void computeNewPosition(int verb) {
      // As a starting point, the new position is the same as the old
      if (verb == HOLD) {
         if (!holdPressed) {
            holdPressed = true;
            //if (heldPiece != null) {
            Piece temp = heldPiece;
            heldPiece = currentPiece;
            hold.setPiece(heldPiece);
            if (temp == null) {
               addNewPiece();
            } else {
               addNewPiece(temp);
            }
         }
      }
      newPiece = currentPiece;
      newX = currentX;
      newY = currentY;
      
      // Make changes based on the verb
      switch (verb) {
         case LEFT: 
         newX--; 
         break;
         
         case RIGHT: 
         newX++; 
         break;
         
         case ROTATE_RIGHT:
         newPiece = newPiece.rightRotation();
         
         // tricky: make the piece appear to rotate about its center
         // can't just leave it at the same lower-left origin as the
         // previous piece.
         newX = newX + (currentPiece.getWidth() - newPiece.getWidth()) / 2;
         newY = newY + (currentPiece.getHeight() - newPiece.getHeight()) / 2;
         break;
         
         case ROTATE_LEFT:
         newPiece = newPiece.leftRotation();
         newX = newX + (currentPiece.getWidth() - newPiece.getWidth()) / 2;
         newY = newY + (currentPiece.getHeight() - newPiece.getHeight()) / 2;
         break;
         
         case DOWN: case TICK_DOWN:
         newY--; 
         break;
         
         case DROP:
         // note: if the piece were in the board, it would interfere here
         //newY = board.dropHeight(newPiece, newX);
         newY = board.ghostHeight(newPiece, newX, newY);
         break;
         
         case HOLD:
         break;

         default:
         throw new RuntimeException("Bad verb");
      }
   }
   
   public static final int TICK_DOWN = 0;
   public static final int LEFT = 1;
   public static final int RIGHT = 2;
   public static final int DROP = 3;
   public static final int DOWN = 4;
   
   public static final int ROTATE_RIGHT = 5;
   public static final int ROTATE_LEFT = 6;
   public static final int HOLD = 7;

   /*
   Called to change the position of the current piece.
   Each key press call this once with the verbs
   LEFT RIGHT ROTATE DROP for the user moves,
   and the timer calls it with the verb DOWN to move
   the piece down one square.
   
   Before this is called, the piece is at some location in the board.
   This advances the piece to be at its next location.
   
   Overriden by the brain when it plays.
   */
   public void tick(int verb) {
      if (!gameOn) 
         return;
      
      if (currentPiece != null) {
         board.undo();	// remove the piece from its old position
      }
      
      // Sets the newXXX ivars
      computeNewPosition(verb);
      
      // try out the new position (rolls back if it doesn't work)
      int result = setCurrent(newPiece, newX, newY);
      
      // if row clearing is going to happen, draw the
      // whole board so the green row shows up
      if (result ==  Board.PLACE_ROW_FILLED) 
         repaint();
      
      boolean failed = (result >= Board.PLACE_OUT_BOUNDS);
      
      // if it didn't work, put it back the way it was
      if (failed && currentPiece != null) {
         board.place(currentPiece, currentX, currentY);
      }
      
      /*
      How to detect when a piece has landed:
      if this move hits something on its DOWN verb,
      and the previous verb was also DOWN (i.e. the player was not
      still moving it),  then the previous position must be the correct
      "landed" position, so we're done with the falling of this piece.
      */
      if (hasLanded(verb, failed)) {
         holdPressed = false;
         if (board.clearRows()) {
            lineCleared.setText("Line cleared: " + board.getTotalLineCleared());
            repaint();	// repaint to show the result of the row clearing
         }
         
         // if the board is too tall, we've lost
         if (board.getMaxHeight() > board.getHeight() - TOP_SPACE) {
            stopGame();
         } else { // Otherwise add a new piece and keep playing
            addNewPiece();
         }
      }
      
      // Note if the player made a successful non-DOWN move --
      // used to detect if the piece has landed on the next tick()
      moved = (!failed && verb != DOWN && verb != TICK_DOWN);
   }
   
   private boolean hasLanded(int verb, boolean failed) {
      return verb == DROP || (failed && (verb == DOWN || verb == TICK_DOWN) && !moved);
   }

   // Given a piece and a position for the piece, generates
   // a repaint for the rectangle that just encloses the piece.
   public void repaintPiece(Piece piece, int x, int y) {
      if (DRAW_OPTIMIZE) {
         int px = xPixel(x);
         int py = yPixel(y + piece.getHeight() - 1);
         int pwidth = xPixel(x + piece.getWidth()) - px;
         int pheight = yPixel(y - 1) - py;
         int pDrop = yPixel(board.ghostHeight(piece, x, y) - 1);

         repaint(px, pDrop, pwidth, pheight);
         repaint(px, py, pwidth, pheight);
      } else {
         repaint();
      }
   }
      
   /*
   Pixel helpers.
   These centralize the translation of (x,y) coords
   that refer to blocks in the board to (x,y) coords that
   count pixels. Centralizing these computations here
   is the only prayer that repaintPiece() and paintComponent()
   will be consistent.
   
   The +1's and -2's are to account for the 1 pixel
   rect around the perimeter.
   */
   
   // width in pixels of a block
   private final float dX() {
      return( ((float) (getWidth()) - 2) / board.getWidth() );
   }
   
   // height in pixels of a block
   private final float dY() {
      return( ((float) (getHeight() - 2)) / board.getHeight() );
   }
   
   // the x pixel coord of the left side of a block
   private final int xPixel(int x) {
      return(Math.round(1 + (x * dX())));
   }
   
   // the y pixel coord of the top of a block
   private final int yPixel(int y) {
      return(Math.round(getHeight() - 1 - (y + 1) * dY()));
   }
   
   /*
   Draws the current board with a 1 pixel border
   around the whole thing. Uses the pixel helpers
   above to map board coords to pixel coords.
   Draws rows that are filled all the way across in green.
   */
   public void paintComponent(Graphics g) {
      // Draw a rect around the whole thing
      //g.drawImage(img, 0, 0, null);
      g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
      
      // Draw the line separating the top
      int spacerY = yPixel(board.getHeight() - TOP_SPACE - 1);
      g.drawLine(0, spacerY, getWidth() - 1, spacerY);
      
      // check if we are drawing with clipping
      //Shape shape = g.getClip();
      Rectangle clip = null;
      if (DRAW_OPTIMIZE) {
         clip = g.getClipBounds();
      }
      
      // Factor a few things out to help the optimizer
      final int dx = Math.round(dX() - 2);
      final int dy = Math.round(dY() - 2);
      final int bWidth = board.getWidth();
      final int bHeight = board.getHeight();
      
      int x, y;
      // Loop through and draw all the blocks
      // left-right, bottom-top
      for (x = 0; x < bWidth; x++) {
         int left = xPixel(x);	// the left pixel
         
         // right pixel (useful for clip optimization)
         int right = xPixel(x + 1) - 1;
         
         // skip this x if it is outside the clip rect
         if (DRAW_OPTIMIZE && clip != null) {
            if ((right < clip.x) || (left >= (clip.x + clip.width))) 
               continue;
         }
         
         // draw from 0 up to the col height
         final int yHeight = board.getColumnHeight(x);
         for (y = 0; y < yHeight; y++) {
            if (board.getGrid(x, y)) {
               final boolean filled = (board.getRowWidth(y) == bWidth);
               if (filled)
                  g.setColor(Color.GREEN);
               else
                  g.setColor(board.getColor(x, y));
               g.fillRect(left + 1, yPixel(y) + 1, dx, dy);	// +1 to leave a white border
               g.setColor(Color.BLACK);
               g.drawRect(left + 1, yPixel(y) + 1, dx, dy);
            }
         }
      }
   }
   
   // Updates the timer to reflect the current setting of the
   // speed slider.
   public void updateTimer() {
      double value = ((double)speed.getValue()) / speed.getMaximum();
      int delay = (int) (DELAY - value * DELAY);
      if (delay < 50) 
         delay = 50;
      timer.setDelay(delay);
   }
   
   /*
   Creates the panel of UI controls.
   This code is very repetitive -- the GUI/XML
   extensions in Java 1.4 should make this sort
   of ugly code less necessary.
   */
   public java.awt.Container createControlPanel() {
      java.awt.Container panel = Box.createVerticalBox();
      
      // TIME
      timeLabel = new TimeLabel();
      input.addTimer(timeLabel);

      panel.add(Box.createVerticalStrut(12));
      
      // START button
      startButton = new JButton("Start");
      startButton.setFocusable(true);
      startButton.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            startGame();
         }
      });
      
      // STOP button
      stopButton = new JButton("Stop");
      stopButton.setFocusable(true);
      stopButton.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            stopGame();
         }
      });
      
      // start and stop button 
      JPanel startStop = new JPanel(new FlowLayout());
      startStop.add(startButton);
      startStop.add(stopButton);
      enableButtons();

      // LINE CLEARED
      lineCleared = new JLabel("Line cleared: 0", SwingConstants.CENTER);
      
      // SPEED slider
      speed = new JSlider(0, 200, 75); // min, max, current
      speed.setFocusable(false);
      speed.setPreferredSize(new Dimension(100,15));
      if (testMode) 
         speed.setValue(200);  // max for test mode
      
      speed.addChangeListener( new ChangeListener() {
         // when the slider changes, sync the timer to its value
         public void stateChanged(ChangeEvent e) {
            updateTimer();
         }
      });
      JPanel speedPanel = new JPanel(new FlowLayout());
      speedPanel.add(new JLabel("Speed:"));
      speedPanel.add(speed);

      container = new JPanel(new GridLayout(0, 1));
      container.add(startStop);
      container.add((Component) timeLabel);
      container.add(lineCleared);
      container.add(speedPanel);

      panel.add(container);
            
      return panel;
   }     
}
import java.awt.event.*;
import javax.swing.Timer;
import java.util.*;

public class KeyInput implements KeyListener {
   private JTetris tetris;
	private Timer timer;
	private boolean isPaused;
   private boolean enabled;

   private Set<TimeComponent> timeComponent;

   public KeyInput(JTetris tetris, Timer timer) {
      this.tetris = tetris;
		this.timer = timer;
      timeComponent = new HashSet<TimeComponent>();
   }
   
   // used only to start and stop the game.
   // it's different than pausing the game.
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public void addTimer(TimeComponent timeComponent) {
      this.timeComponent.add(timeComponent);
   }

   private void stopTimers() {
      for (TimeComponent t : timeComponent) {
         t.stop();
      }
   }

   private void startTimers() {
      for (TimeComponent t : timeComponent) {
         t.start();
      }
   }

   private void resumeTimers() {
      for (TimeComponent t : timeComponent) {
         t.resume();
      }
   }
      
   /*
   isPaused && esc
   !isPaused && esc
   isPaused && !esc : paused, not esc -> should not do anything
   !isPaused && !esc : not paused, not esc -> dont enter if branch

   note: each of four cases behave differently
   */
   public void keyPressed(KeyEvent e) {
      if (!enabled)
         return;
      int code = e.getKeyCode();
      if (isPaused || code == KeyEvent.VK_ESCAPE) {
         if (isPaused && code == KeyEvent.VK_ESCAPE) {
            timer.restart();
            resumeTimers();
            isPaused = false;
            tetris.enableButtons();
         } else if (!isPaused && code == KeyEvent.VK_ESCAPE) {
            timer.stop();
            stopTimers();
            isPaused = true;
            tetris.getStopButton().setEnabled(false);
         }
         return;
      }
      switch(code) {
         case KeyEvent.VK_LEFT:
            tetris.tick(tetris.LEFT);
            break;
         case KeyEvent.VK_UP:
            tetris.tick(tetris.ROTATE_RIGHT);
            break;
         case KeyEvent.VK_X:
            tetris.tick(tetris.ROTATE_RIGHT);
            break;
         case KeyEvent.VK_Z:
            tetris.tick(tetris.ROTATE_LEFT);
            break;
         case KeyEvent.VK_RIGHT:
            tetris.tick(tetris.RIGHT);
            break;
         case KeyEvent.VK_DOWN:
            tetris.tick(tetris.DOWN);
            break;
         case KeyEvent.VK_SPACE:
            tetris.tick(tetris.DROP);
            break;
         case KeyEvent.VK_C: 
            tetris.tick(tetris.HOLD);
            break;
      }
   }

   public void keyReleased(KeyEvent e) {}
   public void keyTyped(KeyEvent e) {}
}
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
/**
 A simple Brain implementation.
 bestMove() iterates through all the possible x values
 and rotations to play a particular piece (there are only
 around 10-30 ways to play a piece).
 
 For each play, it uses the rateBoard() message to rate how
 good the resulting board is and it just remembers the
 play with the lowest score. Undo() is used to back-out
 each play before trying the next. To experiment with writing your own
 brain -- just subclass off LameBrain and override rateBoard().
*/

public class LameBrain implements Brain {
	/**
	 Given a piece and a board, returns a move object that represents
	 the best play for that piece, or returns null if no play is possible.
	 See the Brain interface for details.
	*/
	public Brain.Move bestMove(Board board, Piece piece, int limitHeight, Brain.Move move) {
		// Allocate a move object if necessary
		if (move==null) move = new Brain.Move();
		
		double bestScore = 1e20;
		int bestX = 0;
		int bestY = 0;
		Piece bestPiece = null;
		Piece current = piece;
		
		// loop through all the rotations
		while (true) {
			final int yBound = limitHeight - current.getHeight()+1;
			final int xBound = board.getWidth() - current.getWidth()+1;
			
			// For current rotation, try all the possible columns
			for (int x = 0; x<xBound; x++) {
				int y = board.dropHeight(current, x);
				if (y<yBound) {	// piece does not stick up too far
					int result = board.place(current, x, y);
					if (result <= Board.PLACE_ROW_FILLED) {
						if (result == Board.PLACE_ROW_FILLED) board.clearRows();
						
						double score = rateBoard(board);
						
						if (score<bestScore) {
							bestScore = score;
							bestX = x;
							bestY = y;
							bestPiece = current;
						}
					}
					
					board.undo();	// back out that play, loop around for the next
				}
			}
			
			current = current.leftRotation();
			if (current == piece) break;	// break if back to original rotation
		}
		
		if (bestPiece == null) return(null);	// could not find a play at all!
		else {
			move.x=bestX;
			move.y=bestY;
			move.piece=bestPiece;
			move.score = bestScore;
			return(move);
		}
	}
	
	
	/*
	 A simple brain function.
	 Given a board, produce a number that rates
	 that board position -- larger numbers for worse boards.
	 This version just counts the height
	 and the number of "holes" in the board.
	 See Tetris-Architecture.html for brain ideas.
	*/
	public double rateBoard(Board board) {
		final int width = board.getWidth();
		final int maxHeight = board.getMaxHeight();
		
		int sumHeight = 0;
		int holes = 0;
		
		// Count the holes, and sum up the heights
		for (int x=0; x<width; x++) {
			final int colHeight = board.getColumnHeight(x);
			sumHeight += colHeight;
			
			int y = colHeight - 2;	// addr of first possible hole
			
			while (y>=0) {
				if  (!board.getGrid(x,y)) {
					holes++;
				}
				y--;
			}
		}
		
		double avgHeight = ((double)sumHeight)/width;
		
		// Add up the counts to make an overall score
		// The weights, 8, 40, etc., are just made up numbers that appear to work
		return (8*maxHeight + 40*avgHeight + 1.25*holes);	
	}

}
import java.awt.event.*;

public class listener {
   public static void main(String[] args) {
     System.out.println("left key " + KeyEvent.VK_LEFT);
     System.out.println("up key " + KeyEvent.VK_UP);
     System.out.println("right key " + KeyEvent.VK_RIGHT);
     System.out.println("down key " + KeyEvent.VK_DOWN);
   }
}
import java.awt.*;
import java.util.*;
/**
 An immutable representation of a tetris piece in a particular rotation.
 Each piece is defined by the blocks that make up its body.

 Note: only additional info needed for ghost piece is current piece's 
       drop height, which is computed every tick. It's color can be 
       chosen in JTetris
*/
public final class Piece {
   private Point[] body;
   private int[] skirt; // lowest y coordinate of each x position - zero based
   private int width;
   private int height;
   private Piece next;	// counter clockwise rotation
   private Piece prev;  // clockwise rotation

   private Set<Point> pts; //same contents as body. used for fast equals method
   private Color color;    // color of this piece

   private static Piece[] pieces;   // singleton array of first rotations

	/*
	 Defines a new piece given the Points that make up its body.
	 Makes its own copy of the array and the Point inside it.
	 Does not set up the rotations.

	 This constructor is PRIVATE -- if a client
	 wants a piece object, they must use Piece.getPieces().
	*/
   private Piece(Point[] points, Color color) {
      body = points;
      this.color = color;
      pts = new HashSet<Point>();
      // width and heigth default value = 0
      for (int i = 0; i < body.length; i++) {
         width = Math.max(width, body[i].x);
         height = Math.max(height, body[i].y);
         pts.add(body[i]);
      }
      // plus 1 because they specify length, not index
      width++;
      height++;

      skirt = new int[width];
      for (int i = 0; i < width; i++)
         skirt[i] = height;
      for (Point pt : body)
         if (pt.y < skirt[pt.x])
            skirt[pt.x] = pt.y;
   }

   // prints out points contained in this piece's body
   // NOTE : use print() to see block info
	private void printBody() {
		for (Point pt : body) {
			System.out.print("(" + pt.x + ", " + pt.y + ")");
		}
		System.out.println();
	}

   // prints out body and skirt values
	private void print() {
		printBody();
		System.out.print(Arrays.toString(skirt));
		Piece current = next;
		while (!this.equals(current)) {
			printBody();
			System.out.print(Arrays.toString(current.skirt) + " ");
			current = current.next;
		}
		System.out.println();
	}

   // Returns the width of the piece measured in blocks.
   public int getWidth() {
      return width;
   }

   // Returns the height of the piece measured in blocks.
   public int getHeight() {
      return height;
   }

   // Returns a pointer to the piece's body. The caller
   // should not modify this array.
   public Point[] getBody() {
      return body;
   }

    /*
     Returns a pointer to the piece's skirt. For each x value
     across the piece, the skirt gives the lowest y value in the body.
     This useful for computing where the piece will land.
     The caller should not modify this array.
    */
   public int[] getSkirt() {
      return skirt;
   }

	/*
	 Returns a piece that is 90 degrees counter-clockwise
	 rotated from the receiver.

	 <p>Implementation:
	 The Piece class pre-computes all the rotations once.
	 This method just hops from one pre-computed rotation
	 to the next in constant time.
	*/
   public Piece leftRotation() {
      return next;
   }

   // returns a piece with 90 degrees clockwise rotation
   // prev rotation is piece rotaed in clockwise direction
   public Piece rightRotation() {
      return prev;
   }

   // returns color of this piece
   public Color getColor() {
      return color;
   }

	/*
	 Returns true if two pieces are the same --
	 their bodies contain the same points.
	 Interestingly, this is not the same as having exactly the
	 same body arrays, since the points may not be
	 in the same order in the bodies. Used internally to detect
	 if two rotations are effectively the same.
	*/
   public boolean equals(Piece piece) {
      if (this == piece)
         return true;
      for (int i = 0; i < body.length; i++)
         if (!pts.contains(piece.body[i]))
            return false;
      return true;
   }

	/*
	 Returns an array containing the first rotation of
	 each of the 7 standard tetris pieces.
	 The next (counterclockwise) rotation can be obtained
	 from each piece with the {@link #nextRotation()} message.
	 In this way, the client can iterate through all the rotations
	 until eventually getting back to the first rotation.
	*/
   public static Piece[] getPieces() {
      pieces = new Piece[] {
         pieceRow(new Piece(parsePoints("0 0	0 1	0 2	0 3"), Color.CYAN)),	   // 0  I
         pieceRow(new Piece(parsePoints("0 0	0 1	0 2	1 0"), Color.ORANGE)),  // 1  L
         pieceRow(new Piece(parsePoints("0 0	1 0	1 1	1 2"), Color.BLUE)),	   // 2  J
         pieceRow(new Piece(parsePoints("0 0	1 0	1 1	2 1"), Color.GREEN)),   // 3  S
         pieceRow(new Piece(parsePoints("0 1	1 1	1 0	2 0"), Color.RED)),	   // 4  Z
         pieceRow(new Piece(parsePoints("0 0	0 1	1 0	1 1"), Color.YELLOW)),  // 5  O
         pieceRow(new Piece(parsePoints("0 0	1 0	1 1	2 0"), Color.MAGENTA))	// 6  T
         };
      //printInfoRight(pieces);
      return pieces;
   }

   // prints skirt values and points contained in the given piece
   public static void printOne(Piece piece) {
      String skirtValues = String.format("%1$15s" , Arrays.toString(piece.skirt) + " = ");
      System.out.print(skirtValues);
      for (Point p : piece.body) {
         System.out.print("(" + p.x + "," + p.y + ")");
      }
      System.out.println(" " + piece.getWidth() + ", " + piece.getHeight());
   }

   // prints skirt values and points contained in the list of
   // give pieces (uses counter clockwise rotation)
   private static void printInfoLeft(Piece[] pieces) {
      String header = String.format("%1$15s", "SKIRT = ");
      System.out.println(header + " POINTS");
      for (Piece firstPiece : pieces) {
         printOne(firstPiece);
         Piece current = firstPiece.next;
         while (!firstPiece.equals(current)) {
            printOne(current);
            current = current.next;
         }
         System.out.println();
      }
   }

   // prints skirt values and points contained in the list
   // of given pieces (uses clockwise rotation)
   private static void printInfoRight(Piece[] pieces) {
      String header = String.format("%1$15s", "SKIRT = ");
      System.out.println(header + " POINTS");
      for (Piece firstPiece : pieces) {
         printOne(firstPiece);
         Piece current = firstPiece.prev;
         while (!firstPiece.equals(current)) {
            printOne(current);
            current = current.prev;
         }
         System.out.println();
      }
   }


   // creates a link between pieces such that next rotation piece
   // is linked to the next piece.
   // Note: static method allows access to private fields of any instances
   // of object with the same type
   private static Piece pieceRow(Piece firstPiece) {
      Piece current = firstPiece;
      Color c = firstPiece.getColor();
      Piece nextPiece = new Piece(parsePoints(getNextCoord(firstPiece)), c);
		while (!firstPiece.equals(nextPiece)) {
         current.next = nextPiece;
         nextPiece.prev = current;
         current = current.next;
         nextPiece = new Piece(parsePoints(getNextCoord(current)), c);
      }
      current.next = firstPiece;
      firstPiece.prev = current;
      return firstPiece;
   }

   // returns s String to be used in constructor
   // pieceRow(new Piece(parsePoints("0 0	0 1	0 2	0 3")))
   // transformation steps - counter-clockwise rotation
   // swap x and y
   // -swappedX + (originalHeight - 1)
   private static String getNextCoord(Piece piece) {
      String coordinates = "";
      for (Point p : piece.body) {
         int newX = -p.y + (piece.height - 1);
         int newY = p.x;
         coordinates += newX + " " + newY + " ";
      }
      return coordinates;
   }

	/*
	 Given a string of x,y pairs ("0 0	0 1	0 2	1 0"), parses
	 the points into a Point[] array.
	 (Provided code)
	*/
   private static Point[] parsePoints(String string) {
       // could use Arraylist here, but use vector so works on Java 1.1
      Vector<Point> points = new Vector<Point>();
      StringTokenizer tok = new StringTokenizer(string);
      try {
         while(tok.hasMoreTokens()) {
            int x = Integer.parseInt(tok.nextToken());
            int y = Integer.parseInt(tok.nextToken());

            points.addElement(new Point(x, y));
         }
      }
      catch (NumberFormatException e) {
         throw new RuntimeException("Could not parse x,y string:" + string);
         // cheap way to do assert
      }

   	  // Make an array out of the Vector
      Point[] array = new Point[points.size()];
      points.copyInto(array);
      return(array);
   }
}
public class RotationTest {
   public static void main(String[] args) {
      Piece[] p = Piece.getPieces();
   }
}
public class TestPiece {
   public static void main(String[] args) {
      Piece[] pieces = Piece.getPieces();
   }
}
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
public interface TimeComponent {
	public void start();
	public void stop();
   public void resume();
}import java.awt.event.*;
import java.awt.Dimension;
import javax.swing.*;

public class TimeLabel extends JLabel implements TimeComponent {
   private static final int delay = 100;
   private Timer timer;
   private double currentTime;

   public TimeLabel() {
      super("Time: ", SwingConstants.CENTER);
      currentTime = 0;
      setText("Time: 0.0");
      setTimer();
   }

   public void start() {
      currentTime = 0;
      setText("Time: 0.0");
      timer.start();
   }

   public void stop() {
      timer.stop();
   }

   public void resume() {
      timer.start();
   }

   private double round(double n) {
      return Math.round(n * 100) / 100.0;
   }

   private void setTimer() {
      ActionListener counter = new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            currentTime += (delay / 1000.0);
            setText("Time: " + round(currentTime));
         }
      };
      timer = new Timer(delay, counter);
   }
}
