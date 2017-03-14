package elevator;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Set;

import elevator.display.Canvas;
import elevator.display.Window;
import elevator.elevator.Elevator;
import elevator.elevator.ElevatorQueuer;
import elevator.elevator.Person;

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
        window = new Window("Elevator renderer", true);
        canvas = new Canvas(
                WINDOW_DEFAULTWIDTH,
                WINDOW_DEFAULTHEIGHT,
                RENDER_DEFAULTUPS);

        window.add(canvas);
        canvas.init();
        window.init();

        canvas.useDeviceFrequency();

        canvas.setSurface(g -> {
            renderScene(g);
        });
    }

    /**
     * Renders a floor
     * 
     * @param g Graphics object
     * @param floor Floor
     * @param yOff Y-offset
     * @param height Height
     */
    private void renderFloor(Graphics2D g2, int floor, int yOff, int height) {
        Set<Person> persons = eq.getFloorQueue(floor);

        final Graphics2D g = (Graphics2D) g2.create();

        // Rendering configurations
        int xMin = 50,
                xMax = 400,
                xOff = 0,
                personWidth = 25;

        Font font = new Font("Arial", Font.PLAIN, 14);
        FontMetrics fm = g.getFontMetrics(font);
        g.setFont(font);

        try {
            for (Person p : persons) {
                g.setPaint(Color.GREEN);
                g.fillRect(xMax - xOff - personWidth,
                        yOff - height,
                        personWidth,
                        height);

                String str = Integer.toString(p.getTargetFloor());

                g.setPaint(Color.RED);
                g.drawString(str,
                        xMax - xOff - (personWidth / 2) - (fm.stringWidth(str) / 2),
                        yOff - (height / 2) + 5);

                xOff += personWidth + 25;
            }

            g.setPaint(Color.RED);
            g.drawLine(xMin, yOff, xMax, yOff);
        } finally {
            g.dispose();
        }

        g.setPaint(Color.GREEN);
    }

    /**
     * Renders the elevator
     * 
     * @param g Graphics object
     * @param yOff Y-offset
     * @param height Height
     */
    private void renderElevator(Graphics2D g2, int yOff, int height) {
        final Graphics2D g = (Graphics2D) g2.create();

        // Rendering configurations
        int xMin = 450,
                elevatorWidth = 50,
                elevatorY = yOff - height - ((elevator.getFloor() - elevator.getBottomFloor()) * height);

        try {
            g.setPaint(Color.GRAY);
            g.fillRect(xMin,
                    elevatorY,
                    elevatorWidth,
                    height);

            Font font = new Font("Arial", Font.PLAIN, 14);
            FontMetrics fm = g.getFontMetrics(font);
            String str = Integer.toString(elevator.getFloor());
            g.setFont(font);
            g.setPaint(Color.GREEN);
            g.drawString(str,
                    xMin + (elevatorWidth / 2) - (fm.stringWidth(str) / 2),
                    elevatorY + (height / 2) + 5);
        } finally {
            g.dispose();
        }
    }

    /**
     * Renders the elevator scene
     * 
     * @param g
     */
    private void renderScene(Graphics2D g) {
        int height = 50;
        int floorCount = elevator.getTopFloor() - elevator.getBottomFloor();

        for (int i = 0; i <= floorCount; i++) {
            renderFloor(g, elevator.getBottomFloor() + i, canvas.getHeight() - 50 - (i * height), height);
        }

        renderElevator(g, canvas.getHeight() - height, height);
    }

    public void run() {
        elevator = new Elevator(5, "MAIN");
        elevator.setMoveDelay(200);
        elevator.setFloorRange(1, 10);

        eq = new ElevatorQueuer(elevator);

        Thread elevatorThread = new Thread(elevator, "Elevator" + elevator.getElevatorName());
        elevatorThread.start();

        ElevatorTester.runTester(eq);

        EventQueue.invokeLater(() -> {
            createDisplay();
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        eq.queue(new Person(0, 1, 10));
        eq.queue(new Person(0, 4, 8));
        eq.queue(new Person(0, 3, 6));
        eq.queue(new Person(0, 3, 10));
        eq.queue(new Person(0, 8, 4));
        eq.queue(new Person(0, 7, 3));
        eq.queue(new Person(0, 9, 5));
    }

}
