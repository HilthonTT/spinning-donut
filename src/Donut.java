
import java.util.Arrays;

/**
 * Renders a spinning ASCII torus in the terminal.
 *
 * Based on Andy Sloane's "Donut math: how donut.c works":
 * https://www.a1k0n.net/2011/07/20/donut-math.html
 */
public class Donut {

    private static final double THETA_SPACING = 0.07;
    private static final double PHI_SPACING = 0.02;

    /**
     * Radius of the tube.
     */
    private static final double R1 = 1;
    /**
     * Distance from the torus centre to the centre of the tube.
     */
    private static final double R2 = 2;
    /**
     * Distance from the viewer to the torus.
     */
    private static final double K2 = 5;

    /**
     * Terminal cells are roughly twice as tall as they are wide, so use twice
     * as many columns.
     */
    private static final int SCREEN_HEIGHT = 35;
    private static final int SCREEN_WIDTH = SCREEN_HEIGHT * 2;
    private static final double CHAR_ASPECT = 2.0;

    /**
     * Projection scale chosen so the torus fills about 3/4 of the screen
     * height.
     */
    private static final double K1 = SCREEN_HEIGHT * K2 * 3 / (8 * (R1 + R2));

    private static final String LUMINANCE = ".,-~:;=!*#$@";

    private static final long FRAME_MILLIS = 30;

    private static final String CLEAR_SCREEN = "\u001b[2J";
    private static final String CURSOR_HOME = "\u001b[H";
    private static final String HIDE_CURSOR = "\u001b[?25l";
    private static final String SHOW_CURSOR = "\u001b[?25h";

    private final char[][] output = new char[SCREEN_HEIGHT][SCREEN_WIDTH];
    private final double[][] zBuffer = new double[SCREEN_HEIGHT][SCREEN_WIDTH];
    private final StringBuilder frame = new StringBuilder(CURSOR_HOME.length() + SCREEN_HEIGHT * (SCREEN_WIDTH + 1));

    public static void main(String[] args) throws InterruptedException {
        // Restore the cursor when the program is stopped with Ctrl+C.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print(SHOW_CURSOR);
            System.out.flush();
        }));

        System.out.print(CLEAR_SCREEN + HIDE_CURSOR);

        Donut donut = new Donut();
        double a = 1.0;
        double b = 1.0;

        while (true) {
            long start = System.nanoTime();

            System.out.print(donut.render(a, b));
            System.out.flush();

            // Keep the angles bounded so precision never degrades on long runs.
            a = (a + 0.08) % (2 * Math.PI);
            b = (b + 0.03) % (2 * Math.PI);

            long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
            long sleep = FRAME_MILLIS - elapsedMillis;
            if (sleep > 0) {
                Thread.sleep(sleep);
            }
        }
    }

    /**
     * Renders one frame with rotation {@code a} about the x-axis and {@code b}
     * about the z-axis.
     */
    private CharSequence render(double a, double b) {
        double cosA = Math.cos(a);
        double sinA = Math.sin(a);
        double cosB = Math.cos(b);
        double sinB = Math.sin(b);

        for (int row = 0; row < SCREEN_HEIGHT; row++) {
            Arrays.fill(output[row], ' ');
            Arrays.fill(zBuffer[row], 0);
        }

        for (double theta = 0; theta < 2 * Math.PI; theta += THETA_SPACING) {
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);

            // Point on the circle that is swept around the y-axis to form the torus.
            double circleX = R2 + R1 * cosTheta;
            double circleY = R1 * sinTheta;

            for (double phi = 0; phi < 2 * Math.PI; phi += PHI_SPACING) {
                double cosPhi = Math.cos(phi);
                double sinPhi = Math.sin(phi);

                // Surface normal · light direction (0, 1, -1); only lit surfaces are drawn.
                double luminance = cosPhi * cosTheta * sinB
                        - cosA * cosTheta * sinPhi
                        - sinA * sinTheta
                        + cosB * (cosA * sinTheta - cosTheta * sinA * sinPhi);
                if (luminance <= 0) {
                    continue;
                }

                double x = circleX * (cosB * cosPhi + sinA * sinB * sinPhi) - circleY * cosA * sinB;
                double y = circleX * (sinB * cosPhi - sinA * cosB * sinPhi) + circleY * cosA * cosB;
                double z = K2 + cosA * circleX * sinPhi + circleY * sinA;
                double ooz = 1 / z;

                // Math.floor (not an int cast) so points just left/above the screen are not
                // truncated toward zero onto column/row 0.
                int column = (int) Math.floor(SCREEN_WIDTH / 2.0 + CHAR_ASPECT * K1 * ooz * x);
                int row = (int) Math.floor(SCREEN_HEIGHT / 2.0 - K1 * ooz * y);

                if (column < 0 || column >= SCREEN_WIDTH || row < 0 || row >= SCREEN_HEIGHT) {
                    continue;
                }

                if (ooz > zBuffer[row][column]) {
                    zBuffer[row][column] = ooz;
                    // luminance is at most sqrt(2), so * 8 maps it to 0..11.
                    int index = Math.min((int) (luminance * 8), LUMINANCE.length() - 1);
                    output[row][column] = LUMINANCE.charAt(index);
                }
            }
        }

        frame.setLength(0);
        frame.append(CURSOR_HOME);
        for (char[] line : output) {
            frame.append(line).append('\n');
        }
        return frame;
    }
}
