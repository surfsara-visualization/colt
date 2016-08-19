package nl.surfsara.visualization.colt;

/* Copyright 2012-2015 SURFSara
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;

import nl.surfsara.visualization.colt.datastructures.TouchPoint;
import nl.surfsara.visualization.colt.datastructures.TuioTouchHistory;

/**
 * @author Paul Melis (paul.melis@surfsara.nl)
 * 
 */
/*
 * - when no TUIO events are available, don't send anything? we currently only
 * send an fseq increase, but no touches
 */
public class SaraTouchClient extends JFrame implements ActionListener, ItemListener, WindowListener, ComponentListener,
        TouchEventHandler {
    private static final long serialVersionUID = 1566656153975345778L;

    // All in millimeters
    public static final float DISPLAY_PIXEL_PITCH = 0.63f;
    public static final float DISPLAY_VIEWABLE_WIDTH = 1209.6f;
    public static final float DISPLAY_VIEWABLE_HEIGHT = 680.4f;
    public static final float BEZEL_PHYSICAL_WIDTH = 7.0f;
    public static final float BEZEL_PHYSICAL_HEIGHT = 7.0f;

    /*
     * // The touch overlay isn't nicely aligned with the pixel edges of the //
     * displays. On the left about 4 pixels are *beneath* the touch overlay, //
     * on the bottom about 2 pixels. To right there's a gap of black of // about
     * 2 pixels, on the top of about 4 pixels. public int EDGE_LEFT = -4; public
     * int EDGE_RIGHT = 2; public int EDGE_TOP = 5; public int EDGE_BOTTOM = -2;
     */

    public static final float COORD_LEFT = 0.00122f;
    public static final float COORD_RIGHT = 0.99667f;
    public static final float COORD_TOP = 0.01201f;
    public static final float COORD_BOTTOM = 0.98400f;
    public static final float COORD_WIDTH = COORD_RIGHT - COORD_LEFT;
    public static final float COORD_HEIGHT = COORD_BOTTOM - COORD_TOP;

    public static final int DISPLAY_WIDTH = 1920;
    public static final int DISPLAY_HEIGHT = 1080;
    public static final int BEZEL_WIDTH = (int) (BEZEL_PHYSICAL_WIDTH / DISPLAY_PIXEL_PITCH);
    public static final int BEZEL_HEIGHT = (int) (BEZEL_PHYSICAL_WIDTH / DISPLAY_PIXEL_PITCH);

    public static final int TOTAL_WIDTH = 4 * DISPLAY_WIDTH + 3 * BEZEL_WIDTH;
    public static final int TOTAL_HEIGHT = 2 * DISPLAY_HEIGHT + BEZEL_HEIGHT;

    // Arrays of 4 floats: A, B, C, D
    // These are used to map touch coordinates to (normalized) screen
    // coordinates,
    // depending on the region of interest chosen:
    // x_screen = A + B * x_touch
    // y_screen = C + D * y_touch
    // Note that touch coordinates use a unit coordinate system, i.e.
    // [0,1] for both X and Y
    public static final float calibration_values[][] = {
            // { A, B, C, D }

            // 0: Upper-left display
            // { 0.00375f, 3.99939f, -0.00800f, 2.20194f },
            { 0.00449f, 3.99783f, -0.00683f, 2.01876f },
            // 1
            { -1.00374f, 4.00787f, -0.00623f, 2.01864f },
            // 2
            { -2.01796f, 4.02465f, -0.00930f, 2.03391f },
            // 3: Upper-right display
            { -3.03241f, 4.03634f, -0.00748f, 2.02574f },
            // 4: Lower-left display
            { 0.00373f, 3.99444f, -1.01733f, 2.01556f },
            // 5
            { -1.00906f, 4.01850f, -1.01344f, 2.01387f },
            // 6
            { -2.02162f, 4.02793f, -1.01391f, 2.01795f },
            // 7: Lower-right display
            { -3.03668f, 4.03847f, -1.01592f, 2.02027f },
            // 8: Left 2x2
            { 0.00176f, 1.99361f, -0.00230f, 1.00082f },
            // 9: Middle 2x2
            { -0.50125f, 1.9994f, -0.00195f, 1.00176f },
            // 10: Right 2x2
            { -1.00601f, 2.00601f, -0.00241f, 1.00297f },
            // 11: Whole screen (identity mapping)
            { 0.000000f, 1.00000f, 0.000000f, 1.00000f },

    /*
     * { 7.91161f, 7678.14646f, -6.44838f, 2178.16680f }, { -1930.68770f,
     * 7705.44436f, -5.33848f, 2181.83648f }, { -3870.86675f, 7722.65348f,
     * -6.48688f, 2186.26676f }, { -5825.31076f, 7754.33003f, -8.48132f,
     * 2189.56337f }, { 5.85889f, 7672.58697f, -1093.96187f, 2170.36803f }, {
     * -1930.95976f, 7698.26146f, -1093.52530f, 2175.88811f }, { -3878.62925f,
     * 7729.75958f, -1087.37080f, 2170.74884f }, { -5835.28729f, 7759.76520f,
     * -1097.98312f, 2184.47089f }, { 4.22413f, 3825.94430f, -1.82421f,
     * 1079.89356f }, { -962.28699f, 3839.09643f, -0.49113f, 1080.52979f }, {
     * -1931.44378f, 3851.9570f, -3.53500f, 1084.78162f },
     */
    };

    static final int ROI_DISPLAY_0 = 0;
    static final int ROI_DISPLAY_1 = 1;
    static final int ROI_DISPLAY_2 = 2;
    static final int ROI_DISPLAY_3 = 3;
    static final int ROI_DISPLAY_4 = 4;
    static final int ROI_DISPLAY_5 = 5;
    static final int ROI_DISPLAY_6 = 6;
    static final int ROI_DISPLAY_7 = 7;
    static final int ROI_LEFT_2x2 = 8;
    static final int ROI_MIDDLE_2x2 = 9;
    static final int ROI_RIGHT_2x2 = 10;
    static final int ROI_WHOLE_SCREEN = 11;

    static final int TS_NEW = 0;
    static final int TS_MOVED = 1;
    static final int TS_RELEASED = 2;

    public static void main(String[] args) throws Exception {
        SaraTouchClient client = new SaraTouchClient();
        client.setSize(600, 500);
        client.setVisible(true);
    }

    public SaraTouchClient() throws Exception {
        super("COLT - Collaboratorium Touch Client");

        // Get local screen size
        Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
        screen_size = toolkit.getScreenSize();
        System.out.println("Screen size: " + screen_size.width + ", " + screen_size.height);

        calibration_panel = new JPanel();

        // Create robot for mouse control
        createRobot();

        ignored_touches = new HashSet<Integer>();
        tuio_touch_history = new HashSet<TuioTouchHistory>();
        next_frame_number = 1;

        // Create socket stuff for sending TUIO events locally
        outgoing_tuio_socket = new DatagramSocket();
        localhost_address = InetAddress.getByName("localhost");
        // outgoing_tuio_socket.close();

        connected = false;

        //
        // Set up GUI
        //

        // General

        JPanel general_panel = new JPanel();
        LayoutManager layout = new BoxLayout(general_panel, BoxLayout.PAGE_AXIS);
        general_panel.setLayout(layout);

        bt_connect_to_server = new JButton("Connect to touch server");
        bt_connect_to_server.addActionListener(this);
        general_panel.add(bt_connect_to_server);

        // Mode radio

        JPanel panel = new JPanel(new GridLayout(0, 1));
        Border border = BorderFactory.createTitledBorder("Mode");
        panel.setBorder(border);

        rb_control_mouse = new JRadioButton("Control mouse");
        rb_send_tuio_events = new JRadioButton("Send TUIO events", true); // XXX
                                                                    // rename to
                                                                    // packets

        ButtonGroup mode_group = new ButtonGroup();

        mode_group.add(rb_control_mouse);
        panel.add(rb_control_mouse);

        mode_group.add(rb_send_tuio_events);
        panel.add(rb_send_tuio_events);

        general_panel.add(panel);

        // ROI radio

        panel = new JPanel(new GridLayout(0, 1));
        border = BorderFactory.createTitledBorder("Region of interest");
        panel.setBorder(border);

        ButtonGroup roi_group = new ButtonGroup();

        region_of_interest = ROI_WHOLE_SCREEN;

        rb_roi_display_0 = new JRadioButton("Display 0 (upper-left)");
        rb_roi_display_1 = new JRadioButton("Display 1");
        rb_roi_display_2 = new JRadioButton("Display 2");
        rb_roi_display_3 = new JRadioButton("Display 3 (upper-right)");
        rb_roi_display_4 = new JRadioButton("Display 4 (lower-left)");
        rb_roi_display_5 = new JRadioButton("Display 5");
        rb_roi_display_6 = new JRadioButton("Display 6");
        rb_roi_display_7 = new JRadioButton("Display 7 (lower-right)");
        rb_roi_left_2x2 = new JRadioButton("Left 2x2");
        rb_roi_middle_2x2 = new JRadioButton("Middle 2x2");
        rb_roi_right_2x2 = new JRadioButton("Right 2x2");
        rb_roi_whole_screen = new JRadioButton("Whole screen", true);

        rb_roi_display_0.addItemListener(this);
        rb_roi_display_1.addItemListener(this);
        rb_roi_display_2.addItemListener(this);
        rb_roi_display_3.addItemListener(this);
        rb_roi_display_4.addItemListener(this);
        rb_roi_display_5.addItemListener(this);
        rb_roi_display_6.addItemListener(this);
        rb_roi_display_7.addItemListener(this);
        rb_roi_left_2x2.addItemListener(this);
        rb_roi_middle_2x2.addItemListener(this);
        rb_roi_right_2x2.addItemListener(this);
        rb_roi_whole_screen.addItemListener(this);

        roi_group.add(rb_roi_display_0);
        roi_group.add(rb_roi_display_1);
        roi_group.add(rb_roi_display_2);
        roi_group.add(rb_roi_display_3);
        roi_group.add(rb_roi_display_4);
        roi_group.add(rb_roi_display_5);
        roi_group.add(rb_roi_display_6);
        roi_group.add(rb_roi_display_7);
        roi_group.add(rb_roi_left_2x2);
        roi_group.add(rb_roi_middle_2x2);
        roi_group.add(rb_roi_right_2x2);
        roi_group.add(rb_roi_whole_screen);

        panel.add(rb_roi_display_0);
        panel.add(rb_roi_display_1);
        panel.add(rb_roi_display_2);
        panel.add(rb_roi_display_3);
        panel.add(rb_roi_display_4);
        panel.add(rb_roi_display_5);
        panel.add(rb_roi_display_6);
        panel.add(rb_roi_display_7);
        panel.add(rb_roi_left_2x2);
        panel.add(rb_roi_middle_2x2);
        panel.add(rb_roi_right_2x2);
        panel.add(rb_roi_whole_screen);

        general_panel.add(panel);

        // Advanced

        JPanel advanced_panel = new JPanel();

        advanced_panel.setLayout(new GridLayout(3, 2));
        
        String colt_server = System.getenv("COLT_SERVER");
        String colt_portstring = System.getenv("COLT_PORT");
        
        if (colt_server == null)
            colt_server = "145.100.39.11";  // Linda
        if (colt_portstring == null)
            colt_portstring = "12345";

        JLabel lbl_host = new JLabel("Touch server host");
        tf_host = new JTextField();
        tf_host.setText(colt_server); 
        advanced_panel.add(lbl_host);
        advanced_panel.add(tf_host);

        JLabel lbl_port = new JLabel("Touch server port");
        tf_port = new JTextField();
        tf_port.setText(colt_portstring);
        advanced_panel.add(lbl_port);
        advanced_panel.add(tf_port);

        // Calibration

        /*
         * calibration_panel = new CalibrationPanel(); bt_calibrate = new
         * JButton("Calibrate");
         * bt_calibrate.setAlignmentX(Component.CENTER_ALIGNMENT);
         * bt_calibrate.addActionListener(this);
         * calibration_panel.add(bt_calibrate);
         * 
         * calibration_message = new JLabel("...");
         * calibration_message.setAlignmentX(Component.CENTER_ALIGNMENT);
         * calibration_panel.add(calibration_message);
         */

        // Log

        ta_log = new JTextArea();
        ta_log.setEditable(false);
        ta_log.setLineWrap(true);

        JScrollPane log_scroll_pane = new JScrollPane(ta_log);
        log_scroll_pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel log_panel = new JPanel();
        log_panel.setLayout(new BorderLayout());
        log_panel.add(log_scroll_pane, BorderLayout.CENTER);

        // About
        
        java.util.Properties props = new java.util.Properties();
        
        try 
        {
            java.io.InputStream stream = this.getClass().getResourceAsStream("/colt.properties");  
            props.load(stream);
            stream.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }        

        JPanel about_panel = new JPanel();
        about_panel.setLayout(new GridLayout(0, 1));

        about_panel.add(new JLabel("COLT - Collaboratorium Touch Client"));
        about_panel.add(new JLabel("Copyright (C) 2012-2016, SURFsara"));
        about_panel.add(new JLabel("Version: " + props.getProperty("version")));
        about_panel.add(new JLabel("Git revision/base: " + props.getProperty("revision")));
        about_panel.add(new JLabel("Built " + props.getProperty("buildtime") + " by " + props.getProperty("builder")));
        
        // Tabs

        JTabbedPane tabbed_pane;

        tabbed_pane = new JTabbedPane();
        tabbed_pane.addTab("General", null, general_panel, "General settings");
        tabbed_pane.addTab("Advanced", null, advanced_panel, "Advanced settings");
        // tabbed_pane.addTab("Calibration", null, calibration_panel,
        // "Calibration");
        tabbed_pane.addTab("Log", null, log_panel, "Log");
        tabbed_pane.addTab("About", null, about_panel, "About");

        // setLayout(new FlowLayout());
        add(tabbed_pane);

        addWindowListener(this);
        addComponentListener(this);
    }

    protected void connect(String host, int port) {
        InetSocketAddress addr = new InetSocketAddress(host, port);

        try {
            server_socket = new Socket();
            server_socket.connect(addr, 5000);
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(this, e, "While trying to connect", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e, "While trying to connect", JOptionPane.ERROR_MESSAGE);
            return;
        }

        mouse_point = -1;

        connection_handler = new ConnectionHandler(this, server_socket, host, port);
        connection_thread = new Thread(connection_handler);
        // connection_thread.setDaemon(true);
        connection_thread.start();

        connected = true;
        bt_connect_to_server.setText("Disconnect");

        rb_control_mouse.setEnabled(false);
        rb_send_tuio_events.setEnabled(false);
    }

    protected void disconnect() {
        try {
            // XXX stop connection handler thread
            server_socket.close();
            connected = false;
            bt_connect_to_server.setText("Connect to touch server");
            rb_control_mouse.setEnabled(true);
            rb_send_tuio_events.setEnabled(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e, "While trying to disconnect", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() != ItemEvent.SELECTED)
            return;

        if (event.getSource() == rb_roi_display_0)
            region_of_interest = ROI_DISPLAY_0;
        else if (event.getSource() == rb_roi_display_1)
            region_of_interest = ROI_DISPLAY_1;
        else if (event.getSource() == rb_roi_display_2)
            region_of_interest = ROI_DISPLAY_2;
        else if (event.getSource() == rb_roi_display_3)
            region_of_interest = ROI_DISPLAY_3;
        else if (event.getSource() == rb_roi_display_4)
            region_of_interest = ROI_DISPLAY_4;
        else if (event.getSource() == rb_roi_display_5)
            region_of_interest = ROI_DISPLAY_5;
        else if (event.getSource() == rb_roi_display_6)
            region_of_interest = ROI_DISPLAY_6;
        else if (event.getSource() == rb_roi_display_7)
            region_of_interest = ROI_DISPLAY_7;
        else if (event.getSource() == rb_roi_left_2x2)
            region_of_interest = ROI_LEFT_2x2;
        else if (event.getSource() == rb_roi_middle_2x2)
            region_of_interest = ROI_MIDDLE_2x2;
        else if (event.getSource() == rb_roi_right_2x2)
            region_of_interest = ROI_RIGHT_2x2;
        else if (event.getSource() == rb_roi_whole_screen)
            region_of_interest = ROI_WHOLE_SCREEN;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == bt_connect_to_server) {
            if (!connected) {
                String host = tf_host.getText();
                int port = Integer.parseInt(tf_port.getText());
                connect(host, port);
            } else
                disconnect();

        } else if (event.getSource() == bt_calibrate) {
            // Make sure the window is maximized, to show all markers
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
    }

    public boolean createRobot() {
        try {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
            // robot.setAutoDelay(2);
            System.out.println("Robot autodelay = " + robot.getAutoDelay());
        } catch (AWTException e) {
            System.out.println("System does not allow low-level control");
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println("Screen is not a graphics device");
            return false;
        } catch (SecurityException e) {
            System.out.println("No permission to create robot");
            return false;
        }

        return true;
    }

    public void transformToROI(TouchPoint tp) {
        float A = calibration_values[region_of_interest][0];
        float B = calibration_values[region_of_interest][1];
        float C = calibration_values[region_of_interest][2];
        float D = calibration_values[region_of_interest][3];

        // Transform from touch space to normalized screen space
        tp.nx = A + tp.tx * B;
        tp.ny = C + tp.ty * D;

        // Transform to screen space pixel coordinates
        tp.si = (int) (tp.nx * screen_size.width);
        tp.sj = (int) (tp.ny * screen_size.height);

        // System.out.println("transformToROI(): "+tp.tx+","+tp.ty+" -> "+tp.nx+","+tp.ny+" | "+tp.si+","+tp.sj);
    }

    @Override
    public void OnTouchPoints(double timestamp, TouchPoint[] points, int n) {
        TouchPoint tp;

        // Perform:
        // - Transformation of touch coordinates to calibrated screen
        // coordinates
        // - Removal of events originating outside region of interest
        // - Clamping of events to region of interest

        // We basically throw out elements from points[] that we want to ignore.
        // Reusing the array in place saves allocating a new one
        int next_free_point = 0;

        for (int i = 0; i < n; i++) {
            tp = points[i];

            if (ignored_touches.contains(tp.id)) {
                // Point is already excluded
                if (tp.state == TS_RELEASED)
                    ignored_touches.remove(tp.id);
                continue;
            }

            // Get screen space coordinates
            transformToROI(tp);

            if (tp.state == TS_NEW) {
                if (tp.si < 0 || tp.si >= screen_size.width || tp.sj < 0 || tp.sj >= screen_size.height) {
                    // New touch point is outside region of interest, ignore all
                    // further events for it
                    ignored_touches.add(tp.id);
                    continue;
                }
                // else: new point is already within region of interest, no
                // clamping needed
            } else {
                // Moved or released point, clamp to region of interest if
                // needed
                if (tp.si < 0)
                    tp.si = 0;
                else if (tp.si >= screen_size.width)
                    tp.si = screen_size.width - 1;

                if (tp.sj < 0)
                    tp.sj = 0;
                else if (tp.sj >= screen_size.height)
                    tp.sj = screen_size.height - 1;
            }

            // Store in next free slot
            points[next_free_point] = tp;
            next_free_point++;
        }

        n = next_free_point;

        if (rb_control_mouse.isSelected()) {
            doMouseAction(timestamp, points, n);
        } else if (rb_send_tuio_events.isSelected()) {
            try {
                sendTuioPacket(timestamp, points, n);
            } catch (java.net.UnknownHostException e) {
                JOptionPane.showMessageDialog(this, e, "While trying to send TUIO packet", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, e, "While trying to send TUIO packet", JOptionPane.ERROR_MESSAGE);
            }
        }

        next_frame_number += 1;
    }

    protected void doMouseAction(double timestamp, TouchPoint[] points, int n) {
        if (robot == null)
            return;

        if (mouse_point != -1) {
            // Check what happened to mouse point
            for (int i = 0; i < n; i++) {
                TouchPoint tp = points[i];

                if (tp.id != mouse_point)
                    continue;

                transformToROI(tp);

                if (tp.state == TS_MOVED) {
                    // Mouse move
                    robot.mouseMove(tp.si, tp.sj);
                    addLogLine("Mouse moved to " + tp.si + ", " + tp.sj + "\n");
                } else {
                    // Mouse point removed
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                    addLogLine("Mouse released\n");
                }

                return;
            }

            // Mouse point lost?
            mouse_point = -1;
        }

        // No mouse point yet
        for (int i = 0; i < n; i++) {
            // First new point will become mouse
            TouchPoint tp = points[i];

            transformToROI(tp);

            if (tp.state != TS_NEW)
                continue;

            // New mouse point
            mouse_point = tp.id;
            mouse_down_time = timestamp;

            robot.mouseMove(tp.si, tp.sj);
            robot.mousePress(InputEvent.BUTTON1_MASK);

            addLogLine("Mouse pressed at " + tp.si + ", " + tp.sj + "\n");
        }
    }

    protected int pad4(int n) {
        while (n % 4 != 0)
            n++;
        return n;
    }

    protected void appendPaddedString(DataOutputStream dos, String s) {
        try {
            dos.writeBytes(s);
            dos.writeByte(0x00);
            int n = s.length() + 1;
            while (n % 4 != 0) {
                dos.writeByte(0x00);
                n++;
            }

        } catch (IOException e) {
        }
    }

    protected TuioTouchHistory findTuioTouchHistory(int id) {
        Iterator<TuioTouchHistory> it = tuio_touch_history.iterator();
        TuioTouchHistory res;
        while (it.hasNext()) {
            Object element = it.next();

            res = (TuioTouchHistory) element;
            if (res.id == id)
                return res;
        }
        return null;
    }

    protected void updateTuioTouchHistory(double t, int id, float x, float y, float vx, float vy) {
        TuioTouchHistory res = findTuioTouchHistory(id);

        if (res == null) {
            // Insert new entry
            res = new TuioTouchHistory(t, id, x, y, vx, vy);
            tuio_touch_history.add(res);
        } else {
            // Update
            res.timestamp = t;
            res.last_x = x;
            res.last_y = y;
            res.last_xvel = vx;
            res.last_yvel = vy;
        }
    }

    // Note: sends to localhost
    protected void sendTuioPacket(double timestamp, TouchPoint[] points, int n) throws java.net.UnknownHostException,
            java.io.IOException {
        // First encode a TUIO packet
        // TUIO uses big-endian encoding, which the Java OutputStream's should
        // already do

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        String s;
        int elem_size;
        TouchPoint tp;
        int num_alive;
        TuioTouchHistory history;
        float x, y, vx, vy, a;
        float dt;

        // Header
        appendPaddedString(dos, "#bundle");

        Timestamp ntp_timestamp = new Timestamp(timestamp);
        byte[] ntp_bytes = ntp_timestamp.getByteArray();
        dos.write(ntp_bytes, 0, 8);

        // Alive
        num_alive = 0;

        s = ",s";
        for (int i = 0; i < n; i++) {
            if (points[i].state != TS_RELEASED) {
                s += "i";
                num_alive++;
            }
        }

        elem_size = 12 + pad4(2 + num_alive + 1) + 8 + num_alive * 4;

        dos.writeInt(elem_size);
        appendPaddedString(dos, "/tuio/2Dcur");
        appendPaddedString(dos, s);
        appendPaddedString(dos, "alive");

        for (int i = 0; i < n; i++) {
            if (points[i].state != TS_RELEASED)
                dos.writeInt(points[i].id);
        }

        // Set
        elem_size = 52;

        for (int i = 0; i < n; i++) {
            tp = points[i];

            if (tp.state == TS_RELEASED) {
                tuio_touch_history.remove(tp.id);
                continue;
            }

            // TUIO needs normalized coordinates :)
            x = tp.nx;
            y = tp.ny;

            history = findTuioTouchHistory(tp.id);
            if (history != null) {
                dt = (float) (timestamp - history.timestamp);
                vx = (x - history.last_x) / dt;
                vy = (y - history.last_y) / dt;
                a = (float) Math.sqrt(vx * vx + vy * vy
                        - (history.last_xvel * history.last_xvel + history.last_yvel * history.last_yvel));
            } else {
                vx = vy = 0.0f;
                a = 0.0f;
            }

            dos.writeInt(elem_size);
            appendPaddedString(dos, "/tuio/2Dcur");
            appendPaddedString(dos, ",sifffff");
            appendPaddedString(dos, "set");
            dos.writeInt(tp.id);
            dos.writeFloat(x);
            dos.writeFloat(y);
            dos.writeFloat(vx);
            dos.writeFloat(vy);
            dos.writeFloat(a);

            updateTuioTouchHistory(timestamp, tp.id, x, y, vx, vy);
        }

        // Fseq
        elem_size = 28;

        dos.writeInt(elem_size);
        appendPaddedString(dos, "/tuio/2Dcur");
        appendPaddedString(dos, ",si");
        appendPaddedString(dos, "fseq");
        dos.writeInt(next_frame_number);

        // Send it!

        byte[] payload = bos.toByteArray();
        // System.out.println("Sending TUIO packet with payload size "+payload.length);
        DatagramPacket packet = new DatagramPacket(payload, payload.length, localhost_address, 3333);

        try {
            outgoing_tuio_socket.send(packet);
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(this, e, "While trying to send TUIO packet", JOptionPane.ERROR_MESSAGE);
            return;

        }
    }

    protected void addLogLine(String s) {
        ta_log.append(s);
        // Make sure line just added is visible
        ta_log.setCaretPosition(ta_log.getText().length());
    }

    // WindowListener interface
    @Override
    public void windowClosing(WindowEvent event) {
        System.exit(0);
    }

    @Override
    public void windowClosed(WindowEvent event) {
    } // do nothing for now

    @Override
    public void windowDeiconified(WindowEvent event) {
    }

    @Override
    public void windowIconified(WindowEvent event) {
    }

    @Override
    public void windowActivated(WindowEvent event) {
    }

    @Override
    public void windowDeactivated(WindowEvent event) {
    }

    @Override
    public void windowOpened(WindowEvent event) {
    }

    // ComponentListener interface
    @Override
    public void componentMoved(ComponentEvent e) {
        calibration_panel.repaint();
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentResized(ComponentEvent e) {
    }

    protected JButton bt_connect_to_server;
    protected Socket server_socket;
    protected boolean connected;
    protected ConnectionHandler connection_handler;
    protected Thread connection_thread;

    protected JRadioButton rb_control_mouse;
    protected JRadioButton rb_send_tuio_events;

    protected JRadioButton rb_roi_display_0;
    protected JRadioButton rb_roi_display_1;
    protected JRadioButton rb_roi_display_2;
    protected JRadioButton rb_roi_display_3;
    protected JRadioButton rb_roi_display_4;
    protected JRadioButton rb_roi_display_5;
    protected JRadioButton rb_roi_display_6;
    protected JRadioButton rb_roi_display_7;
    protected JRadioButton rb_roi_left_2x2;
    protected JRadioButton rb_roi_middle_2x2;
    protected JRadioButton rb_roi_right_2x2;
    protected JRadioButton rb_roi_whole_screen;

    protected int region_of_interest;
    protected JPanel calibration_panel;
    protected JButton bt_calibrate;
    protected JLabel calibration_message;
    protected Robot robot;
    protected int mouse_point;
    protected double mouse_down_time;

    protected int next_frame_number;
    protected HashSet<Integer> ignored_touches;
    protected HashSet<TuioTouchHistory> tuio_touch_history;

    protected DatagramSocket outgoing_tuio_socket;
    protected InetAddress localhost_address;

    protected Dimension screen_size;

    private final JTextField tf_host;
    private final JTextField tf_port;
    protected JTextArea ta_log;
}
