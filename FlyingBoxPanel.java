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
import java.util.Random;


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
    private double thrusterMaxAccel = 15.0;  // m/s^2
    private int setpoint = height/4;  // pixels
    private int[] target = {width/2, setpoint};

    private BufferedImage thrusterJet;
    private int thrusterJetWidth;
    private int thrusterJetHeight;
    private Random rand = new Random();

    private enum ControllerType {
        P,
        PI,
        PD,
        PID
    }
    private double proportionalGain = thrusterMaxAccel * 2;
    private double integralGain = 0.5;
    private double derivativeGain = 300;
    private double relError = 0;
    private double relErrorPrev = 0;
    private double relErrorSum = 0;
    private double relErrorSumMax = thrusterMaxAccel * 1.5;
    private double relErrorSumMin = -thrusterMaxAccel / 2;
    private boolean firstStep = true;
    private ControllerType controller = ControllerType.PID;

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
        
        double error = boxCenter[1] - setpoint;
        relError = error/(height/2);

        if (firstStep) {
            relErrorPrev = relError;
            firstStep = false;
        }
        double proportionalTerm = 0;
        double integralTerm = 0;
        double derivativeTerm = 0;
        double relErrorDelta = 0;
        switch (controller) {
            case P: // proportional control
                proportionalTerm = relError * proportionalGain;
                thrusterAccel = proportionalTerm;
                break;
            
            case PI:  // proportional-integral control
                proportionalTerm = relError * proportionalGain;
                relErrorSum += relError;
                // clamp the integral sum to prevent excessive wind-up
                relErrorSum = Math.max(relErrorSumMin, Math.min(relErrorSumMax, relErrorSum));
                integralTerm = relErrorSum * integralGain;
                thrusterAccel = proportionalTerm + integralTerm;
                break;
            
            case PD:  // proportional-derivative control
                proportionalTerm = relError * proportionalGain;
                relErrorDelta = (relError - relErrorPrev) / timestep;
                derivativeTerm = relErrorDelta * derivativeGain;
                thrusterAccel = proportionalTerm + derivativeTerm;
                relErrorPrev = relError;
                break;

            case PID:  // proportional-integral-derivative
                proportionalTerm = relError * proportionalGain;
                relErrorSum += relError;
                // clamp the integral sum to prevent excessive wind-up
                relErrorSum = Math.max(relErrorSumMin, Math.min(relErrorSumMax, relErrorSum));
                integralTerm = relErrorSum * integralGain;
                relErrorDelta = (relError - relErrorPrev) / timestep;
                derivativeTerm = relErrorDelta * derivativeGain;
                thrusterAccel = proportionalTerm + integralTerm + derivativeTerm;
                relErrorPrev = relError;
                break;
        }
        thrusterAccel = Math.min(thrusterAccel, thrusterMaxAccel);
        thrusterAccel = Math.max(thrusterAccel, 0);
        boxVelocity[1] += thrusterAccel * timestep;
        

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
        // draw the target
        g.setColor(Color.WHITE);
        g.drawLine(target[0]+boxSideLength/2, target[1]+boxSideLength/2, target[0]+boxSideLength, target[1]+boxSideLength/2);

        // draw dat boi
        g.setColor(Color.RED);
        for (int i=0; i<boxVertices.length; i++) {
            g.drawLine((int)boxVertices[i][0], (int)boxVertices[i][1], (int)boxVertices[(i+1)%boxVertices.length][0], (int)boxVertices[(i+1)%boxVertices.length][1]);
        }

        // draw dat boi's thruster (if it's on)
        if (thrusterAccel > 0) {
            int newWidth = (int)(thrusterJetWidth * thrusterAccel/thrusterMaxAccel) + (rand.nextInt(3+1) - 1);
            int newHeight = (int)(thrusterJetHeight * thrusterAccel/thrusterMaxAccel) + (rand.nextInt(3+1) - 1);
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
