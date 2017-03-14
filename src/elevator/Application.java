package elevator;

import elevator.elevator.Elevator;

public class Application {

    public static void main(String[] args) {
        Application app = new Application();
        app.run();
    }

    public void run() {
        Elevator elevator = new Elevator(1, "MAIN");
        elevator.setMoveDelay(250);
        elevator.setFloorRange(1, 10);
        elevator.start();
    }

}
