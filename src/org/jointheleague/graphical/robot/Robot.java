package org.jointheleague.graphical.robot;

import org.jointheleague.graphical.robot.curves.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * This class is used to show a robot inside a window. If no RobotWindow exists
 * when instantiating a Robot, a window is created and the Robot placed inside
 * the window. If a RobotWindow already exists, the new Robot is placed inside
 * the existing RobotWindow.
 * </p>
 * <p>
 * A Robot is controlled by calling its {@link #move(int)}, {@link #turn(double)},
 * {@link #microMove(int)}, and {@link #microTurn(int)} methods. These methods
 * should be called from the same thread, which is typically the main thread,
 * but different Robots may be controlled from different threads, thereby
 * allowing the Robots to move simultaneously.
 * </p>
 * <p>
 * A Robot also has state, e.g., visible or hidden, pen size, pen up or down,
 * etc. This state may be modified by calling the respective setter methods.
 * </p>
 *
 * @author David Dunn &amp; Erik Colban &copy; 2016
 */
public class Robot implements RobotInterface {

    static final int TICK_LENGTH = 20; // in milliseconds
    private static final int MAXI_IMAGE_SIZE = 100;
    private static final int MINI_IMAGE_SIZE = 25;
    private static final int MIN_SPEED = 1;
    private static final int MAX_SPEED = 100;

    // Robot state start
    private int speed;
    private boolean penDown;
    private int penWidth;
    private Color penColor;
    private Pos pos;
    private double angle;
    private boolean isVisible;
    private boolean isSparkling;
    private ArrayList<Drawable> drawables;
    private Drawable currentDrawable;
    private boolean isMini;
    private Image maxiImage;
    private Image miniImage;
    private Image image;
    // Robot state end

    private RobotWindow window;
    private BlockingQueue<TimeQuantum> leakyBucket = new ArrayBlockingQueue<>(1);
    private DynamicPath currentPath;

    public Robot() {
        this("rob");
    }

    /**
     * Instantiates a new default Robot at the position provided.
     *
     * @param xPos the x-coordinate of the Robot's center
     * @param yPos the y-coordinate of the Robot's center
     */
    public Robot(int xPos, int yPos) {
        this("rob", xPos, yPos);
    }

    /**
     * Instantiates a new Robot whose image is specified by the filename at the
     * center of the RobotWindow.
     *
     * @param fileName The file name without extension of a file in robi format that
     *                 specifies the Robot's image.
     */
    public Robot(String fileName) {
        this(RobotImage.loadRobi(fileName));
    }

    /**
     * Instantiates a new Robot at the center of the RobotWindow.
     *
     * @param robotImage a BufferedImage containing the robot image. It does not need
     *                   to be to scale since it will be scaled to the appropriate
     *                   size.
     */
    public Robot(BufferedImage robotImage) {
        this(robotImage, 0, 0);
        Dimension dimension = window.getSize();
        setPos(dimension.width / 2F, dimension.height / 2F);
    }

    /**
     * @param fileName the name of the file containing the Robot image, without the
     *                 ".robi" extension.
     * @param xPos     the initial x-coordinate of the robot
     * @param yPos     the initial y-coordinate of the robot
     */
    public Robot(String fileName, int xPos, int yPos) {
        this(RobotImage.loadRobi(fileName), xPos, yPos);
    }

    /**
     * @param inputImage a BufferedImage containing the robot image. It does not need
     *                   to be to scale since it will be scaled to the appropriate
     *                   size.
     * @param xPos       the initial x-coordinate of the robot
     * @param yPos       the initial y-coordinate of the robot
     */
    public Robot(BufferedImage inputImage, int xPos, int yPos) {
        angle = 0;
        speed = 1;
        this.pos = new Pos(xPos, yPos);
        penWidth = 1;
        penColor = Color.BLACK;

        isVisible = true;
        penDown = false;
        isSparkling = false;

        maxiImage = inputImage.getScaledInstance(MAXI_IMAGE_SIZE, MAXI_IMAGE_SIZE, Image.SCALE_SMOOTH);
        miniImage = inputImage.getScaledInstance(MINI_IMAGE_SIZE, MINI_IMAGE_SIZE, Image.SCALE_SMOOTH);
        image = maxiImage;
        isMini = false;

        drawables = new ArrayList<>();
        window = RobotWindow.getInstance();
        window.addRobot(this);
    }

    /**
     * Sets the window's background color
     *
     * @param color the new window background color.
     */
    public static void setWindowColor(final Color color) {
        SwingUtilities.invokeLater(() -> RobotWindow.getInstance().setWinColor(color));
    }

    /**
     * Sets the window's background image
     *
     * @param imageLocation the new window background image location.
     */
    public static void setWindowImage(final String imageLocation) {
        SwingUtilities.invokeLater(() -> RobotWindow.getInstance().setBackgroundImage(imageLocation));
    }

    /**
     * Sets the window size
     *
     * @param width  the width of the window
     * @param height the height of the window
     */
    public static void setWindowSize(int width, int height) {
        SwingUtilities.invokeLater(() -> RobotWindow.getInstance().setWindowSize(width, height));
    }

    /**
     * Sets the window's background color given the red, green and blue
     * components of the new color. The components are specified as an integer
     * between 0 and 255.
     *
     * @param r the red component of the new color
     * @param g the green component of the new color
     * @param b the blue component of the new color
     */
    public static void setWindowColor(int r, int g, int b) {
        r = Math.min(Math.max(0, r), 255);
        g = Math.min(Math.max(0, g), 255);
        b = Math.min(Math.max(0, b), 255);

        Robot.setWindowColor(new Color(r, g, b));
    }

    /**
     * Draws the Robot
     *
     * @param g2 The graphics object used to draw the Robot.
     */
    Robot draw(Graphics2D g2) {
        for (Drawable drawable : getDrawables()) {
            drawable.draw(g2);
        }
        // draws under robot
        if (isPenDown()) {
            Drawable drawable = getCurrentDrawable();
            if (drawable != null) drawable.draw(g2);
        }

        // first cache the standard coordinate system
        AffineTransform cached = g2.getTransform();
        // align the coordinate system with the center of the robot:
        g2.translate(pos.x, pos.y);
        g2.rotate(Math.toRadians(getAngle()));

        if (isVisible()) {
            int offset = -(isMini() ? MINI_IMAGE_SIZE : MAXI_IMAGE_SIZE) / 2;
            g2.drawImage(image, offset, offset, null);
        }

        if (penDown && isVisible) // draws over robot
        {
            g2.setColor(Color.RED);
            if (isMini()) {
                g2.fillOval(-2, -2, 4, 4);
            } else {
                g2.fillOval(-4, -4, 8, 8);
            }
        }

        if (isVisible() && isSparkling()) {
            if (isMini()) {
                double s = (double) MINI_IMAGE_SIZE / MAXI_IMAGE_SIZE;
                g2.scale(s, s);
            }
            Random r = new Random();
            int xDot = r.nextInt(MAXI_IMAGE_SIZE - 4) - MAXI_IMAGE_SIZE / 2;
            int yDot = r.nextInt(MAXI_IMAGE_SIZE - 4) - MAXI_IMAGE_SIZE / 2;
            g2.setColor(Color.WHITE);
            g2.fillRect(xDot, yDot, 5, 5);
        }
        g2.setTransform(cached); // restore the standard coordinate system
        return this;
    }

    private synchronized boolean isMini() {
        return isMini;
    }

    @Override
    public synchronized Robot changeRobot(BufferedImage im) {
        Image imMax = im.getScaledInstance(MAXI_IMAGE_SIZE, MAXI_IMAGE_SIZE, Image.SCALE_SMOOTH);
        Image imMin = im.getScaledInstance(MINI_IMAGE_SIZE, MINI_IMAGE_SIZE, Image.SCALE_SMOOTH);
        synchronized (this) {
            maxiImage = imMax;
            miniImage = imMin;
            image = isMini ? miniImage : maxiImage;
        }
        return this;
    }

    @Override
    public synchronized Robot changeRobot(String urlName) {
        BufferedImage newImage;
        try {
            URL url = new URL(urlName);
            newImage = ImageIO.read(url);
        } catch (IOException e) {
            System.err.println("There was an error changing robot's image. Make sure the URL addresses an image.");
            e.printStackTrace();
            newImage = (BufferedImage) image;
        }
        changeRobot(newImage);
        return this;
    }

    @Override
    public int getPenWidth() {
        return penWidth;
    }

    @Override
    public synchronized Robot setPenWidth(int size) {
        penWidth = Math.min(Math.max(1, size), 10);
        return this;
    }

    @Override
    public synchronized Color getPenColor() {
        return penColor;
    }

    @Override
    public synchronized Robot setPenColor(Color color) {
        penColor = color;
        return this;
    }

    @Override
    public Robot setPenColor(int r, int g, int b) {
        r = Math.min(Math.max(0, r), 255);
        g = Math.min(Math.max(0, g), 255);
        b = Math.min(Math.max(0, b), 255);

        penColor = new Color(r, g, b);
        return this;
    }

    @Override
    public Robot setRandomPenColor() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int r = rng.nextInt(256);
        int b = rng.nextInt(256);
        int g = rng.nextInt(256);
        penColor = new Color(r, g, b);
        return this;
    }

    private synchronized Robot addDrawable(final Drawable segment) {
        drawables.add(segment);
        return this;
    }

    @Override
    public synchronized Robot clearDrawables() {
        drawables.clear();
        return this;
    }

    private synchronized List<Drawable> getDrawables() {
        return new ArrayList<>(drawables);
    }

    private synchronized Drawable getCurrentDrawable() {
        return currentDrawable;
    }

    private synchronized Robot setCurrentDrawable(Drawable drawable) {
        this.currentDrawable = drawable;
        return this;
    }

    @Override
    public synchronized Robot miniaturize() {
        image = miniImage;
        isMini = true;
        return this;
    }

    @Override
    public synchronized Robot expand() {
        image = maxiImage;
        isMini = false;
        return this;
    }

    @Override
    public synchronized Robot setPos(float x, float y) {
        pos = new Robot.Pos(x, y);
        return this;
    }

    @Override
    public synchronized double getAngle() {
        return angle;
    }

    @Override
    public synchronized Robot setAngle(double a) {
        angle = (a + 180.0) % 360.0 - 180.0;
        if (angle < -180) angle += 360;
        return this;
    }

    private synchronized Robot incrementAngle(int delta) {
        setAngle(angle + delta);
        return this;
    }

    private synchronized boolean isSparkling() {
        return isSparkling;
    }

    @Override
    public synchronized Robot sparkle() {
        isSparkling = true;
        return this;
    }

    @Override
    public synchronized Robot unSparkle() {
        isSparkling = false;
        return this;
    }

    private synchronized boolean isVisible() {
        return isVisible;
    }

    @Override
    public synchronized Robot hide() {
        isVisible = false;
        return this;
    }

    @Override
    public synchronized Robot show() {
        isVisible = true;
        return this;
    }

    @Override
    public Robot move(int distance) {
        float[] ctrlPoints = new float[2];

        final double rAngle = Math.toRadians(getAngle());
        ctrlPoints[0] = (float) (getX() + distance * Math.sin(rAngle));
        ctrlPoints[1] = (float) (getY() - distance * Math.cos(rAngle));
        segmentTo(new Line(getX(), getY(), ctrlPoints, getPenWidth(), getPenColor()), distance >= 0);
        return this;
    }

    @Override
    public Robot microMove(int sgn) throws InterruptedException {
        if (sgn == 0) {
            throw new IllegalArgumentException("The argument sgn must be non-zero.");
        }
        final float distance = (sgn < 0 ? -1 : 1) * speed;
        final double rAngle = Math.toRadians(getAngle());
        final float startX = getX();
        final float startY = getY();
        final float endX = (float) (getX() + distance * Math.sin(rAngle));
        final float endY = (float) (getY() - distance * Math.cos(rAngle));
        leakyBucket.take();
        pos = new Pos(endX, endY);
        if (isPenDown()) {
            final float[] ctrlPoints = new float[]{endX, endY};
            synchronized (this) {
                addDrawable(new Line(startX, startY, ctrlPoints, getPenWidth(), getPenColor()));
            }
        }
        return this;
    }

    @Override
    public Robot turn(double degrees) {
        double degreesTurned = 0;
        int sgn = degrees < 0 ? -1 : 1;

        double angle0 = getAngle();
        try {
            while (sgn * (degreesTurned - degrees) < 0) {
                leakyBucket.take(); // will block until a TimeQuatum.TICK becomes available
                degreesTurned += sgn * speed;
                if (sgn * (degreesTurned - degrees) > 0) {
                    degreesTurned = degrees;
                }
                setAngle(angle0 + degreesTurned);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public Robot turnTo(double degrees) {
        turn(getAngleToTurn(degrees));
        return this;
    }

    @Override
    public Robot microTurn(int sgn) throws InterruptedException {
        if (sgn == 0) {
            throw new IllegalArgumentException("sgn must be non-zero.");
        }
        leakyBucket.take();
        incrementAngle(sgn * speed);
        return this;
    }

    Robot doNothing() {
        try {
            leakyBucket.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public Robot sleep(int millis) {
        try {
            int numTicks = millis / TICK_LENGTH;
            for (int i = 0; i < numTicks; i++) {
                leakyBucket.take();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    @Deprecated
    public synchronized Robot moveTo(float x, float y) {
        pos = new Pos(x, y);
        return this;
    }

    @Override
    public Robot moveTo(float x, float y, boolean relative) {
        float[] ctrlPoints = new float[2];
        ctrlPoints[0] = relative ? getX() + x : x;
        ctrlPoints[1] = relative ? getY() + y : y;
        segmentTo(new Move(getX(), getY(), ctrlPoints), true);
        return this;
    }

    @Override
    public Robot lineTo(final float x, final float y, final boolean relative) {
        float[] ctrlPoints = new float[2];
        ctrlPoints[0] = relative ? getX() + x : x;
        ctrlPoints[1] = relative ? getY() + y : y;
        segmentTo(new Line(getX(), getY(), ctrlPoints, getPenWidth(), getPenColor()), true);
        return this;
    }

    @Override
    public Robot quadTo(float x1, float y1, float x2, float y2, boolean relative) {
        float[] ctrlPoints = new float[4];
        ctrlPoints[0] = relative ? getX() + x1 : x1;
        ctrlPoints[1] = relative ? getY() + y1 : y1;
        ctrlPoints[2] = relative ? getX() + x2 : x2;
        ctrlPoints[3] = relative ? getY() + y2 : y2;
        segmentTo(new Quad(getX(), getY(), ctrlPoints, getPenWidth(), getPenColor()), true);
        return this;
    }

    @Override
    public Robot cubicTo(float x1, float y1, float x2, float y2, float x3, float y3, boolean relative) {
        float[] ctrlPoints = new float[6];
        ctrlPoints[0] = relative ? getX() + x1 : x1;
        ctrlPoints[1] = relative ? getY() + y1 : y1;
        ctrlPoints[2] = relative ? getX() + x2 : x2;
        ctrlPoints[3] = relative ? getY() + y2 : y2;
        ctrlPoints[4] = relative ? getX() + x3 : x3;
        ctrlPoints[5] = relative ? getY() + y3 : y3;
        segmentTo(new Cubic(getX(), getY(), ctrlPoints, getPenWidth(), getPenColor()), true);
        return this;
    }

    private Robot segmentTo(Segment segment, boolean forwards) {
        final double directionAdjustment = forwards ? 0.0 : Math.PI;
        double startAngle = segment.getStartAngle();
        if (!Double.isNaN(startAngle)) turnTo(startAngle + directionAdjustment);

        final float deltaT = speed / segment.getSize();
        float t = 0.0F;
        try {
            while (t < 1.0F) {
                leakyBucket.take();
                t += deltaT;
                Segment subSegment = segment.subSegment(t);
                pos = subSegment.getPos(1F);
                double endAngle = subSegment.getEndAngle();
                if (!Double.isNaN(endAngle)) setAngle(Math.toDegrees(endAngle + directionAdjustment));
                if (isPenDown() && (subSegment instanceof Drawable)) {
                    setCurrentDrawable((Drawable) subSegment);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (this) {
            if (currentDrawable != null) {
                addDrawable(currentDrawable);
                setCurrentDrawable(null);
            }
        }
        return this;
    }

    @Override
    public Robot followPath(PathIterator pathIterator, boolean fill) {
        DynamicPath dynamicPath = new DynamicPath(pathIterator, getPenWidth(), getPenColor(), this, fill);
        if (isPenDown()) currentDrawable = dynamicPath;
        try {
            while (!dynamicPath.isComplete()) {
                leakyBucket.take();
                dynamicPath.incrementTime(speed);
            }
        } catch (InterruptedException ignore) {
        }
        synchronized (this) {
            if (currentDrawable != null) {
                addDrawable(currentDrawable);
                setCurrentDrawable(null);
            }
        }
        return this;
    }

    @Override
    public Robot followPath(PathIterator pathIterator) {
        followPath(pathIterator, false);
        return this;
    }

    private double getAngleToTurn(final double targetAngle) {
        final double angle = Math.toDegrees(targetAngle - Math.toRadians(getAngle()));
        if (angle > 180.0) return (angle + 180.0) % 360.0 - 180.0;
        if (angle < -180.0) return (angle - 180.0) % 360.0 + 180.0;
        return angle;
    }

    @Override
    public synchronized float getX() {
        return pos.x;
    }

    @Override
    public synchronized float getY() {
        return pos.y;
    }

    private synchronized boolean isPenDown() {
        return penDown;
    }

    @Override
    public synchronized Robot penUp() {
        penDown = false;
        return this;
    }

    @Override
    public synchronized Robot penDown() {
        penDown = true;
        return this;
    }

    @Override
    public synchronized Robot setSpeed(int speed) {
        this.speed = Math.min(Math.max(MIN_SPEED, speed), MAX_SPEED);
        return this;
    }

    @Override
    public Robot addKeyboardAdapter(final KeyboardAdapter adapter) {
        SwingUtilities.invokeLater(() -> {
            RobotWindow window = RobotWindow.getInstance();
            KeyListener[] listeners = window.getKeyListeners();
            for (KeyListener listener : listeners) {
                if (listener instanceof KeyboardAdapter) {
                    KeyboardAdapter a = (KeyboardAdapter) listener;
                    if (a.robot == adapter.robot) {
                        window.removeKeyListener(a);
                        break;
                    }
                }
            }
            adapter.setRobot(Robot.this);
            window.addKeyListener(adapter);
        });
        return this;
    }

    private enum TimeQuantum {
        TICK
    }

    ActionListener getTickerListener() {
        return e -> {
            leakyBucket.offer(TimeQuantum.TICK);
            window.repaint();
        };
    }

    public static class Pos {
        private final float x;
        private final float y;

        public Pos(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }
}
