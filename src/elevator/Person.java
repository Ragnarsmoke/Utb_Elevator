package elevator;

public class Person {

    private String name;
    private int targetFloor;
    private int weight;

    /**
     * Gets the person's name
     * 
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the person's target floor
     * 
     * @return Target floor
     */
    public int getTargetFloor() {
        return targetFloor;
    }

    /**
     * Gets the person's weight
     *
     * @return Weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Person
     *
     * @param name Person name
     * @param targetFloor Target floor
     * @param weight Weight
     */
    public Person(String name, int targetFloor, int weight) {
        this.name = name;
        this.targetFloor = targetFloor;
        this.weight = weight;
    }

}
