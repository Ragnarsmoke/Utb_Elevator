package elevator.elevator;

import java.util.Random;

public class ElevatorSpawner {

    private final Random rand = new Random();

    private volatile boolean keepSpawning = true;
    private final Elevator elevator;
    private final ElevatorQueuer eq;

    /**
     * Spawns passengers randomly in a group
     *
     * @param maxSize Maximum group size
     */
    public void spawnGroup(int maxSize) {
        int groupSize = rand.nextInt(maxSize) + 1,
                floors = elevator.getTopFloor() - elevator.getBottomFloor() + 1,
                floor,
                targetFloor;

        for (int i = 0; i < groupSize; i++) {
            floor = elevator.getBottomFloor() + rand.nextInt(floors);

            // Bad exclusive random method, random complexity, but it works!
            while ((targetFloor = elevator.getBottomFloor() + rand.nextInt(floors)) != floor);

            // Using 0 weight for testing purposes
            eq.queue(new Person(0, floor, targetFloor));
        }
    }

    /**
     * Starts spawning passengers
     * 
     * @param minDelay Minimum delay
     * @param maxDelay Maximum delay
     * @param maxGroup Max group size
     */
    public void startSpawning(int minDelay, int maxDelay, int maxGroup) {
        Runnable run = () -> {
            this.keepSpawning = true;

            while (keepSpawning) {
                try {
                    Thread.sleep(minDelay + (maxDelay - minDelay + 1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }

                spawnGroup(maxGroup);
            }
        };

        new Thread(run, String.format("Elevator%sSpawner", elevator.getElevatorName())).start();
    }

    /**
     * Stops spawning passengers
     */
    public void stopSpawning() {
        this.keepSpawning = false;
    }

    /**
     * Elevator tester
     * 
     * @param elevator Elevator
     * @param eq Elevator queuer
     */
    public ElevatorSpawner(Elevator elevator, ElevatorQueuer eq) {
        this.elevator = elevator;
        this.eq = eq;
    }

}
