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
