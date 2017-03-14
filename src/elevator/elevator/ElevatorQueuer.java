package elevator.elevator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

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
        elevator.addPassenger(person, true);

        elevator.request(person.getTargetFloor());
    }

    /**
     * Process when the elevator hits a floor
     *
     * @param floor Floor
     */
    private final void processFloor(int floor) {
        synchronized (queueLock) {
            LinkedList<Person> filtered = persons.stream()
                    .filter(person -> person.getFloor() == floor)
                    .collect(Collectors.toCollection(LinkedList::new));

            filtered.forEach(person -> {
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

        elevator.request(person.getFloor());
    }

    /**
     * Gets the queue of people at the given floor
     *
     * @param floor Floor
     * @return Subset of people queueing up at the given floor
     */
    public final HashSet<Person> getFloorQueue(int floor) {
        synchronized (queueLock) {
            return (HashSet<Person>) persons.stream()
                    .filter(p -> p.getFloor() == floor)
                    .collect(Collectors.toSet());
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
