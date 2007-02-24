package etomica.space;

/**
 * Implementation of ICoordinate interface in which
 * the only atom state parameter is its position, which
 * is represented by a Vector from an arbitrary-dimension
 * Space.
 */

public class Coordinate implements ICoordinate, java.io.Serializable {

    /**
     * Makes the coordinate vector using the given Space.
     */
    public Coordinate(Space space) {
        r = space.makeVector();
    }
    
    
    /**
     * Set this coordinate's parameters equal to those of the
     * given coordinate.
     */
    public void E(ICoordinate coord) {
        r.E(coord.getPosition());
    }

    /**
     * Returns the position vector (not a copy).
     */
    public final Vector getPosition() {
        return r;
    }

    protected final Vector r;
    private static final long serialVersionUID = 1L;
}
