package elevator.elevator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Elevator extends Thread {

    private String elevatorName;
    private volatile int floor;
    private volatile boolean running = true;
    private int maxWeight = 600;
    private int maxPeople = 8;
    private int fRangeMin;
    private int fRangeMax;
    private int moveDelay;

    private final BlockingDeque<Integer> queue = new LinkedBlockingDeque<>();
    private final Set<Person> passengers = new HashSet<>();
    private final Set<Consumer<Integer>> floorListeners = new HashSet<>();

    private final Object queueLock = new Object();

    /**
     * Callbacks the floor listeners
     */
    private void callbackFloorListeners() {
        int i = 0;

        floorListeners.forEach((listener) -> {
            new Thread(() -> {
                listener.accept(getFloor());
            }, String.format("Elevator%sListener%d", getElevatorName(), i)).start();
        });
    }

    /**
     * Attempts to requests a single floor
     *
     * @param floor Floor
     * @return True if the floor was successfully queued, false if the the floor
     *         was not queued or the floor was already queued
     */
    private boolean simpleRequest(int floor) {
        if ((fRangeMin != 0 && fRangeMax != 0)
                && (floor < fRangeMin || floor > fRangeMax)) {
            throw new IllegalArgumentException(String.format("Outside range (%d, %d)", fRangeMin, fRangeMax));
        }

        // Ensuring uniqueness
        if (!queue.contains(floor)) {
            return queue.offerLast(floor);
        } else {
            return false;
        }
    }

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
     * Adds a floor listener
     *
     * @param listener Listener
     */
    public void addFloorListener(Consumer<Integer> listener) {
        floorListeners.add(listener);
    }

    /**
     * Calculates and sets the prioirity of the elevator stops
     */
    public void prioritize() {
        if (queue.isEmpty()) {
            return;
        }

        int direction = queue.peek() > getLastFloor() ? 1 : -1;

        // Gets slices of the queue containing floors above and below the current floor
        // and orders them by descending and ascending order respectively
        LinkedList<Integer> above = (LinkedList<Integer>) queue.stream()
                .filter(floor -> floor > getLastFloor())
                .sorted((a, b) -> a > b ? 1 : -1)
                .collect(Collectors.toCollection(LinkedList::new)),

                below = (LinkedList<Integer>) queue.stream()
                        .filter(floor -> floor < getLastFloor())
                        .sorted((a, b) -> a < b ? 1 : -1)
                        .collect(Collectors.toCollection(LinkedList::new));

        synchronized (queueLock) {
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
    }

    /**
     * Attempts to requests a single floor
     *
     * @param floor Floor
     * @return True if the floor was successfully queued, false if the the floor
     *         was not queued or the floor was already queued
     */
    public boolean request(int floor) {
        boolean isSuccess = simpleRequest(floor);

        System.out.printf("Elevator %s requested floor %d%n", getElevatorName(), floor);
        prioritize();

        synchronized (this) {
            notify();
        }

        return isSuccess;
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

            if (simpleRequest(floor)) {
                success = true;
            }
        }

        System.out.printf("Elevator %s requested floors %s%n", getElevatorName(), floors.toString());
        prioritize();

        synchronized (this) {
            notify();
        }

        return success;
    }

    /**
     * Attempts to add a passenger to the elevator
     * 
     * @param passenger Passenger
     * @param request Automatically request floor
     */
    public boolean addPassenger(Person passenger, boolean request) {
        // Checks if the elevator can support this person
        if ((getTotalWeight() + passenger.getWeight() <= getMaxWeight())
                && (passengers.size() + 1 <= getMaxPeople())) {
            boolean isSuccess = passengers.add(passenger);

            if (isSuccess) {
                System.out.printf("Person %s entered at floor %d, target floor: %d%n",
                        passenger.getName(),
                        getFloor(),
                        passenger.getTargetFloor());

                if (request) {
                    request(passenger.getTargetFloor());
                    System.out.printf("Person %s requested floor %d", passenger.getName(), passenger.getTargetFloor());
                }

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
        System.out.printf("-- Elevator %s moved to floor %d%n", getElevatorName(), getFloor() + direction);
        setFloor(getFloor() + direction);

        passengers.forEach(person -> person.setFloor(getFloor()));
    }

    /**
     * Moves the elevator to the destinations in the queue
     */
    @Override
    public void run() {
        this.running = true;

        synchronized (this) {
            while (running) {
                while (queue.isEmpty()) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                int floor, direction;

                System.out.printf("Elevator %s started running from floor %d%n", getElevatorName(), getFloor());

                while (!queue.isEmpty()) {
                    synchronized (queueLock) {
                        floor = queue.poll();
                    }

                    direction = floor > getFloor() ? 1 : -1;

                    while (getFloor() != floor) {
                        move(direction);

                        callbackFloorListeners();

                        try {
                            Thread.sleep(getMoveDelay());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    System.out.printf("%nElevator %s stopping at floor %d%n", getElevatorName(), getFloor());
                    if (!ejectPassengers()) {
                        System.out.println("No passengers exited");
                    } else {
                        System.out.printf("Passengers inside: %d, Current weight: %dkg%n%n",
                                passengers.size(),
                                getTotalWeight());
                    }
                    System.out.println();
                }

                System.out.printf("Elevator %s finished running%n", getElevatorName());
            }
        }
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
     * Returns true if the elevator is running
     *
     * @return True if the elevator is running, otherwise false
     */
    public final boolean isRunning() {
        return running;
    }

    /**
     * Gets the elevator name
     * 
     * @return Elevator name
     */
    public final String getElevatorName() {
        return elevatorName;
    }

    /**
     * Gets the last floor in the elevator queue
     *
     * @return Last floor
     */
    public final int getLastFloor() {
        if (queue.isEmpty()) {
            return queue.peekLast();
        } else {
            return getFloor();
        }
    }

    /**
     * Gets the elevator's current floor
     * 
     * @return Floor
     */
    public final int getFloor() {
        return floor;
    }

    /**
     * Gets the elevator's max weight
     * 
     * @return Max weight
     */
    public final int getMaxWeight() {
        return maxWeight;
    }

    /**
     * Gets the elevator's maximum amount of people
     * 
     * @return Maximum amount of people
     */
    public final int getMaxPeople() {
        return maxPeople;
    }

    /**
     * Gets the elevator's active passengers
     *
     * @return Active passengers
     */
    @SuppressWarnings("unchecked")
    public final HashSet<Person> getPassengers() {
        return (HashSet<Person>) ((HashSet<Person>) passengers).clone();
    }

    /**
     * Gets the elevator's movement delay (speed)
     *
     * @return Movement delay
     */
    public int getMoveDelay() {
        return moveDelay;
    }

    /**
     * Stops the elevator from running
     */
    public void stopRunning() {
        this.running = false;
    }

    /**
     * Sets the elevator's floor
     * 
     * @param floor Target floor
     */
    public final void setFloor(int floor) {
        this.floor = floor;
    }

    /**
     * Sets the floor range (bottom floor, top floor)
     * 
     * @param min Bottom floor
     * @param max Top floor
     */
    public final void setFloorRange(int min, int max) {
        fRangeMin = min;
        fRangeMax = max;
    }

    /**
     * Sets the elevator's max weight
     * 
     * @param weight Max weight
     */
    public final void setMaxWeight(int weight) {
        this.maxWeight = weight;
    }

    /**
     * Sets the elevator's maximum amount of people
     * 
     * @param limit Limit
     */
    public final void setMaxPeople(int limit) {
        this.maxPeople = limit;
    }

    /**
     * Sets the elevator's movement delay (speed)
     *
     * @param moveDelay Movement delay
     */
    public void setMoveDelay(int moveDelay) {
        this.moveDelay = moveDelay;
    }

    /**
     * Elevator
     */
    public Elevator() {
        this.elevatorName = "?";
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
        this.elevatorName = name;
    }

    /**
     * Elevator
     * 
     * @param floor Starting floor
     * @param name Elevator name
     */
    public Elevator(int floor, String name) {
        setFloor(floor);
        this.elevatorName = name;
    }

}
