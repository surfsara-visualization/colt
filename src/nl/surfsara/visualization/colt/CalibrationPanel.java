package nl.surfsara.visualization.colt;

/* Copyright 2013 SURFSara
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class CalibrationPanel extends JPanel {
    private static final long serialVersionUID = 3837266976937243051L;

    // Calibration marker positions, in normalized screen coordinates
    public static final float marker_positions[][] = { { 0.25f, 0.25f }, { 0.75f, 0.25f }, { 0.25f, 0.75f },
            { 0.75f, 0.75f } };

    public CalibrationPanel() {
        super();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
        screen_size = toolkit.getScreenSize();
        System.out.println("Screen size: " + screen_size.width + ", " + screen_size.height);

        current_marker = 0;

        solution = new float[4];
        for (int i = 0; i < 4; i++)
            solution[i] = 0.0f;

        touches = new float[marker_positions.length][2];
        touch_defined = new boolean[marker_positions.length];
        for (int i = 0; i < marker_positions.length; i++)
            touch_defined[i] = false;
    }

    // Clear calibration data
    public void reset() {
        for (int i = 0; i < marker_positions.length; i++)
            touch_defined[i] = false;
        current_marker = 0;
    }

    // Set the touch position associated with the current marker (as input by
    // the user)
    // Returns true if all positions have been set, and calibration is done
    public boolean setTouchPosition(float tx, float ty) {
        touches[current_marker][0] = tx;
        touches[current_marker][1] = ty;
        touch_defined[current_marker] = true;
        current_marker = current_marker + 1;

        if (current_marker < marker_positions.length)
            return false;

        current_marker = 0;
        computeSolution();

        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Point p = this.getLocationOnScreen();

        int panel_left = p.x;
        int panel_top = p.y;

        g.setColor(Color.RED);
        Graphics2D g2D = (Graphics2D) g;
        g2D.setStroke(new BasicStroke(3.0f));

        float marker_x, marker_y;
        int marker_i, marker_j;

        for (int i = 0; i < marker_positions.length; i++) {
            marker_x = marker_positions[i][0];
            marker_y = marker_positions[i][1];

            // Center of marker, local to panel, in screen pixel coordinates
            marker_i = (int) (screen_size.width * marker_x) - panel_left;
            marker_j = (int) (screen_size.height * marker_y) - panel_top;

            g.drawLine(marker_i - 80, marker_j, marker_i + 80, marker_j);
            g.drawLine(marker_i, marker_j - 80, marker_i, marker_j + 80);

            // Highlight current marker
            if (i == current_marker)
                g.drawRect(marker_i - 25, marker_j - 25, 50, 50);
        }
    }

    protected boolean doLinearRegression(float result[], float A[], float B[], int n) {
        float a, b;
        float sum_a, sum_b, sum_a2, sum_b2, sum_ab;

        n = 0;
        sum_a = sum_b = sum_a2 = sum_b2 = sum_ab = 0.0f;

        for (int i = 0; i < n; i++) {
            a = A[i];
            b = B[i];
            sum_a += a;
            sum_b += b;
            sum_a2 += a * a;
            sum_b2 += b * b;
            sum_ab += a * b;
        }

        if (n * sum_a2 - sum_a * sum_a < 1.0e-6f)
            return false;

        result[1] = 1.0f * (n * sum_ab - sum_a * sum_b) / (n * sum_a2 - sum_a * sum_a);
        result[0] = 1.0f * sum_b / n - 1.0f * result[1] / n * sum_a;

        return true;
    }

    public boolean computeSolution() {
        float[] x_touch = new float[marker_positions.length];
        float[] x_screen = new float[marker_positions.length];
        float[] y_touch = new float[marker_positions.length];
        float[] y_screen = new float[marker_positions.length];

        for (int i = 0; i < marker_positions.length; i++) {
            if (!touch_defined[i])
                return false;

            x_touch[i] = touches[i][0];
            x_screen[i] = marker_positions[i][0];

            y_touch[i] = touches[i][1];
            y_screen[i] = marker_positions[i][1];
        }

        float[] linreg = new float[2];

        if (!doLinearRegression(linreg, x_touch, x_screen, marker_positions.length))
            return false;

        solution[0] = linreg[0];
        solution[1] = linreg[1];

        if (!doLinearRegression(linreg, y_touch, y_screen, marker_positions.length))
            return false;

        solution[2] = linreg[0];
        solution[3] = linreg[1];

        return true;
    }

    protected Dimension screen_size;

    protected int current_marker;

    // User-generated touch coordinates corresponding to calibration markers.
    // Touch coordinate space.
    protected float touches[][];
    protected boolean touch_defined[];

    // Array of 4 floats: A, B, C, D
    // That can be used to map touch coordinates to (normalized) screen
    // coordinates:
    // x_screen = A + B * x_touch
    // y_screen = C + D * y_touch
    public float solution[];
}
