package elevator.elevator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Elevator implements Runnable {

    private class ActionConsumerStruct {

        private ElevatorAction action;
        private Consumer<Object> consumer;

        public Consumer<Object> getConsumer() {
            return consumer;
        }

        public ElevatorAction getAction() {
            return action;
        }

        public ActionConsumerStruct(ElevatorAction action, Consumer<Object> consumer) {
            this.consumer = consumer;
            this.action = action;
        }

    }

    private String elevatorName;
    private volatile int floor;
    private volatile boolean running = true;
    private int maxWeight = Integer.MAX_VALUE;
    private int maxPeople = Integer.MAX_VALUE;
    private int fRangeMin;
    private int fRangeMax;
    private int moveDelay;

    private final Deque<Integer> queue = new ArrayDeque<>();
    private final List<Person> passengers = new LinkedList<>();

    private final Set<ActionConsumerStruct> listeners = new HashSet<>();

    private final Object queueLock = new Object();
    private final Object consumerLock = new Object();
    private final Object passengerLock = new Object();

    /**
     * Callbacks the listeners
     * 
     * @param action Action
     * @param data Data
     */
    private void callbackListeners(ElevatorAction action, Object data) {
        int i = 1;

        for (ActionConsumerStruct listener : listeners) {
            if (listener.getAction() == action) {
                new Thread(() -> {
                    listener.getConsumer().accept(data);
                }, String.format("Elevator%sListener%d", getElevatorName(), i)).start();
            }

            i++;
        }
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

        synchronized (queueLock) {
            // Ensuring uniqueness
            if (!queue.contains(floor)) {
                return queue.offerLast(floor);
            } else {
                return false;
            }
        }
    }

    /**
     * Ejects the passengers that have their stop at the current floor
     */
    private boolean ejectPassengers() {
        Person person;
        boolean exited = false;

        synchronized (passengerLock) {
            Iterator<Person> it = passengers.iterator();

            while (it.hasNext()) {
                person = it.next();

                // Ejects the passenger and removes it from the list
                if (person.getTargetFloor() == getFloor()) {
                    callbackListeners(ElevatorAction.EJECT, person);
                    it.remove();
                    exited = true;
                }
            }
        }

        return exited;
    }

    /**
     * Adds a floor listener
     *
     * @param action Action
     * @param listener Listener
     */
    public void addListener(ElevatorAction action, Consumer<Object> listener) {
        listeners.add(new ActionConsumerStruct(action, listener));
    }

    /**
     * Calculates and sets the prioirity of the elevator stops
     */
    public void prioritize() {
        if (queue.isEmpty()) {
            return;
        }

        int direction = (int) Math.signum(queue.peek() - getLastFloor());

        // Gets slices of the queue containing floors above and below the current floor
        // and orders them by descending and ascending order respectively
        synchronized (queueLock) {
            LinkedList<Integer> above = (LinkedList<Integer>) queue.stream()
                    .filter(floor -> floor > getLastFloor())
                    .sorted((a, b) -> a > b ? 1 : -1)
                    .collect(Collectors.toCollection(LinkedList::new)),

                    below = (LinkedList<Integer>) queue.stream()
                            .filter(floor -> floor < getLastFloor())
                            .sorted((a, b) -> a < b ? 1 : -1)
                            .collect(Collectors.toCollection(LinkedList::new));

            queue.clear();

            // Prioritize in the given order
            if (direction == 1 || direction == 0) {
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

        prioritize();

        synchronized (consumerLock) {
            consumerLock.notify();
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
        synchronized (consumerLock) {
            Iterator<? extends Integer> it = floors.iterator();
            boolean success = false;
            int floor;

            while (it.hasNext()) {
                floor = it.next();

                if (simpleRequest(floor)) {
                    success = true;
                }
            }

            prioritize();
            consumerLock.notify();

            return success;
        }
    }

    /**
     * Attempts to add a passenger to the elevator
     * 
     * @param passenger Passenger
     * @param request Automatically request floor
     */
    public boolean addPassenger(Person passenger, boolean request) {
        synchronized (passengerLock) {
            // Checks if the elevator can support this person
            if ((getTotalWeight() + passenger.getWeight() <= getMaxWeight())
                    && (passengers.size() + 1 <= getMaxPeople())) {
                boolean isSuccess = passengers.add(passenger);

                if (isSuccess && request) {
                    request(passenger.getTargetFloor());
                }

                return isSuccess;
            } else {
                return false;
            }
        }
    }

    /**
     * Moves the elevator in the given direction
     * 
     * @param direction Direction
     */
    public void move(int direction) {
        setFloor(getFloor() + direction);

        synchronized (passengerLock) {
            passengers.forEach(person -> person.setFloor(getFloor()));
        }
    }

    /**
     * Moves the elevator to the destinations in the queue
     */
    @Override
    public void run() {
        this.running = true;

        while (running) {
            while (queue.isEmpty() && running) {
                try {
                    synchronized (consumerLock) {
                        consumerLock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }

            int floor, direction;

            while (!queue.isEmpty() && running) {
                synchronized (queueLock) {
                    floor = queue.poll();
                }

                direction = (int) Math.signum(floor - getFloor());

                while (getFloor() != floor) {
                    move(direction);

                    callbackListeners(ElevatorAction.STOP, getFloor());

                    try {
                        Thread.sleep(getMoveDelay());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                ejectPassengers();
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
     * Gets the elevator's bottom floor
     * 
     * @return Bottom floor
     */
    public int getBottomFloor() {
        return fRangeMin;
    }

    /**
     * Gets the elevator's top floor
     *
     * @return Top floor
     */
    public int getTopFloor() {
        return fRangeMax;
    }

    /**
     * Gets the floor count
     * 
     * @return Floor count
     */
    public int getFloorCount() {
        return getTopFloor() - getBottomFloor() + 1;
    }

    /**
     * Gets the elevator's active passengers
     *
     * @return Active passengers
     */
    public final List<Person> getPassengers() {
        return new ArrayList<>(passengers);
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
