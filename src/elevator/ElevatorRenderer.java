package elevator;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import elevator.elevator.Elevator;
import elevator.elevator.ElevatorAction;
import elevator.elevator.ElevatorQueuer;
import elevator.elevator.Person;

public class ElevatorRenderer {

    // Rendering configurations
    private final static int GLOBAL_MARGIN = 20;

    private final static int PERSON_HEIGHT = 30;
    private final static int PERSON_WIDTH = 20;
    private final static int PERSON_MARGIN = 10;

    private final static int FLOOR_HEIGHT = 40;
    private final static int FLOOR_WIDTH = 300;

    private final static int ELEVATOR_WIDTH = 200;

    // Variables
    private final Elevator elevator;
    private final ElevatorQueuer eq;

    private final Queue<Person> ejected = new ConcurrentLinkedQueue<>();

    // Font
    private Font labelFont = new Font("Arial", Font.BOLD, 16);

    private ScheduledThreadPoolExecutor timerExecutor = new ScheduledThreadPoolExecutor(1024);

    /**
     * Draws the person at the given coordinates
     *
     * @param g Graphics object
     * @param person Person
     * @param x X-coordinate
     * @param y Y-coordinate
     */
    private void drawPerson(Graphics2D g, Person person, int x, int y) {
        g.setPaint(Color.GREEN);
        g.fillRect(x, y, PERSON_WIDTH, PERSON_HEIGHT);

        g.setPaint(Color.RED);
        g.setFont(labelFont);
        FontMetrics fm = g.getFontMetrics();
        String str = Integer.toString(person.getTargetFloor());

        g.drawString(str, x + PERSON_WIDTH / 2 - fm.stringWidth(str) / 2, y + PERSON_HEIGHT / 2 + fm.getHeight() / 2);
    }

    /**
     * Draws a list of persons at the given coordinates
     *
     * @param g Graphics object
     * @param person Person
     * @param x X-coordinate
     * @param y Y-coordinate
     */
    private void drawPersons(Graphics2D g, List<Person> list, int x, int y) {
        int iteration = 0;

        for (Person person : list) {
            int xOff = iteration * PERSON_WIDTH + iteration * PERSON_MARGIN;
            drawPerson(g, person, x + xOff, y);
            iteration++;
        }
    }

    /**
     * Draws a floor at the given coordinates
     *
     * @param g Graphics object
     * @param floor Floor
     * @param x X-coordinate
     * @param y Y-coordinate
     */
    private void drawFloor(Graphics2D g, int floor, int x, int y) {
        g.setPaint(Color.RED);
        g.drawLine(x, y + FLOOR_HEIGHT, x + FLOOR_WIDTH, y + FLOOR_HEIGHT);
    }

    /**
     * Draws the elevator at the given coordinates
     *
     * @param g Graphics object
     * @param x X-coordinate
     * @param y Y-coordinate
     */
    private void drawElevator(Graphics2D g, int x, int y) {
        g.setPaint(Color.GRAY);
        g.fillRect(x, y, ELEVATOR_WIDTH, FLOOR_HEIGHT);
    }

    /**
     * Renders the floors and persons queued at the floor
     *
     * @param g Graphics object
     * @param x X-coordinate
     * @param y Y-coordinate
     */
    private void renderFloors(Graphics2D g, int x, int y) {
        int iteration = 0;

        for (int i = elevator.getTopFloor(); i >= elevator.getBottomFloor(); i--) {
            int floorY = y + iteration * FLOOR_HEIGHT;
            drawFloor(g, i, x, floorY);

            List<Person> persons = eq.getFloorQueue(i);

            int personsWidth = persons.size() * PERSON_WIDTH + (persons.size() - 1) * PERSON_MARGIN;

            drawPersons(g, persons, x + FLOOR_WIDTH - personsWidth, floorY + (FLOOR_HEIGHT - PERSON_HEIGHT) / 2);

            iteration++;
        }
    }

    /**
     * Renders the elevator
     * 
     * @param g Graphics object
     * @param x X-coordinate
     * @param y Y-coordinate
     */
    private void renderElevator(Graphics2D g, int x, int y) {
        int yOff = (elevator.getFloorCount() - elevator.getFloor()) * FLOOR_HEIGHT;

        drawElevator(g, x, y + yOff);

        List<Person> list = elevator.getPassengers();

        drawPersons(g,
                list,
                x + PERSON_MARGIN,
                y + yOff + (FLOOR_HEIGHT - PERSON_HEIGHT) / 2);
    }

    /**
     * Renders the elevator
     * 
     * @param g Graphics object
     * @param x X-coordinate
     * @param y Y-coordinate
     */
    private void renderEjected(Graphics2D g, int x, int y) {
        List<Person> persons = new LinkedList<>();
        Map<Integer, List<Person>> floors = new HashMap<>();

        for (int i = elevator.getBottomFloor(); i <= elevator.getTopFloor(); i++) {
            floors.put(i, new LinkedList<Person>());
        }

        for (Person p : ejected) {
            floors.get(p.getFloor()).add(p);
        }

        for (int i = elevator.getTopFloor(); i >= elevator.getBottomFloor(); i--) {
            persons = floors.get(i);

            if (persons != null) {
                int floorY = y + (elevator.getTopFloor() - i) * FLOOR_HEIGHT;
                drawPersons(g, persons, x, floorY);
            }
        }
    }

    /**
     * Renders the scene
     * 
     * @param g Graphics object
     */
    private void renderScene(Graphics2D g) {
        renderFloors(g, GLOBAL_MARGIN, GLOBAL_MARGIN);
        renderElevator(g, GLOBAL_MARGIN * 2 + FLOOR_WIDTH, GLOBAL_MARGIN);
        renderEjected(g, GLOBAL_MARGIN * 3 + FLOOR_WIDTH + ELEVATOR_WIDTH, GLOBAL_MARGIN);
    }

    /**
     * Renders elevator
     * 
     * @param g2 Graphics object
     */
    public void render(Graphics2D g2) {
        final Graphics2D g = (Graphics2D) g2.create();

        try {
            renderScene(g);
        } finally {
            g.dispose();
        }
    }

    /**
     * Elevator renderer
     * 
     * @param elevator Elevator
     * @param eq Elevator queuer
     */
    public ElevatorRenderer(Elevator elevator, ElevatorQueuer eq) {
        this.elevator = elevator;
        this.eq = eq;

        elevator.addListener(ElevatorAction.EJECT, p -> {
            final Person person = (Person) p;
            ejected.offer(person);
            timerExecutor.schedule(() -> {
                ejected.poll();
            }, 1, TimeUnit.SECONDS);
        });
    }

}
