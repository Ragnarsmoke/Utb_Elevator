package elevator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Elevator {

    private String name;
    private volatile int floor;
    private LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
    private ArrayList<Person> passengers = new ArrayList<>();

    /**
     * Ejects the passengers that have their stop at the current floor
     */
    private boolean ejectPassengers() {
        return false;
    }

    /**
     * Calculates and sets the prioirity of the elevator stops
     */
    public synchronized void prioritize() {
        if (queue.isEmpty()) {
            return;
        }

        int direction = queue.peek() > getFloor() ? 1 : -1;

        // Gets slices of the queue containing floors above and below the current floor
        LinkedList<Integer> above = (LinkedList<Integer>) queue.stream()
                .filter(floor -> floor > getFloor())
                .sorted((a, b) -> a > b ? 1 : -1)
                .collect(Collectors.toCollection(LinkedList::new)),

                below = (LinkedList<Integer>) queue.stream()
                        .filter(floor -> floor < getFloor())
                        .sorted((a, b) -> a < b ? 1 : -1)
                        .collect(Collectors.toCollection(LinkedList::new));

        queue.clear();

        // Prioritize in the given order
        if (direction == 1) {
            queue.addAll(above);
            queue.addAll(below);
        } else if (direction == -1) {
            queue.addAll(below);
            queue.addAll(above);
        }
    }

    /**
     * Queues a single floor
     *
     * @param floor Floor
     */
    public void queue(int floor) {
        queue.add(floor);
    }

    /**
     * Queues multiple floors
     *
     * @param floors Floors
     */
    public void queue(Collection<? extends Integer> floors) {
        queue.addAll(floors);
    }

    /**
     * Moves the elevator in the given direction
     * 
     * @param direction Direction
     */
    public void move(int direction) {
        System.out.printf("Elevator %s moving to floor %d%n", getName(), (floor + direction));
        setFloor(getFloor() + direction);
    }

    /**
     * Moves the elevator to the destinations in the queue
     */
    public void run() {
        int floor, direction;

        System.out.printf("Elevator %s started running from floor %d%n", getName(), getFloor());

        while (!queue.isEmpty()) {
            floor = queue.poll();
            direction = floor > getFloor() ? 1 : -1;

            while (getFloor() != floor) {
                move(direction);
            }

            System.out.printf("Elevator %s stopping at floor %d%n", getName(), getFloor());
            if (!ejectPassengers()) {
                System.out.println("No passengers exited");
            }
        }
    }

    /**
     * Gets the elevator name
     * 
     * @return Elevator name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the elevator's current floor
     * 
     * @return Floor
     */
    public int getFloor() {
        return floor;
    }

    /**
     * Gets the elevator's active passengers
     *
     * @return Active passengers
     */
    public ArrayList<Person> getPassengers() {
        return passengers;
    }

    /**
     * Sets the elevator's floor
     * 
     * @param floor Target floor
     */
    public void setFloor(int floor) {
        this.floor = floor;
    }

    /**
     * Elevator
     */
    public Elevator() {
        this.name = "?";
        setFloor(0);
    }

    /**
     * Elevator
     *
     * @param floor Starting floor
     */
    public Elevator(int floor) {
        this();
        setFloor(floor);
    }

    /**
     * Elevator
     * 
     * @param name Elevator name
     */
    public Elevator(String name) {
        setFloor(0);
        this.name = name;
    }

    /**
     * Elevator
     * 
     * @param floor Starting floor
     * @param name Elevator name
     */
    public Elevator(int floor, String name) {
        setFloor(floor);
        this.name = name;
    }

}
