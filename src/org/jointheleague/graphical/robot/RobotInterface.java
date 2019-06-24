package org.jointheleague.graphical.robot;

import java.awt.*;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;

public interface RobotInterface {

    /**
     * Changes the image of the Robot
     *
     * @param im a BufferedImage containing the robot image. It does not need
     *           to be to scale since it will be scaled to the appropriate
     *           size.
     */
	RobotInterface changeRobot(BufferedImage im);

    /**
     * Changes the image of the Robot
     *
     * @param urlName The URL of the image that specifies the Robot's image.
     */
    RobotInterface changeRobot(String urlName);

    int getPenWidth();

    /**
     * Sets the pen size.
     *
     * @param size the new pen size given as an integer between 1 and 10.
     */
    RobotInterface setPenWidth(int size);

    Color getPenColor();

    /**
     * Sets the pen color
     *
     * @param color the new pen color
     */
    RobotInterface setPenColor(Color color);

    /**
     * Sets the pen color given the red, green and blue components of the new
     * color. The components are specified as an integer between 0 and 255.
     *
     * @param r the red component of the new color
     * @param g the green component of the new color
     * @param b the blue component of the new color
     */
    RobotInterface setPenColor(int r, int g, int b);

    /**
     * Sets the pen color to a random color.
     */
    RobotInterface setRandomPenColor();

    /**
     * Removes all lines drawn by this Robot.
     */
    RobotInterface clearDrawables();

    /**
     * Makes the image of the Robot small
     */
    RobotInterface miniaturize();

    /**
     * Makes the image of the Robot big
     */
    RobotInterface expand();

    /**
     * Set the position of the robot to (x, y) while maintaining its direction.
     *
     * @param x the x-coordinate of the new position
     * @param y the y-coordinate of the new position
     */
    RobotInterface setPos(float x, float y);

    /**
     * Gets the robot's angle of orientation in degrees.
     * An angle of 0 means that the robot is pointing straight up and the angle
     * increases as the robot turns clockwise.
     *
     * @return the angle of the robot
     */
    double getAngle();

    /**
     * Set the robot's angle of orientation.
     *
     * @param a the angle in radians
     * @see #getAngle()
     */
    RobotInterface setAngle(double a);

    /**
     * Makes the Robot sparkle.
     */
    RobotInterface sparkle();

    /**
     * Removes sparkles
     */
    RobotInterface unSparkle();

    /**
     * Make the Robot invisible. The Robot is visible initially.
     */
    RobotInterface hide();

    /**
     * Makes the Robot visible
     */
    RobotInterface show();

    /**
     * Makes the robot move a given distance. A negative distance makes the
     * robot move backwards.
     *
     * @param distance the distance to move in units of points
     */
    RobotInterface move(int distance);

    /**
     * Makes the Robot move one step. If the sgn is negative, the Robot moves
     * one step backwards. The step size depends on the Robot's speed. This
     * method is intended to be used by a KeyboardAdapter.
     *
     * @param sgn if positive the Robot moves forward, if negative the Robot
     *            moves backwards.
     * @throws InterruptedException     if interrupted before making the turn (which is very unlikely
     *                                  to happen).
     * @throws IllegalArgumentException if sgn is 0.
     */
    RobotInterface microMove(int sgn) throws InterruptedException;

    /**
     * Makes the Robot turn in place a given number of degrees. If the argument
     * is positive, the Robot turn clockwise, if negative the Robot turns
     * counter-clockwise.
     *
     * @param degrees The number of degrees to turn.
     */
    RobotInterface turn(double degrees);

    /**
     * Turn the robot in place until it has reached an orientation given in degrees.
     * Zero is up, 90 is right.
     *
     * @param degrees the desired orientation.
     */
    RobotInterface turnTo(double degrees);

    /**
     * Makes the Robot turn in place a small angle. If the argument is positive,
     * the Robot turn clockwise, if negative the Robot turns counter-clockwise.
     * The angle turn is dependent on the Robot's speed.
     *
     * @param sgn The sign of the direction to turn.
     * @throws InterruptedException     if interrupted before making the turn (which is very unlikely
     *                                  to happen).
     * @throws IllegalArgumentException if the sgn is 0.
     */
    RobotInterface microTurn(int sgn) throws InterruptedException;

