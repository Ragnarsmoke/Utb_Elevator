package elevator.display;

@FunctionalInterface
public interface Renderable {

    /**
     * Renders the renderable
     * 
     * @param g Graphics object
     */
    public void render(java.awt.Graphics2D g);

}
