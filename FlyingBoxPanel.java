/** 
 * @author matt elmer
*/
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.util.concurrent.TimeUnit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


/**
 * A panel that draws a flying box.
 */
public class FlyingBoxPanel extends JPanel {
    private double gravity = 9.81;  // m/s^2
    private double timestep = 0.03;  // seconds

    private int width = 1280;  // pixels
    private int height = 720;  // pixels

    private int boxSideLength = 50;
    private double[] boxCenter = {width/2, height/5};
    private double[] boxVelocity = {0.0, 0.0};
    private double[] boxAccel = {0.0, -gravity};
    private double[][] boxVertices = {
        {(int)boxCenter[0]-boxSideLength/2, (int)boxCenter[1]-boxSideLength/2},  // top left
        {(int)boxCenter[0]+boxSideLength/2, (int)boxCenter[1]-boxSideLength/2},  // top right
        {(int)boxCenter[0]+boxSideLength/2, (int)boxCenter[1]+boxSideLength/2},  // bottom right
        {(int)boxCenter[0]-boxSideLength/2, (int)boxCenter[1]+boxSideLength/2}   // bottom left
    };

    private double thrusterAccel = 0.0;
    private double thrusterMaxAccel = 18.0;  // m/s^2
    private int setpoint = height/4;  // pixels
    private double relError;

    private BufferedImage thrusterJet;
    private int thrusterJetWidth;
    private int thrusterJetHeight;

    public FlyingBoxPanel() {
        readThrusterJetImage();
    }

    public void readThrusterJetImage() {
        try {
            thrusterJet = ImageIO.read(new File("thruster_flame_cropped_40px.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        thrusterJetWidth = thrusterJet.getWidth();
        thrusterJetHeight = thrusterJet.getHeight();
    }

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
        // do box stuff
        boxCenter[0] += boxVelocity[0] * timestep;
        boxCenter[1] -= boxVelocity[1] * timestep;
        updateVertices();

        boxVelocity[0] += boxAccel[0] * timestep;
        boxVelocity[1] += boxAccel[1] * timestep;
        if (boxCenter[1] > setpoint) {  // if box below setpoint, fire thruster proportionally to error
            double error = boxCenter[1] - setpoint;
            relError = error/(height/2);
            thrusterAccel = thrusterMaxAccel * relError;
            thrusterAccel = Math.min(thrusterAccel, thrusterMaxAccel);
            boxVelocity[1] += thrusterAccel * timestep;
        } else relError = -1;

        repaint();
        try {
            TimeUnit.MILLISECONDS.sleep(3);
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

        // just draw dat boi
        g.setColor(Color.RED);
        for (int i=0; i<boxVertices.length; i++) {
            g.drawLine((int)boxVertices[i][0], (int)boxVertices[i][1], (int)boxVertices[(i+1)%boxVertices.length][0], (int)boxVertices[(i+1)%boxVertices.length][1]);
        }

        // just draw dat boi's thruster (if it's on)
        if (relError > 0) {
            int newWidth = (int)(thrusterJetWidth * relError);
            int newHeight = (int)(thrusterJetHeight * relError);
            if (newWidth > 0 && newHeight > 0) {
                Image thrusterJetScaled = thrusterJet.getScaledInstance(newWidth, newHeight, Image.SCALE_FAST);
                g.drawImage(thrusterJetScaled, (int)(boxCenter[0] - newWidth/2 + 1), (int)(boxCenter[1] + boxSideLength/2), this);
            }
        }
    }

    public static void main(String[] args) {
        FlyingBoxPanel fbPanel = new FlyingBoxPanel();
        fbPanel.setBackground(Color.BLACK);
        JFrame frame = new JFrame("Flying Box");
        frame.setSize(fbPanel.width, fbPanel.height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(fbPanel, BorderLayout.CENTER);
        frame.setVisible(true);
        while (true) {
            fbPanel.takeStep();
            frame.getToolkit().sync();
        }
    }
}
