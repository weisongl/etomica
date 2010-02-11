package etomica.models.nitrogen;

import etomica.api.IVectorMutable;
import etomica.space.ISpace;
import etomica.space.Space;
import etomica.space3d.RotationTensor3D;
import etomica.space3d.Tensor3D;
import etomica.units.Degree;

public class TestRotationVector {

	public TestRotationVector(){
		space = Space.getInstance(3);
		axis = new IVectorMutable[3];
		for (int i=0; i<3; i++){
			axis[i] = space.makeVector();
		}
		
		axis[0].E(new double[]{1.0, 0.0, 0.0});
		axis[1].E(new double[]{0.0, 1.0, 0.0});
		axis[0].normalize();
		axis[1].normalize();
		
		axis[2].E(new double[]{0.0, 0.0, 1.0});
		axis[2].normalize();
		u = new double[2];
		
		tensor = new Tensor3D(new double[][]{{1.0, 0.0, 0.0},{0.0, 1.0, 0.0},{0.0, 0.0, 1.0}});
		rotation = new RotationTensor3D();
		rotation.E(tensor);
		rotationAxis = space.makeVector();
	}
	
	public double[] calcU(IVectorMutable vector){
		
		double u3 = vector.dot(axis[1]);
		double u4 = vector.dot(axis[2]);
		double ratio = Math.abs(u3/u4);
		
		double a = vector.dot(axis[0]);
		double theta = Math.acos(a);
		double sintheta = Math.sin(theta);
		
		   if(u4 == 0.0){
               u[0] = Math.sqrt(2*(1-Math.cos(theta)));
               u[1] = u4;
		   } else {
               if(u4 < 0.0){
            	   System.out.println("BAd1");
                       u[1] = -Math.sqrt(2*(1-Math.cos(theta))/(ratio*ratio+1));
               } else {
            	   System.out.println("BAd2");
                       u[1] = Math.sqrt(2*(1-Math.cos(theta))/(ratio*ratio+1));
               }

               if (u3 < 0.0){
            	   System.out.println("BAd10");
                       u[0] = -ratio*Math.sqrt(2*(1-Math.cos(theta))/(ratio*ratio+1));
               } else {
            	   System.out.println("BAd11");
                       u[0] = ratio*Math.sqrt(2*(1-Math.cos(theta))/(ratio*ratio+1));
               }
       }


//		if(vector.dot(axis[0]) < 0.0){
//			u[0] = u[0];
//			u[1] = u[1];
//		}
		
		return u; 
	}
	
	public void setToU(double[] u, IVectorMutable r){
		
		double angle = Math.acos(r.dot(axis[0]));
		rotationAxis.E(r);
		rotationAxis.XE(axis[0]);
		rotationAxis.normalize();
		
		
		rotation.setRotationAxis(rotationAxis, angle);
		rotation.transform(r);
		r.normalize();
		System.out.println("Back to initial nominal position: "+ r.toString());
		
		double theta = Math.acos(1 - (u[0]*u[0] + u[1]*u[1])*0.5);

		//System.out.println("setToU theta^2: " + (theta*theta));
		
		r.E(0.0);
		r.PEa1Tv1(u[0], axis[1]);
		r.PEa1Tv1(u[1], axis[2]);
		r.normalize();
		
		rotationAxis.E(r);
		rotationAxis.XE(axis[0]);
		rotationAxis.normalize();
		rotation.setRotationAxis(rotationAxis, (Math.PI/2-theta));
		rotation.transform(r);		
		System.out.println("Set to destination: " + r.toString());
		
	}
	
	public static void main (String[] args){
		TestRotationVector testVector = new TestRotationVector();
		
		IVectorMutable rVector = testVector.space.makeVector();
		rVector.E(new double[]{-1.0, -2.0, 1.0});
		rVector.normalize();
		System.out.println("Initial position: " + rVector.toString());
		//System.out.println("cos(alpha)^2: "+Math.pow(rVector.dot(testVector.axis[1]),2.0)+" ; cos(beta)^2: "+Math.pow(rVector.dot(testVector.axis[2]), 2.0));
		System.out.println("theta^2:    "+Math.acos(rVector.dot(testVector.axis[0]))*Math.acos(rVector.dot(testVector.axis[0])));
		double[] u = testVector.calcU(rVector);
		for (int i=0; i<u.length; i++){
			System.out.println("u["+i+"]^2: "+u[i]*u[i]);
		}
		System.out.println();
		
		
		testVector.setToU(u, rVector);
		
		
		
		
		
		
//		double rdotx = rVector.dot(testVector.axis[0]);
//		double rdoty = rVector.dot(testVector.axis[1]);
//		double rdotz = rVector.dot(testVector.axis[2]);
//		
//		double angleX = Math.acos(rdotx);
//		double angleY = Math.acos(rdoty);
//		double angleZ = Math.acos(rdotz);
//		double thetaX = Math.asin(Math.sqrt(rdoty*rdoty + rdotz*rdotz)); 
//		
//		double ratio = rdoty/rdotz;
//		double u3 = ratio*Math.sqrt(Math.sin(angleX)*Math.sin(thetaX)/(ratio*ratio +1));
//		double u4 = Math.sqrt(Math.sin(angleX)*Math.sin(thetaX)/(ratio*ratio +1));
//		
//		
//		System.out.println("rVector: "+ rVector.toString());
//		System.out.println("rdotx: "+rdotx+" "+Degree.UNIT.fromSim(angleX));
//		System.out.println("rdoty: "+rdoty+" "+Degree.UNIT.fromSim(angleY));
//		System.out.println("rdotz: "+rdotz+" "+Degree.UNIT.fromSim(angleZ));
//		System.out.println("thetaX: "+Degree.UNIT.fromSim(thetaX));
//		System.out.println("u3: "+u3+" ;u4: "+ u4);
		
		
		
	}
	
	protected double[] u;
	protected IVectorMutable[] axis;
	protected ISpace space;
	protected Tensor3D tensor;
	protected RotationTensor3D rotation;
	protected IVectorMutable rotationAxis;
	
	
	
}