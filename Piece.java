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
