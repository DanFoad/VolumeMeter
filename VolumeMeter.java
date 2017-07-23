import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;

import java.awt.MediaTracker;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.awt.geom.Ellipse2D;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.LineUnavailableException;

import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

import java.util.List;
import java.util.ArrayList;

/**
 * VolumeMeter
 * -----------------
 * Display mic level in undecorated window, always on top
 * @author Dan Foad
 * @version 1.1.0
 */
public class VolumeMeter extends JFrame {
    
    // List of volume bars
    private List<Bar> bars = new ArrayList<Bar>();
    
    // Data line for mic input
    protected TargetDataLine line = null;
  
    /** VolumeMeter::VolumeMeter
     * Constructor :: Set title and call GUI initialisation
     */
    public VolumeMeter() {
        super("Volume Meter");
        initGUI();
    }

    /** VolumeMeter::initGUI
     * Initialise frame settings, create bars, setup display
     */
    public void initGUI() {
        
        // Create main panel to hold contents
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBackground(new Color(0, 0, 0, 0));
        
        getContentPane().add(mainPanel); // Add main panel to frame
        
        // Frame settings
        setUndecorated(true); // Remove frame
        setAlwaysOnTop(true); // Overlay other apps
        setSize(50, 100); // Just needs to be large enough
        setLocation(16, 20); // Default position
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0)); // Make frame transparent
        setType(javax.swing.JFrame.Type.UTILITY);
        
        // Create volume bars and add to main panel
        Bar bar10 = new Bar(new Color(0xFF0000)); // Red
        mainPanel.add(bar10);
        Bar bar9 = new Bar(new Color(0xFF0000)); // Red
        mainPanel.add(bar9);
        Bar bar8 = new Bar(new Color(0xFFFF00)); // Yellow
        mainPanel.add(bar8);
        Bar bar7 = new Bar(new Color(0xFFFF00)); // Yellow
        mainPanel.add(bar7);
        Bar bar6 = new Bar(new Color(0x00FF00)); // Green
        mainPanel.add(bar6);
        Bar bar5 = new Bar(new Color(0x00FF00)); // Green
        mainPanel.add(bar5);
        Bar bar4 = new Bar(new Color(0x00FF00)); // Green
        mainPanel.add(bar4);
        Bar bar3 = new Bar(new Color(0x00FF00)); // Green
        mainPanel.add(bar3);
        Bar bar2 = new Bar(new Color(0x00FF00)); // Green
        mainPanel.add(bar2);
        Bar bar1 = new Bar(new Color(0x00FF00)); // Green
        mainPanel.add(bar1);
        
        // Add all bars to list
        bars.add(bar1); bars.add(bar2); bars.add(bar3); bars.add(bar4); bars.add(bar5); bars.add(bar6); bars.add(bar7); bars.add(bar8); bars.add(bar9); bars.add(bar10); 
        
        pack(); // Pack contents of frame together
        
        // Allow movement by dragging window
        MouseAdapter ma = new MouseAdapter() {
            int lastX, lastY; // Last known position of mouse
            
            @Override
            public void mousePressed(MouseEvent e) {
                // Get mouse location
                lastX = e.getXOnScreen();
                lastY = e.getYOnScreen();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                // Get mouse location
                int x = e.getXOnScreen();
                int y = e.getYOnScreen();
                
                // Move frame by the mouse delta
                setLocation(getLocationOnScreen().x + x - lastX,
                        getLocationOnScreen().y + y - lastY);
                lastX = x;
                lastY = y;
            }
        };
        // Add listener to frame
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }
    
    /** VolumeMeter::startListening
     * Open audio channel and start working with stream
     */
    public void startListening() {
        // Open a TargetDataLine for getting mic input level
        AudioFormat format = new AudioFormat(42000.0f, 16, 1, true, true); // Get default line
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) { // If no default line
            System.out.println("The TargetDataLine is unavailable");
        }
        
        // Obtain and open the line.
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
        } catch (LineUnavailableException ex) {
            System.out.println("The TargetDataLine is Unavailable.");
        }
        
        int level = 0; // Hold calculated RMS volume level
        byte tempBuffer[] = new byte[6000]; // Data buffer for raw audio
        try {
            // Continually read in mic data into buffer and calculate RMS
            while (true) {
                // If read in enough, calculate RMS
                if (line.read(tempBuffer, 0, tempBuffer.length) > 0) {
                    level = calculateRMSLevel(tempBuffer);
                    updateBars(level); // Update bar display
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(0);
        }
    }
    
    /** VolumeMeter::calculateRMSLevel
     * Calculate the RMS of the raw audio in buffer
     * @param byte[] audioData  The buffer containing snippet of raw audio data
     * @return int  The RMS value of the buffer
     */
    protected static int calculateRMSLevel(byte[] audioData) {
        long lSum = 0;
        for(int i = 0; i < audioData.length; i++)
            lSum = lSum + audioData[i];

        double dAvg = lSum / audioData.length;

        double sumMeanSquare = 0d;
        for(int j = 0; j < audioData.length; j++)
            sumMeanSquare = sumMeanSquare + Math.pow(audioData[j] - dAvg, 2d);

        double averageMeanSquare = sumMeanSquare / audioData.length;
        return (int)(Math.pow(averageMeanSquare, 0.5d) + 0.5) - 50;
    }
    
    /** VolumeMeter::updateBars
     * Update GUI volume bars in EDT
     * @param int level     RMS value of latest audio snippet
     */
    public void updateBars(int level) {
        // Invoke on Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Update each bar to show if level > barNo.
                for (int i = 0; i < 10; i++) {
                    bars.get(i).show = (level > i);
                    bars.get(i).repaint();
                    bars.get(i).revalidate();
                }
            }
        });
    }
    
    /** VolumeMeter::close
     * Close TargetDataLine on exit
     */
    public void close() {
        line.close();
    }
    
    /** 
     * Bar
     * ---
     * JPanel with drawn rectangle representing volume bar
     * @author Dan Foad
     * @version 1.0.0
     */
    class Bar extends JPanel {
        
        // Dimensions
        private static final int RECT_X = 0;
        private static final int RECT_Y = 0;
        private static final int RECT_WIDTH = 24;
        private static final int RECT_HEIGHT = 8;
        
        // Rectangle colour
        private Color color;
        
        // Whether to display rectangle
        public boolean show = false;
        
        /** Bar::Bar
         * Constructor :: Set background colour to transparent and get rect colour
         */
        public Bar(Color color) {
            this.color = color;
            setBackground(new Color(0, 0, 0, 0));
        }
        
        /** Bar::paintComponent
         * Draw rectangle if show == true
         * @param Graphics g    Graphics object to draw with
         */
        @Override
        protected void paintComponent(Graphics g) {
            // Call default paintComponent
            super.paintComponent(g);
            
            // If show, draw background-coloured rectangle, other blank rectangle
            if (show) {
                g.setColor(color);
                g.fillRect(RECT_X, RECT_Y, RECT_WIDTH, RECT_HEIGHT);
            } else {
                g.setColor(new Color(0, 0, 0, 0));
                g.clearRect(RECT_X, RECT_Y, RECT_WIDTH, RECT_HEIGHT);
                g.fillRect(RECT_X, RECT_Y, RECT_WIDTH, RECT_HEIGHT);
            }
        }
        
        /** Bar::getPreferredSize
         * Make sure preferred size can contain rectangle
         * @return Dimension    A dimension that can contain the rectangle + offset
         */
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(RECT_WIDTH + RECT_X, RECT_HEIGHT + RECT_Y);
        }
    }

    public static void main(String[] args) {
        // Determine what the GraphicsDevice can support
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        final boolean isTranslucencySupported = gd.isWindowTranslucencySupported(TRANSLUCENT);
        
        // If translucent windows aren't supported, exit
        if (!isTranslucencySupported) {
            System.err.println("Translucency not supported, exitting");
            System.exit(0);
        }
        
        // Create GUI on event-dispatching thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                
                // Create main instantiation
                VolumeMeter main = new VolumeMeter();
                main.setVisible(true);
                
                // Activate main running code on seperate thread outside of EDT
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        main.startListening(); // Start main process
                        
                        // Add shutdown hook to close TargetDataLine on exit
                        Runtime.getRuntime().addShutdownHook(new Thread() {
                            @Override
                            public void run() {
                                main.close();
                            }
                        });
                    }
                };
                new Thread(r).start();
            }
        });
    }
}