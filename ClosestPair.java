import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;

class Point {
    double x;
    double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
}


class BoundingBox {
    public double minX, minY, maxX, maxY;

    public BoundingBox(List<Point> points) {
        if (points.isEmpty()) {
            // Handle empty list
            minX = minY = maxX = maxY = 0.0;
            return;
        }

        // Initialize min and max coordinates with the first point
        minX = maxX = points.get(0).x;
        minY = maxY = points.get(0).y;

        // Update min and max coordinates based on other points
        for (int i = 1; i < points.size(); i++) {
            Point point = points.get(i);
            if (point.x < minX) minX = point.x;
            if (point.x > maxX) maxX = point.x;
            if (point.y < minY) minY = point.y;
            if (point.y > maxY) maxY = point.y;
        }
    }
}

class DrawingPanel extends JPanel {
    private List<Point> points;
    public double x1, y1, x2, y2;
    private double minX, minY, maxX, maxY;
    private Point[] closestpair;

    public DrawingPanel(List<Point> points, BoundingBox boundingbox, Point[] closestpair) {
        this.points = points;
        this.closestpair = closestpair;

        this.minX = boundingbox.minX;
        this.minY = boundingbox.minY;
        this.maxX = boundingbox.maxX;
        this.maxY = boundingbox.maxY;
        setPreferredSize(new Dimension(600, 600));
    }

    public void drawPoint(Graphics2D g2d, Point point, Color color) {
        double pointsWidth = maxX - minX;
        double pointsHeight = maxY - minY;
	int px = (int) ((point.x - minX) / pointsWidth * getWidth());
	int py = (int) ((maxY - point.y) / pointsHeight * getHeight());
	//int py = (int) ((point.y - minY) / pointsHeight * getHeight());
	g2d.setColor(color);
	g2d.fillOval(px - 3, py - 3, 6, 6);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw points
        for (Point point : points) {
	    drawPoint(g2d, point, Color.BLACK);
        }
	drawPoint(g2d, closestpair[0], Color.RED);
	drawPoint(g2d, closestpair[1], Color.RED);

        float thickness = 3;
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(thickness));

        // Draw bounding box rectangle
        g2d.setColor(Color.BLUE);
        g2d.drawRect(1, 1, (int) getWidth()-2, getHeight()-2);

        //g2d.setColor(Color.GREEN);
        //g2d.drawRect(3, getHeight()-3-100, 100, 100);

        g2d.setStroke(oldStroke);
    }
}

public class ClosestPair {
    public static List<Point> readPointsFromFile(String fileName) throws IOException {
        List<Point> points = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = br.readLine()) != null) {
            String[] coordinates = line.split("\\s+");
            double x = Double.parseDouble(coordinates[0]);
            double y = Double.parseDouble(coordinates[1]);
            points.add(new Point(x, y));
        }
        br.close();
        return points;
    }

    public static Point[] findClosestPair(List<Point> points) {
        Point[] sortedX = points.toArray(new Point[0]);
        Arrays.sort(sortedX, Comparator.comparingDouble(p -> p.x));
        return closestPair(sortedX, 0, sortedX.length - 1);
    }

    public static Point[] closestPair(Point[] points, int low, int high) {

        if (high - low <= 3) {
            return bruteForceClosestPair(points, low, high);
        }
        int mid = (low + high) / 2;
        Point[] leftPair = closestPair(points, low, mid);
        Point[] rightPair = closestPair(points, mid + 1, high);
        double leftDistance = calculateDistance(leftPair[0], leftPair[1]);
        double rightDistance = calculateDistance(rightPair[0], rightPair[1]);
        double minDistance = Math.min(leftDistance, rightDistance);
        Point[] closestSplitPair = closestSplitPair(points, low, high, points[mid].x, minDistance);
        if (closestSplitPair == null) {
            if (leftDistance < rightDistance) {
                return leftPair;
            } else {
                return rightPair;
            }
        } else {
            return calculateDistance(closestSplitPair[0], closestSplitPair[1]) < minDistance ? closestSplitPair :
                    (leftDistance < rightDistance ? leftPair : rightPair);
        }
    }

    public static Point[] bruteForceClosestPair(Point[] points, int low, int high) {
        double minDistance = Double.POSITIVE_INFINITY;
        Point[] closestPair = new Point[2];
        for (int i = low; i <= high; i++) {
            for (int j = i + 1; j <= high; j++) {
                double distance = calculateDistance(points[i], points[j]);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestPair[0] = points[i];
                    closestPair[1] = points[j];
                }
            }
        }
        return closestPair;
    }

    public static Point[] closestSplitPair(Point[] points, int low, int high, double midX, double minDistance) {
        Point[] strip = new Point[high - low + 1];
        int j = 0;
        for (int i = low; i <= high; i++) {
            if (Math.abs(points[i].x - midX) < minDistance) {
                strip[j++] = points[i];
            }
        }
        Arrays.sort(strip, 0, j, Comparator.comparingDouble(p -> p.y));
        double min = minDistance;
        Point[] closestPair = new Point[2];
        for (int i = 0; i < j; i++) {
            for (int k = i + 1; k < j && (strip[k].y - strip[i].y) < min; k++) {
                double distance = calculateDistance(strip[i], strip[k]);
                if (distance < min) {
                    min = distance;
                    closestPair[0] = strip[i];
                    closestPair[1] = strip[k];
                }
            }
        }
        return closestPair[0] == null ? null : closestPair;
    }

    public static double calculateDistance(Point p1, Point p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static void main(String[] args) {
        try {
            List<Point> points = readPointsFromFile("points.txt");
            Point[] closestPair = findClosestPair(points);
            System.out.println("Closest pair:");
            System.out.println("Point 1: (" + closestPair[0].x + ", " + closestPair[0].y + ")");
            System.out.println("Point 2: (" + closestPair[1].x + ", " + closestPair[1].y + ")");
            double distance = calculateDistance(closestPair[0], closestPair[1]);
            System.out.println("Distance: " + distance);

            // bounding box:
            BoundingBox boundingbox = new BoundingBox(points);

            // Create Swing window
            JFrame frame = new JFrame("Closest Pair");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 600);

            // Create drawing panel
            DrawingPanel drawingPanel = new DrawingPanel(points, boundingbox, closestPair);

            // Add drawing panel to the frame
            frame.add(drawingPanel);

            // Show the frame
            frame.setVisible(true);

//            List<Point> result = rangeTree.queryRange(x1, y1, x2, y2);
//            System.out.println("Points within the range [" + x1 + ", " + y1 + "] to [" + x2 + ", " + y2 + "]:");
//            for (Point point : result) {
//                System.out.println("(" + point.x + ", " + point.y + ")");
//            }

        } catch (IOException e) {
            System.err.println("Error reading points from file: " + e.getMessage());
        }
    }
}
