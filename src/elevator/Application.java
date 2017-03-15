package elevator;

import java.awt.EventQueue;
import elevator.display.Canvas;
import elevator.display.Window;
import elevator.elevator.Elevator;
import elevator.elevator.ElevatorQueuer;
import elevator.elevator.ElevatorSpawner;

public class Application {

    private static final int WINDOW_DEFAULTWIDTH = 700;
    private static final int WINDOW_DEFAULTHEIGHT = 800;
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

        // Initializes the elevator renderer
        ElevatorRenderer renderer = new ElevatorRenderer(elevator, eq);

        canvas.setSurface(g -> {
            renderer.render(g);
        });
    }

    /**
     * Initializes elevator
     */
    private void initElevator() {
        // Initializes the main elevator
        elevator = new Elevator(1, "MAIN");
        elevator.setMoveDelay(400);
        elevator.setFloorRange(1, 15);

        eq = new ElevatorQueuer(elevator);

        // Initializes the elevator thread
        Thread elevatorThread = new Thread(elevator, "Elevator" + elevator.getElevatorName());
        elevatorThread.start();

        // Initializes the elevator spawner
        ElevatorSpawner tester = new ElevatorSpawner(elevator, eq);
        tester.startSpawning(100, 1000, 1);
    }

    /**
     * Runs the program
     */
    public void run() {
        EventQueue.invokeLater(() -> {
            createDisplay();
        });

        initElevator();
    }

}
