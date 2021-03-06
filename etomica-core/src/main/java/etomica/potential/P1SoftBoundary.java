/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.potential;

import etomica.api.IAtomList;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.space.ISpace;
import etomica.space.Tensor;
import etomica.units.Dimension;
import etomica.units.Length;

/**
 * @author David Kofke
 *
 * Inverse-power potential between an atom and all four boundaries of the box.  Potential
 * is of the form <tt>u(r) = (R/r)^12</tt>, where <tt>R</tt> is the repulsion radius.  This
 * term is summed over all four boundaries.
 */
public class P1SoftBoundary extends Potential1 implements PotentialSoft {

    private static final long serialVersionUID = 1L;
	private final IVectorMutable[] gradient;
	private double radius;
	
    public P1SoftBoundary(ISpace space) {
        this(space, 0.5);
    }
	public P1SoftBoundary(ISpace space, double radius) {
		super(space);
        gradient = new IVectorMutable[1];
		gradient[0] = space.makeVector();
		setRadius(radius);
	}

	public double energy(IAtomList a) {
		IVector dimensions = boundary.getBoxSize();
		double rx = a.getAtom(0).getPosition().getX(0);
		double ry = a.getAtom(0).getPosition().getX(1);
		double dx1 = (dimensions.getX(0) - rx);
		double dy1 = (dimensions.getX(1) - ry);
		return energy(rx) + energy(ry) + energy(dx1) + energy(dy1);		
	}//end of energy
	
	private double energy(double r) {
		r /= radius;
		double r2 = 1./(r*r);
		double r6 = r2*r2*r2;
		return r6*r6;
	}
	
	private double gradient(double r) {
		double rr = radius/r;
		double r2 = rr*rr;
		double r6 = r2*r2*r2;
		return -12*r6*r6/r;
	}
	
	public IVector[] gradient(IAtomList a) {
		IVector dimensions = boundary.getBoxSize();
		double rx = a.getAtom(0).getPosition().getX(0);
		double ry = a.getAtom(0).getPosition().getX(1);
		double dx1 = (dimensions.getX(0) - rx);
		double dy1 = (dimensions.getX(1) - ry);
		double gradx = gradient(rx) - gradient(dx1);
		double grady = gradient(ry) - gradient(dy1);
		gradient[0].setX(0,gradx);
		gradient[0].setX(1,grady);
		return gradient;
	}
    
    public IVector[] gradient(IAtomList a, Tensor pressureTensor) {
        return gradient(a);
    }
	
	public double virial(IAtomList atoms) {
	    return 0.0;
    }
    
	/**
	 * Returns the radius.
	 * @return double
	 */
	public double getRadius() {
		return radius;
	}

	/**
	 * Sets the radius.
	 * @param radius The radius to set
	 */
	public void setRadius(double radius) {
		this.radius = radius;
	}
    
    public Dimension getRadiusDimension() {
        return Length.DIMENSION;
    }
    
}
