package elevator;

import java.awt.Graphics2D;

import elevator.elevator.Elevator;
import elevator.elevator.ElevatorQueuer;

public class ElevatorRenderer {

    /**
     * Renders elevator
     * 
     * @param g2 Graphics object
     * @param elevator Elevator
     * @param eq Elevator queuer
     */
    public static void render(Graphics2D g2, Elevator elevator, ElevatorQueuer eq) {
        final Graphics2D g = (Graphics2D) g2.create();

        try {
            // Render here
        } finally {
            g.dispose();
        }
    }

}
