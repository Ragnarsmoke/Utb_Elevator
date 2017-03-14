package elevator.elevator;

public class Person {

    private int floor;
    private int targetFloor;
    private int weight;

    /**
     * Gets the person's current floor
     * 
     * @return Current floor
     */
    public int getFloor() {
        return floor;
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
     * Sets the person's current floor
     *
     * @param floor Floor
     */
    public void setFloor(int floor) {
        this.floor = floor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + targetFloor;
        result = prime * result + weight;
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Person other = (Person) obj;
        if (targetFloor != other.targetFloor)
            return false;
        if (weight != other.weight)
            return false;
        return true;
    }

    /**
     * Person
     *
     * @param name Person name
     * @param weight Weight
     * @param floor Current floor
     * @param targetFloor Target floor
     */
    public Person(int weight, int floor, int targetFloor) {
        this.weight = weight;
        this.floor = floor;
        this.targetFloor = targetFloor;
    }

}
