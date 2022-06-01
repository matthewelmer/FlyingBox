/** 
 * @author matt elmer
*/

/**
 * NOTES:
 * - currently this is just a falling box.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.concurrent.TimeUnit;


/**
 * A panel that draws a flying box.
 */
public class FlyingBoxPanel extends JPanel {
    double gravity = 9.81;  // m/s^2
    double timestep = 0.01;  // seconds

    private int width = 1280;  // pixels
    private int height = 720;  // pixels

    private int boxSideLength = 50;
    private double[] boxCenter = {width/2, height/2};
    private double[] boxVelocity = {0.0, 0.0};
    private double[] boxAccel = {0.0, -gravity};
    private double[][] boxVertices = {
        {(int)boxCenter[0]-boxSideLength/2, (int)boxCenter[1]-boxSideLength/2},
        {(int)boxCenter[0]+boxSideLength/2, (int)boxCenter[1]-boxSideLength/2},
        {(int)boxCenter[0]+boxSideLength/2, (int)boxCenter[1]+boxSideLength/2},
        {(int)boxCenter[0]-boxSideLength/2, (int)boxCenter[1]+boxSideLength/2}
    };

    private void updateVertices() {
        boxVertices[0][0] = boxCenter[0] - boxSideLength/2;
        boxVertices[0][1] = boxCenter[1] - boxSideLength/2;
        boxVertices[1][0] = boxCenter[0] + boxSideLength/2;
        boxVertices[1][1] = boxCenter[1] - boxSideLength/2;
        boxVertices[2][0] = boxCenter[0] + boxSideLength/2;
        boxVertices[2][1] = boxCenter[1] + boxSideLength/2;
        boxVertices[3][0] = boxCenter[0] - boxSideLength/2;
        boxVertices[3][1] = boxCenter[1] + boxSideLength/2;
    }

    public void takeStep() {
        boxCenter[0] += boxVelocity[0] * timestep;
        boxCenter[1] -= boxVelocity[1] * timestep;
        updateVertices();

        boxVelocity[0] += boxAccel[0] * timestep;
        boxVelocity[1] += boxAccel[1] * timestep;

        repaint();
        try {
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    /**
     * Called by runtime system whenever panel needs painting.
     * Override it to draw the flying box!
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // do the below if you care about resizing
        // width = getWidth();
        // height = getHeight();

        // boxSideLength = Math.min(width/2, height/2);
        // do the above if you care about resizing

        // just paint dat boi
        g.setColor(Color.RED);
        for (int i=0; i<boxVertices.length; i++) {
            g.drawLine((int)boxVertices[i][0], (int)boxVertices[i][1], (int)boxVertices[(i+1)%boxVertices.length][0], (int)boxVertices[(i+1)%boxVertices.length][1]);
        }
    }

    public static void main(String[] args) {
        FlyingBoxPanel panel = new FlyingBoxPanel();
        panel.setBackground(Color.BLACK);
        JFrame frame = new JFrame("Flying Box");
        frame.setSize(panel.width, panel.height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
        while (true) {
            panel.takeStep();
        }
    }
}
