package elevator;

import java.awt.EventQueue;

import elevator.display.Canvas;
import elevator.display.Window;
import elevator.elevator.Elevator;

public class Application {

    private static final int WINDOW_DEFAULTWIDTH = 800;
    private static final int WINDOW_DEFAULTHEIGHT = 600;

    private static final int RENDER_DEFAULTUPS = 0;

    private Window window;
    private Canvas canvas;

    public static void main(String[] args) {
        Application app = new Application();
        app.run();
    }

    /**
     * Creates the program display
     */
    private void createDisplay() {
        window = new Window("Elevator renderer", true);
        canvas = new Canvas(
                WINDOW_DEFAULTWIDTH,
                WINDOW_DEFAULTHEIGHT,
                RENDER_DEFAULTUPS);

        window.add(canvas);
        canvas.init();
        window.init();

        canvas.useDeviceFrequency();
    }

    public void run() {
        Elevator elevator = new Elevator(1, "MAIN");
        elevator.setMoveDelay(250);
        elevator.setFloorRange(1, 10);

        ElevatorTester.runTester(elevator);

        EventQueue.invokeLater(() -> {
            createDisplay();
        });
    }

}
