package elevator.display;

import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

/**
 * Display canvas
 *
 * @author Emil Bertilsson
 * @version 2015/10/15
 */
@SuppressWarnings("serial")
public class Canvas extends java.awt.Canvas {

    private boolean running;
    private int width;
    private int height;
    private int fps;
    private int targetFrequency;

    private GraphicsDevice gDevice;
    private GraphicsConfiguration gConfig;
    private DisplayMode dMode;
    private BufferStrategy bStrategy;
    private BufferedImage bImage;
    private Renderable surface;

    /**
     * Sets the surface object
     * 
     * @param surface
     */
    public void setSurface(Renderable surface) {
        this.surface = surface;
    }

    /**
     * Rendering
     *
     * @param g Graphics object
     */
    private void render(Graphics2D g) {
        g.setColor(Color.RED);
        g.drawString("FPS: " + fps, 10, 20);

        if (surface != null && g != null) {
            surface.render(g);
        }
    }

    /**
     * Initalizes graphics configuration
     */
    private void initGraphicsConfig() {
        this.gDevice = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        this.gConfig = gDevice.getDefaultConfiguration();
        this.dMode = gDevice.getDisplayMode();
    }

    /**
     * Initializes buffer
     */
    private void initBuffer() {
        createBufferStrategy(2);
        this.bStrategy = getBufferStrategy();
        this.bImage = gConfig.createCompatibleImage(width, height);
    }

    /**
     * Initializes rendering
     */
    private void initRender() {
        Graphics bStrategyGraphics = null;
        Graphics2D bImageGraphics = null;

        long curTime, lastTime, wait;

        while (running) {
            try {
                // Sets last time to pre-render phase
                lastTime = System.currentTimeMillis();

                // Clears the buffer
                bStrategyGraphics = bStrategy.getDrawGraphics();
                bImageGraphics = bImage.createGraphics();
                bImageGraphics.setBackground(Color.BLACK);
                bImageGraphics.clearRect(0, 0, width, height);

                // Renders and draws the buffer
                this.render(bImageGraphics);
                bStrategyGraphics.drawImage(bImage, 0, 0, null);

                // Blits the buffer
                if (!bStrategy.contentsLost()) {
                    bStrategy.show();
                }

                // FPS-counter and framerate adjuster
                // TODO: Implement a render thread scheduler instead
                curTime = System.currentTimeMillis();

                if (targetFrequency > 0) {
                    wait = targetFrequency - (curTime - lastTime);

                    if (wait > 0) {
                        try {
                            Thread.sleep(wait);
                        } catch (InterruptedException e) {
                        }
                    }
                }

                curTime = System.currentTimeMillis();

                fps = (int) (((curTime - lastTime) > 0)
                        ? 1000 / (curTime - lastTime)
                        : 0);

            } finally {
                if (bStrategyGraphics != null) {
                    bStrategyGraphics.dispose();
                }

                if (bImageGraphics != null) {
                    bImageGraphics.dispose();
                }
            }
        }
    }

    /**
     * Sets target update frequency
     *
     * @param ups Updates per second
     */
    private void setUpdateFrequency(int ups) {
        if (ups > 0) {
            this.targetFrequency = 1000 / ups;
        } else {
            this.targetFrequency = 0;
        }
    }

    /**
     * Uses the screen device's update frequency
     */
    public void useDeviceFrequency() {
        int freq = dMode.getRefreshRate();

        if (freq != DisplayMode.REFRESH_RATE_UNKNOWN) {
            setUpdateFrequency(freq);
        }
    }

    /**
     * Initializes the canvas
     */
    public void init() {
        // Canvas
        setSize(width, height);
        setFocusable(true);
        setIgnoreRepaint(true);

        // Graphics
        initGraphicsConfig();
        initBuffer();

        this.running = true;

        new Thread(() -> {
            Canvas.this.initRender();
        }, "Canvas").start();
    }

    /**
     * Creates a new canvas
     *
     * @param width Canvas width
     * @param height Canvas height
     * @param ups Updates per second
     */
    public Canvas(int width, int height, int ups) {
        super();

        this.width = width;
        this.height = height;

        setUpdateFrequency(ups);
    }

}
