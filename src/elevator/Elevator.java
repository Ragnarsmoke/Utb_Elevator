package elevator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Elevator {

    private String name;
    private volatile int floor;
    private int maxWeight = 600; // Default value
    private int maxPeople = 8; // Default value

    private LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
    private HashSet<Person> passengers = new HashSet<>();

    /**
     * Ejects the passengers that have their stop at the current floor
     */
    private boolean ejectPassengers() {
        Iterator<Person> it = passengers.iterator();
        Person p;
        boolean exited = false;

        while (it.hasNext()) {
            p = it.next();

            if (p.getTargetFloor() == getFloor()) {
                it.remove();
                System.out.printf("Person %s exited at floor %d%n", p.getName(), getFloor());
                exited = true;
            }
        }

        return exited;
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
     * Attempts to requests a single floor
     *
     * @param floor Floor
     * @return True if the floor was successfully queued, false if the the floor
     *         was not queued or the floor was already queued
     */
    public boolean request(int floor) {
        // Ensuring uniqueness
        if (!queue.contains(floor)) {
            return queue.add(floor);
        } else {
            return false;
        }
    }

    /**
     * Requests multiple floors
     *
     * @param floors Floors
     * @return True if any floors were added
     */
    public boolean request(Collection<? extends Integer> floors) {
        Iterator<? extends Integer> it = floors.iterator();
        boolean success = false;
        int floor;

        while (it.hasNext()) {
            floor = it.next();

            if (request(floor)) {
                success = true;
            }
        }

        return success;
    }

    /**
     * Attempts to add a passenger to the elevator
     * 
     * @param passenger Passenger
     */
    public boolean addPassenger(Person passenger) {
        // Checks if the elevator can support this person
        if ((getTotalWeight() + passenger.getWeight() <= getMaxWeight())
                && (passengers.size() + 1 <= getMaxPeople())) {
            boolean isSuccess = passengers.add(passenger);

            if (isSuccess) {
                System.out.printf("Person %s entered at floor %d, target floor: %d%n",
                        passenger.getName(),
                        getFloor(),
                        passenger.getTargetFloor());
                System.out.printf("Passengers inside: %d, Current weight: %dkg%n%n", passengers.size(),
                        getTotalWeight());
            }

            return isSuccess;
        } else {
            return false;
        }
    }

    /**
     * Moves the elevator in the given direction
     * 
     * @param direction Direction
     */
    public void move(int direction) {
        System.out.printf("-- Elevator %s moved to floor %d%n", getName(), floor + direction);
        setFloor(getFloor() + direction);

        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

            System.out.printf("%nElevator %s stopping at floor %d%n", getName(), getFloor());
            if (!ejectPassengers()) {
                System.out.println("No passengers exited");
            } else {
                System.out.printf("Passengers inside: %d, Current weight: %dkg%n%n",
                        passengers.size(),
                        getTotalWeight());
            }
            System.out.println();
        }

        System.out.printf("Elevator %s finished running%n", getName());
    }

    /**
     * Gets the total combined weight of the persons in the elevator
     * 
     * @return Total weight
     */
    public int getTotalWeight() {
        return passengers.stream()
                .mapToInt(Person::getWeight)
                .reduce(0, (a, b) -> a + b);
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
     * Gets the elevator's max weight
     * 
     * @return Max weight
     */
    public int getMaxWeight() {
        return maxWeight;
    }

    /**
     * Gets the elevator's maximum amount of people
     * 
     * @return Maximum amount of people
     */
    public int getMaxPeople() {
        return maxPeople;
    }

    /**
     * Gets the elevator's active passengers
     *
     * @return Active passengers
     */
    @SuppressWarnings("unchecked")
    public HashSet<Person> getPassengers() {
        return (HashSet<Person>) passengers.clone();
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
     * Sets the elevator's max weight
     * 
     * @param weight Max weight
     */
    public void setMaxWeight(int weight) {
        this.maxWeight = weight;
    }

    /**
     * Sets the elevator's maximum amount of people
     * 
     * @param limit Limit
     */
    public void setMaxPeople(int limit) {
        this.maxPeople = limit;
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
