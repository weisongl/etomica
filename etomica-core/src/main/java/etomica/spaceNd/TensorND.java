/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.spaceNd;

import Jama.Matrix;
import etomica.api.IFunction;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.space.Tensor;

public class TensorND implements Tensor {
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError(ex.toString());
        }
    }

    public TensorND(int dim) {
        this.dim = dim;
        this.x = new double[dim][dim];
    }

    public int D() {
        return dim;
    }

    public double component(int i, int j) {
        return x[i][j];
    }

    public void setComponent(int i, int j, double d) {
        x[i][j] = d;
    }

    public void E(Tensor u) {
        TensorND t = (TensorND)u;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] = t.x[i][j];
            }
        }
    }

    public void E(IVector[] v) {
        if(v.length != dim) {
            throw new IllegalArgumentException("Tensor requires " + dim + " vectors to set its values");
        }
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] = ((VectorND)v[i]).x[j];
            }
        }
    }

    public void assignTo(IVectorMutable[] v) {
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                ((VectorND)v[i]).x[j] = x[i][j];
            }
        }
    }

    public void Ev1v2(IVector v1, IVector v2) {
        VectorND u1 = (VectorND)v1;
        VectorND u2 = (VectorND)v2;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] = u1.x[i]*u2.x[j];
            }
        }
    }

    public void E(double a) {
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] = a;
            }
        }
    }

    public void PE(double a) {
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] += a;
            }
        }
    }

    public void PE(Tensor u) {
        TensorND t = (TensorND)u;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] += t.x[i][j];
            }
        }
    }

    public void PE(int i, int j, double a) {
        x[i][j] += a; 
    }

    public void PEv1v2(IVector v1, IVector v2) {
        VectorND u1 = (VectorND)v1;
        VectorND u2 = (VectorND)v2;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] += u1.x[i]*u2.x[j];
            }
        }
    }

    public void MEv1v2(IVector v1, IVector v2) {
        VectorND u1 = (VectorND)v1;
        VectorND u2 = (VectorND)v2;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] -= u1.x[i]*u2.x[j];
            }
        }
    }

    public void ME(Tensor u) {
        TensorND t = (TensorND)u;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] -= t.x[i][j];
            }
        }
    }

    public void PEa1Tt1(double a1, Tensor u1) {
        TensorND t1 = (TensorND)u1;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] += a1*t1.x[i][j];
            }
        }
    }

    public double trace() {
        double sum = 0;
        for(int i=0; i<dim; i++){
            sum += x[i][i];
        }

        return sum;
    }

    public void transpose() {
        for(int i=0; i<dim-1; i++){
            for(int j=i+1; j<dim; j++){
                double tmp = x[i][j];
                x[i][j] = x[j][i];
                x[j][i] = tmp;
            }
        }
    }

    public double determinant() {
        Matrix tmp = new Matrix(x);
        return tmp.det();
    }

    public void invert() {
        Matrix tmp = new Matrix(x);
        tmp = tmp.inverse();
        double[][] y = tmp.getArray();
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] = y[i][j];
            }
        }
    }

    public void TE(double a) {
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] *= a;
            }
        }
    }

    public void TE(Tensor u) {
        TensorND t = (TensorND)u;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] *= t.x[i][j];
            }
        }
    }

    public void DE(Tensor u) {
        TensorND t = (TensorND)u;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] /= t.x[i][j];
            }
        }
    }

    public double[] toArray() {
        return null;
    }

    public String toString() {
        String s = "";
        for(int i=0; i<dim; i++){
            s += "(";
            for(int j=0; j<dim-1; j++){
                s += x[i][j] + ",";
            }
            s += x[i][dim-1] + ")\n";
        }
        return s;
    }

    public boolean isNaN() {
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                if(Double.isNaN(x[i][j])){
                    return true;

                }
            }
        }
        return false;
    }

    public void map(IFunction f) {
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[i][j] = f.f(x[i][j]);
            }
        }
    }

    public void E(double[][] d) {
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                x[j][j] = d[i][j];
            }
        }
    }


    public void transform(IVectorMutable A) {
        VectorND tmp = new VectorND(dim);
        VectorND B = (VectorND)A;
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                tmp.x[i] += x[i][j]*B.x[j];
            }
        }
        A.E(tmp);
    }

    public void assignTo(double[] d) {
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                d[i*dim + j] =  x[i][j];
            }
        }
    }

    public void assignTo(double[][] d) {
        for(int i=0; i<dim; i++){
            for(int j=0; j<dim; j++){
                d[j][j] = x[i][j];

            }
        }
    }
    protected final int dim;
    protected final double[][] x;
}
