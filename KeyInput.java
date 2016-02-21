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
