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
}