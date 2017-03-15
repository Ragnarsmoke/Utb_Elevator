package elevator.elevator;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ElevatorQueuer {

    private final List<Person> persons = new LinkedList<>();

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

        if (elevator.request(person.getFloor())) {
            //System.out.println("Notice: Person could not enter the elevator");
        }
    }

    /**
     * Gets the queue of people at the given floor
     *
     * @param floor Floor
     * @return Subset of people queueing up at the given floor
     */
    public final List<Person> getFloorQueue(int floor) {
        synchronized (queueLock) {
            return persons.stream()
                    .filter(p -> p.getFloor() == floor)
                    .collect(Collectors.toCollection(LinkedList::new));
        }
    }

    /**
     * Elevator queue handler
     *
     * @param elevator Elevator
     */
    public ElevatorQueuer(Elevator elevator) {
        this.elevator = elevator;

        elevator.addListener(ElevatorAction.STOP, floor -> {
            processFloor((int) floor);
        });
    }

}
