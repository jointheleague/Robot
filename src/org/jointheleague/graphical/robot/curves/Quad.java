package org.jointheleague.graphical.robot.curves;

import org.jointheleague.graphical.robot.Robot;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;

final public class Quad implements Drawable, Segment {

    private final float startX;
    private final float startY;
    private final float[] ctrlPoints;
    private final int lineSize;
    private final Color color;
    private final Line line;

    /**
     * Constructor
     *
     * @param x          x-coordinate of the segment's starting point
     * @param y          y-coordinate of the segment's starting point
     * @param ctrlPoints an array of length at least 4 that
     *                   contains the x- and y-coordinates of the segment's control points.
     * @param lineSize   the line width used to draw the segment
     * @param color      the color used to draw the segment
     */
    public Quad(float x, float y, float[] ctrlPoints, int lineSize, Color color) {
        this.startX = x;
        this.startY = y;
        this.ctrlPoints = ctrlPoints;
        this.lineSize = lineSize;
        this.color = color;
        this.line = new Line(x, y, ctrlPoints, 0, null);
    }

    @Override
    public void draw(Graphics2D g2) {
        g2.setStroke(new BasicStroke(lineSize));
        g2.setColor(color);
        QuadCurve2D quad = new QuadCurve2D.Float(
                startX, startY, ctrlPoints[0], ctrlPoints[1], ctrlPoints[2], ctrlPoints[3]);
        g2.draw(quad);
    }

    @Override
    public Segment subSegment(float t) {
        Robot.Pos pos1 = line.getPos(t);
        Robot.Pos pos2 = getPos(t);

        float[] ctrlPoints = {
                pos1.getX(), pos1.getY(),
                pos2.getX(), pos2.getY()};

        return new Quad(startX, startY, ctrlPoints, lineSize, color);
    }

    @Override
    public Robot.Pos getPos(float t) {
        if (t <= 0F) return new Robot.Pos(startX, startY);
        if (1F <= t) return new Robot.Pos(ctrlPoints[2], ctrlPoints[3]);
        float u = 1F - t;
        float c0 = u * u;
        float c1 = 2 * u * t;
        float c2 = t * t;
        return new Robot.Pos(
                c0 * startX + c1 * ctrlPoints[0] + c2 * ctrlPoints[2],
                c0 * startY + c1 * ctrlPoints[1] + c2 * ctrlPoints[3]);

    }

    @Override
    public double getStartAngle() {
        if (ctrlPoints[0] == startX && ctrlPoints[1] == startY) return Double.NaN;
        return Math.atan2(ctrlPoints[0] - startX, -ctrlPoints[1] + startY);
    }

    @Override
    public double getEndAngle() {
        if (ctrlPoints[2] == ctrlPoints[0] && ctrlPoints[3] == ctrlPoints[1]) return Double.NaN;
        return Math.atan2(ctrlPoints[2] - ctrlPoints[0], -ctrlPoints[3] + ctrlPoints[1]);
    }

    @Override
    public double getAngle(float time) {
        if (time <= 0F) return getStartAngle();
        if (1F <= time) return getEndAngle();
        return subSegment(time).getEndAngle();
    }

    @Override
    public float getSize() {
        double a = ctrlPoints[0] - startX;
        double b = ctrlPoints[1] - startY;
        double c = ctrlPoints[2] - ctrlPoints[0];
        double d = ctrlPoints[3] - ctrlPoints[1];
        return (float) (Math.sqrt(a * a + b * b) + Math.sqrt(c * c + d * d));
    }

    @Override
    public Path2D addTo(Path2D path2D) {
        path2D.quadTo(ctrlPoints[0], ctrlPoints[1], ctrlPoints[2], ctrlPoints[3]);
        return path2D;
    }

    @Override
    public Path2D addTo(Path2D path2D, float t) {
        Robot.Pos pos1 = line.getPos(t);
        Robot.Pos pos2 = getPos(t);
        path2D.quadTo(pos1.getX(), pos1.getY(), pos2.getX(), pos2.getY());
        return path2D;
    }
}
