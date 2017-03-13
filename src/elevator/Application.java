package elevator;

import java.util.Arrays;

public class Application {

    public static void main(String[] args) {
        Elevator e = new Elevator(6, "1");
        e.request(Arrays.asList(4, 2, 3, 8, 7, 5));
        e.addPassenger(new Person(
                "John Doe",
                4,
                80));
        e.prioritize();
        e.run();
    }

}
