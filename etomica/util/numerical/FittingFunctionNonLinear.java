package etomica.util.numerical;


/**
 * 
 * This class is coded for specific nonlinear model that decribes the Equation:
 *  f(x) = polynomialA + exp(-Kx)*polynomialB
 * 
 * M and N are the input parameters that determine the order of polynomialA
 *  and polynomialB respectively
 *  
 * polynomialA is given as: 
 *                M-1
 *  polynomialA = sum [ a_m * x^m ]
 *                m=0
 * 
 * for example, when M=3; polynomialA = a_0 + a_1 * x + a_2 * x^2
 * 
 * 
 * Note: polynomialB has same form as polynomialA 
 *               
 * @author Tai Boon Tan
 *
 */
public class FittingFunctionNonLinear{
	public FittingFunctionNonLinear(int M, int N){
		this.M = M;
		this.N = N;
	}

	public double f(double[] a, double x) {
		double sumPolyA = 0.0;
		double sumPolyB = 0.0;
		double expKxL = Math.exp(-a[M]*Math.pow(x,a[M+1]));

		//polynomialA
		double xMultiply = 1;
		for(int i=0; i<M; i++){
			if(i==0){
				sumPolyA += a[i]*(1-expKxL);
				
			} else{
				sumPolyA += a[i]*xMultiply;
			}
			xMultiply *= x;
		}
		
		//polynomialB
		xMultiply = 1;
		for(int i=(M+2); i<(M+N+2); i++){
			xMultiply *= x;
			sumPolyB += a[i]*xMultiply;
		
		}
		
		return sumPolyA + expKxL*sumPolyB;
		
	}

	/*
	 * d is derivatives
	 * a is array of parameters
	 * x is the x-value
	 */
	public double df(int d, double[] a, double x) {
		double dsumPolyB = 0.0;
		double expKxL = Math.exp(-a[M]*Math.pow(x, a[M+1]));
		double xMultiply;
		
		xMultiply = 1;
		
		if(d < M){
			for(int i=0; i< M; i++){
				if(d==0){
					return (1-expKxL);
				} else if(i==d){
					return xMultiply;
				}
				xMultiply *= x;
			}
			
		} else if((d >= M) && (d < M+2)){
			double coeff;
			if(d==M){
				coeff = -Math.pow(x,a[M+1])*expKxL;
			} else {
				coeff = -a[M]*Math.log(x)*Math.pow(x,a[M+1])*expKxL;
			}
			
			for(int i=M+2; i<M+N+2; i++){
				xMultiply *= x;
				dsumPolyB += a[i]*xMultiply;
				
			}
			
			return coeff*(dsumPolyB-a[0]);
			
		} 
		
		for(int i=(M+2); i<(M+N+2); i++){
			xMultiply *= x;
			
			if(d==i){
				return xMultiply*expKxL;	
			}
		}
		System.out.println("screw!");
		return Double.NaN;

	}
	
	protected int M, N;
}