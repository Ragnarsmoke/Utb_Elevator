package elevator;

import java.util.Arrays;

public class Application {

    public static void main(String[] args) {
        Elevator e = new Elevator(6, "1");
        e.queue(Arrays.asList(4, 2, 3, 8, 7, 5));
        e.prioritize();
        e.run();
    }

}
