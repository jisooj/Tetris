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
