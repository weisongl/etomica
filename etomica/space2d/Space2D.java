package etomica.space2d;

import java.io.ObjectStreamException;

import etomica.EtomicaInfo;
import etomica.Space;
import etomica.space.Boundary;

public final class Space2D extends Space {

    /**
     * Private constructor for singleton.
     */
    private Space2D() {
        super();
    }

    /**
     * @return Returns the instance.
     */
    public static Space2D getInstance() {
        return INSTANCE;
    }

    public final int D() {
        return 2;
    }

    public final int powerD(int n) {
        return n * n;
    }

    public final double powerD(double a) {
        return a * a;
    }

    /**
     * Returns the square root of the given value, a^(1/D) which is a^(1/2).
     */
    public double rootD(double a) {
        return Math.sqrt(a);
    }

    public double sphereVolume(double r) {
        return Math.PI * r * r;
    } //volume of a sphere of radius r

    public double sphereArea(double r) {
        return 2.0 * Math.PI * r;
    } //surface area of sphere of radius r (used for differential shell volume)

    public etomica.space.Vector makeVector() {
        return new Vector2D();
    }

    public etomica.space.Orientation makeOrientation() {
        return new Orientation();
    }

    public etomica.space.Tensor makeTensor() {
        return new Tensor2D();
    }

    public etomica.space.Tensor makeRotationTensor() {
        return new RotationTensor();
    }

    public int[] makeArrayD(int i) {
        return new int[] { i, i };
    }

    public double[] makeArrayD(double d) {
        return new double[] { d, d };
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Two-dimensional space");
        return info;
    }

    public static final double r2(Vector2D u1, Vector2D u2, Boundary b) {
        Vector2D.WORK.x = u1.x - u2.x;
        Vector2D.WORK.y = u1.y - u2.y;
        b.nearestImage(Vector2D.WORK);
        return Vector2D.WORK.x * Vector2D.WORK.x + Vector2D.WORK.y
                * Vector2D.WORK.y;
    }

    /**
     * Required to guarantee singleton when deserializing.
     * 
     * @return the singleton INSTANCE
     */
    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

    private static final Space2D INSTANCE = new Space2D();

}//end of Space2D
