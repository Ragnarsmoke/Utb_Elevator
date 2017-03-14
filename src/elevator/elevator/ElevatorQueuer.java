package elevator.elevator;

import java.util.HashSet;
import java.util.Set;

public class ElevatorQueuer {

    private final Set<Person> persons = new HashSet<>();
    private final Elevator elevator;

    private final Object queueLock = new Object();

    /**
     * Processes a single person
     *
     * @param person Person
     */
    private final void processPerson(Person person) {
        synchronized (queueLock) {
            elevator.addPassenger(person, true);
        }
    }

    /**
     * Process when the elevator hits a floor
     *
     * @param floor Floor
     */
    private final void processFloor(int floor) {
        synchronized (queueLock) {
            persons.stream()
                    .filter(person -> person.getFloor() == floor)
                    .forEach(person -> {
                        persons.remove(person);
                        processPerson(person);
                    });
        }
    }

    /**
     * Queues a person (Waits for the elevator to reach the person's floor)
     * 
     * @param person Person
     */
    public final void queue(Person person) {
        synchronized (queueLock) {
            persons.add(person);
        }
    }

    /**
     * Elevator queue handler
     *
     * @param elevator Elevator
     */
    public ElevatorQueuer(Elevator elevator) {
        this.elevator = elevator;

        elevator.addFloorListener((floor) -> {
            processFloor(floor);
        });
    }

}
