package com.muhammetgecgil.mg3dscanner;

final class Matrix {
    private Matrix() {}
    static void perspectiveM(float[] m,int offset,float fovy,float aspect,float zNear,float zFar){android.opengl.Matrix.perspectiveM(m,offset,fovy,aspect,zNear,zFar);}
    static void setIdentityM(float[] m,int offset){android.opengl.Matrix.setIdentityM(m,offset);}
    static void translateM(float[] m,int offset,float x,float y,float z){android.opengl.Matrix.translateM(m,offset,x,y,z);}
    static void rotateM(float[] m,int offset,float a,float x,float y,float z){android.opengl.Matrix.rotateM(m,offset,a,x,y,z);}
    static void multiplyMM(float[] r,int ro,float[] lhs,int lo,float[] rhs,int rr){android.opengl.Matrix.multiplyMM(r,ro,lhs,lo,rhs,rr);}
}
