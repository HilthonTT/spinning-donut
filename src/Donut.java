package src;

public class Donut {

    private static final double THETA_SPACING = 0.07;
    private static final double PHI_SPACING = 0.02;

    private static final double R1 = 1;
    private static final double R2 = 2;
    private static final double K2 = 5;

    private static final int SCREEN_WIDTH = 35;
    private static final int SCREEN_HEIGHT = 35;

    private static final double K1 = SCREEN_WIDTH * K2 * 3 / (8 * (R1 + R2));

    private static final String LUMINANCE = ".,-~:;=!*#$@";

    public static void main(String[] args) throws InterruptedException {
        System.out.print("\u001b[2J");

        double a = 1.0;
        double b = 1.0;

        while (true) {
            renderFrame(a, b);
            a += 0.08;
            b += 0.03;
            Thread.sleep(30);
        }

        // for (int i = 0; i < 250; i++) {
        // }
    }

    private static void renderFrame(double a, double b) {
        double cosA = Math.cos(a);
        double sinA = Math.sin(a);
        double cosB = Math.cos(b);
        double sinB = Math.sin(b);

        // Indexed as [row][column], i.e. [yp][xp], so the print loop below can read it
        // row by row
        char[][] charOutput = new char[SCREEN_HEIGHT][SCREEN_WIDTH];
        double[][] zbuffer = new double[SCREEN_HEIGHT][SCREEN_WIDTH];

        for (char[] row : charOutput) {
            java.util.Arrays.fill(row, ' ');
        }

        for (double theta = 0; theta < 2 * Math.PI; theta += THETA_SPACING) {
            double costheta = Math.cos(theta);
            double sintheta = Math.sin(theta);

            for (double phi = 0; phi < 2 * Math.PI; phi += PHI_SPACING) {
                double cosphi = Math.cos(phi);
                double sinphi = Math.sin(phi);

                double circlex = R2 + R1 * costheta;
                double circley = R1 * sintheta;

                double x = circlex * (cosB * cosphi + sinA * sinB * sinphi) - circley * cosA * sinB;
                double y = circlex * (sinB * cosphi - sinA * cosB * sinphi) + circley * cosA * cosB;
                double z = K2 + cosA * circlex * sinphi + circley * sinA;
                double ooz = 1 / z;

                int xp = (int) (SCREEN_WIDTH / 2 + K1 * ooz * x);
                int yp = (int) (SCREEN_HEIGHT / 2 - K1 * ooz * y);

                double l = cosphi * costheta * sinB
                        - cosA * costheta * sinphi
                        - sinA * sintheta
                        + cosB * (cosA * sintheta - costheta * sinA * sinphi);

                if (l > 0) {
                    if (xp < 0 || xp >= SCREEN_WIDTH || yp < 0 || yp >= SCREEN_HEIGHT) {
                        continue;
                    }
                    if (ooz > zbuffer[yp][xp]) {
                        zbuffer[yp][xp] = ooz;
                        int luminanceIndex = (int) (l * 8);
                        if (luminanceIndex > LUMINANCE.length() - 1) {
                            luminanceIndex = LUMINANCE.length() - 1;
                        }
                        charOutput[yp][xp] = LUMINANCE.charAt(luminanceIndex);
                    }
                }
            }
        }

        StringBuilder frame = new StringBuilder("\u001b[H");
        for (int i = 0; i < SCREEN_HEIGHT; i++) {
            for (int j = 0; j < SCREEN_WIDTH; j++) {
                frame.append(charOutput[i][j]);
            }
            frame.append('\n');
        }
        System.out.print(frame);
        System.out.flush();
    }
}
