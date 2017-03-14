package elevator.display;

import java.awt.event.WindowEvent;
import javax.swing.JFrame;

/**
 * Program window
 *
 * @author Emil Bertilsson
 * @version 2015/10/15
 */
@SuppressWarnings("serial")
public class Window extends JFrame {

    /**
     * Initializes the window
     */
    public void init() {
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Closes the window
     */
    public void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * Creates a new display
     *
     * @param title Window title
     * @param exitOnClose Exit the program upon closing
     */
    public Window(String title, boolean exitOnClose) {
        super(title);

        setResizable(false);

        if (exitOnClose) {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        setVisible(true);
    }

}
