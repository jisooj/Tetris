import java.awt.event.*;
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