    /**
     * Waits a given number of milliseconds.
     *
     * @param millis the number of milliseconds to wait
     */
    RobotInterface sleep(int millis);

    /**
     * Places the robot at (x, y)
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @deprecated Use {@link #setPos(float x, float y)}
     */
    @Deprecated
    RobotInterface moveTo(float x, float y);

    /**
     * Move the Robot to a given position. The Robot does not draw any line
     * regardless of whether pen is up or down. Unless <code>jump</code> is true,
     * if necessary, turn the robot first such that it is heading in the right
     * direction before moving the robot.
     *
     * @param x        the x-coordinate of the new position
     * @param y        the y-coordinate of the new position
     * @param relative if true, x and y a relative to the robot's current position
     */
    RobotInterface moveTo(float x, float y, boolean relative);

    /**
     * Move the robot forward to a given position. If necessary, turn the robot
     * first such that it is heading in the right direction.
     *
     * @param x        the x-coordinate of the robot's destination
     * @param y        the y-coordinate of the robot's destination
     * @param relative if true, x and y are given relative to the robot's current
     *                 position
     */
    RobotInterface lineTo(float x, float y, boolean relative);

    /**
     * Move the robot along a quadratic curve given by the robot's current position
     * and the control points (x1, y1) and (x2, y2). If necessary, turn the robot
     * first such that it is heading in the right direction.
     *
     * @param x1       the x-coordinate of the first control point
     * @param y1       the y-coordinate of the first control point
     * @param x2       the x-coordinate of the second control point
     * @param y2       the y-coordinate of the second control point
     * @param relative if true, the coordinates are give relative to the robot's
     *                 current position
     */
    RobotInterface quadTo(float x1, float y1, float x2, float y2, boolean relative);

    /**
     * Move the robot along a cubic curve given by the robot's current position
     * and the control points (x1, y1), (x2, y2) and (x3, y3). If necessary, turn
     * the robot first such that it is heading in the right direction.
     *
     * @param x1       the x-coordinate of the first control point
     * @param y1       the y-coordinate of the first control point
     * @param x2       the x-coordinate of the second control point
     * @param y2       the y-coordinate of the second control point
     * @param x3       the x-coordinate of the third control point
     * @param y3       the y-coordinate of the third control point
     * @param relative if true, the coordinates are give relative to the robot's
     *                 current position
     */
    RobotInterface cubicTo(float x1, float y1, float x2, float y2, float x3, float y3, boolean relative);

    /**
     * Move the robot along a path.
     *
     * @param pathIterator a PathIterator specifying the path
     * @param fill         if true and the robot's pen is down, fill the path
     *                     with the current pen color, otherwise draw the path
     */
    RobotInterface followPath(PathIterator pathIterator, boolean fill);

    /**
     * Move the robot along a path and, if the pen is down, draw the path
     *
     * @param pathIterator a PathIterator specifying the path
     */
    RobotInterface followPath(PathIterator pathIterator);

    /**
     * @return the x-coordinate of the robot's position
     */
    float getX();

    /**
     * @return the y-coordinate of the robot's position
     */
    float getY();

    /**
     * Lifts the pen, i.e., the Robot stops drawing lines or curves.
     */
    RobotInterface penUp();

    /**
     * Set the pen down, i.e., the Robot traces its movements with lines or curves.
     */
    RobotInterface penDown();

    /**
     * Sets the speed of the Robot.
     *
     * @param speed the speed specified as a number between 1 and 100.
     */
    RobotInterface setSpeed(int speed);

    /**
     * Adds a {@link KeyboardAdapter} to the robot. It is possible to add more
     * than one KeyboardAdapter, each controlling a different Robot. If two
     * KeyboardAdapters controlling the same Robot are added, the last one added
     * <em>replaces</em> the first.
     *
     * @param adapter the KeyboardAdapter
     * @see KeyboardAdapter
     */
    RobotInterface addKeyboardAdapter(KeyboardAdapter adapter);
}
