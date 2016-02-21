import java.awt.*;
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
