package elevator;

import java.awt.EventQueue;
import elevator.display.Canvas;
import elevator.display.Window;
import elevator.elevator.Elevator;
import elevator.elevator.ElevatorQueuer;
import elevator.elevator.ElevatorSpawner;

public class Application {

    private static final int WINDOW_DEFAULTWIDTH = 800;
    private static final int WINDOW_DEFAULTHEIGHT = 600;
    private static final int RENDER_DEFAULTUPS = 0;

    private Window window;
    private Canvas canvas;
    private Elevator elevator;
    private ElevatorQueuer eq;

    public static void main(String[] args) {
        Application app = new Application();
        app.run();
    }

    /**
     * Creates the program display
     */
    private void createDisplay() {
        // Creates a new window and canvas object
        window = new Window("Elevator renderer", true);
        canvas = new Canvas(
                WINDOW_DEFAULTWIDTH,
                WINDOW_DEFAULTHEIGHT,
                RENDER_DEFAULTUPS);

        // Adds the canvas to the window and initializes the window and the canvas
        window.add(canvas);
        canvas.init();
        window.init();

        canvas.useDeviceFrequency();
    }

    /**
     * Initializes elevator
     */
    private void initElevator() {
        // Initializes the main elevator
        elevator = new Elevator(5, "MAIN");
        elevator.setMoveDelay(250);
        elevator.setFloorRange(1, 10);

        eq = new ElevatorQueuer(elevator);

        // Initializes the elevator thread
        Thread elevatorThread = new Thread(elevator, "Elevator" + elevator.getElevatorName());
        elevatorThread.start();

        // Initializes the elevator spawner
        ElevatorSpawner tester = new ElevatorSpawner(elevator, eq);
        tester.startSpawning(100, 1000, 5);
    }

    /**
     * Runs the program
     */
    public void run() {
        initElevator();

        EventQueue.invokeLater(() -> {
            createDisplay();
        });
    }

}
