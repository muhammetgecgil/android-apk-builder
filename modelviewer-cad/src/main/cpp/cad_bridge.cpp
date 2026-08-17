#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <cstdio>
#include <cctype>
#include <STEPControl_Reader.hxx>
#include <IGESControl_Reader.hxx>
#include <BRepTools.hxx>
#include <BRep_Builder.hxx>
#include <BRepMesh_IncrementalMesh.hxx>
#include <BRep_Tool.hxx>
#include <TopExp_Explorer.hxx>
#include <TopAbs.hxx>
#include <TopoDS.hxx>
#include <TopoDS_Shape.hxx>
#include <TopoDS_Face.hxx>
#include <TopoDS_Edge.hxx>
#include <Poly_Triangulation.hxx>
#include <TopLoc_Location.hxx>
#include <Bnd_Box.hxx>
#include <BRepBndLib.hxx>
#include <BRepAdaptor_Curve.hxx>
#include <BRepAdaptor_Surface.hxx>
#include <GeomAbs_CurveType.hxx>
#include <GeomAbs_SurfaceType.hxx>
#include <IFSelect_ReturnStatus.hxx>

static TopoDS_Shape gShape;
static std::vector<float> gTriangles;
static std::string gInfo;

static std::string lowerS(std::string s){for(char&c:s)c=(char)std::tolower((unsigned char)c);return s;}
static bool ends(const std::string&s,const std::string&e){return s.size()>=e.size()&&s.compare(s.size()-e.size(),e.size(),e)==0;}

static bool bounds(double&xmin,double&ymin,double&zmin,double&xmax,double&ymax,double&zmax){
 if(gShape.IsNull())return false; Bnd_Box b;BRepBndLib::Add(gShape,b);if(b.IsVoid())return false;b.Get(xmin,ymin,zmin,xmax,ymax,zmax);return true;
}

static void tessellate(){
 gTriangles.clear();if(gShape.IsNull())return;BRepMesh_IncrementalMesh mesher(gShape,0.20,Standard_False,0.30,Standard_True);
 double xmin,ymin,zmin,xmax,ymax,zmax;if(!bounds(xmin,ymin,zmin,xmax,ymax,zmax))return;double cx=(xmin+xmax)/2,cy=(ymin+ymax)/2,cz=(zmin+zmax)/2;double sc=std::max({xmax-xmin,ymax-ymin,zmax-zmin})/2.0;if(sc<1e-9)sc=1;
 for(TopExp_Explorer ex(gShape,TopAbs_FACE);ex.More();ex.Next()){
  TopoDS_Face face=TopoDS::Face(ex.Current());TopLoc_Location loc;Handle(Poly_Triangulation) tri=BRep_Tool::Triangulation(face,loc);if(tri.IsNull())continue;gp_Trsf tr=loc.Transformation();
  for(int i=1;i<=tri->NbTriangles();++i){int a,b,c;tri->Triangle(i).Get(a,b,c);int ids[3]={a,b,c};for(int k=0;k<3;k++){gp_Pnt p=tri->Node(ids[k]).Transformed(tr);gTriangles.push_back((float)((p.X()-cx)/sc));gTriangles.push_back((float)((p.Y()-cy)/sc));gTriangles.push_back((float)((p.Z()-cz)/sc));}}
 }
 int faces=0,edgesN=0,circles=0,cyls=0;for(TopExp_Explorer ex(gShape,TopAbs_FACE);ex.More();ex.Next()){faces++;try{BRepAdaptor_Surface s(TopoDS::Face(ex.Current()));if(s.GetType()==GeomAbs_Cylinder)cyls++;}catch(...){}}
 for(TopExp_Explorer ex(gShape,TopAbs_EDGE);ex.More();ex.Next()){edgesN++;try{BRepAdaptor_Curve c(TopoDS::Edge(ex.Current()));if(c.GetType()==GeomAbs_Circle)circles++;}catch(...){}}
 char buf[512];std::snprintf(buf,sizeof(buf),"CAD %.3f x %.3f x %.3f mm | yuz %d | kenar %d | dairesel %d | silindir %d",xmax-xmin,ymax-ymin,zmax-zmin,faces,edgesN,circles,cyls);gInfo=buf;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_muhammetgecgil_modelviewer_CadNative_loadCad(JNIEnv*env,jclass,jstring js){const char*p=env->GetStringUTFChars(js,nullptr);std::string path=p;env->ReleaseStringUTFChars(js,p);std::string l=lowerS(path);gShape.Nullify();try{if(ends(l,".step")||ends(l,".stp")){STEPControl_Reader r;if(r.ReadFile(path.c_str())!=IFSelect_RetDone)return JNI_FALSE;r.TransferRoots();gShape=r.OneShape();}else if(ends(l,".iges")||ends(l,".igs")){IGESControl_Reader r;if(r.ReadFile(path.c_str())!=IFSelect_RetDone)return JNI_FALSE;r.TransferRoots();gShape=r.OneShape();}else if(ends(l,".brep")){BRep_Builder b;if(!BRepTools::Read(gShape,path.c_str(),b))return JNI_FALSE;}else return JNI_FALSE;if(gShape.IsNull())return JNI_FALSE;tessellate();return gTriangles.empty()?JNI_FALSE:JNI_TRUE;}catch(...){gShape.Nullify();gTriangles.clear();return JNI_FALSE;}}
extern "C" JNIEXPORT jfloatArray JNICALL Java_com_muhammetgecgil_modelviewer_CadNative_getTriangles(JNIEnv*env,jclass){jfloatArray a=env->NewFloatArray((jsize)gTriangles.size());if(!gTriangles.empty())env->SetFloatArrayRegion(a,0,(jsize)gTriangles.size(),gTriangles.data());return a;}
extern "C" JNIEXPORT jstring JNICALL Java_com_muhammetgecgil_modelviewer_CadNative_getModelInfo(JNIEnv*env,jclass){return env->NewStringUTF(gInfo.c_str());}
extern "C" JNIEXPORT jstring JNICALL Java_com_muhammetgecgil_modelviewer_CadNative_autoDimensions(JNIEnv*env,jclass){if(gShape.IsNull())return env->NewStringUTF("Model yok");double xmin,ymin,zmin,xmax,ymax,zmax;if(!bounds(xmin,ymin,zmin,xmax,ymax,zmax))return env->NewStringUTF("Olcu yok");std::vector<double>rs;for(TopExp_Explorer ex(gShape,TopAbs_EDGE);ex.More();ex.Next())try{BRepAdaptor_Curve c(TopoDS::Edge(ex.Current()));if(c.GetType()==GeomAbs_Circle)rs.push_back(c.Circle().Radius());}catch(...){}char buf[1024];int n=std::snprintf(buf,sizeof(buf),"X=%.3f mm  Y=%.3f mm  Z=%.3f mm",xmax-xmin,ymax-ymin,zmax-zmin);for(size_t i=0;i<rs.size()&&i<10&&n<(int)sizeof(buf)-40;i++)n+=std::snprintf(buf+n,sizeof(buf)-n,"  Ø%.3f mm",2*rs[i]);return env->NewStringUTF(buf);}
extern "C" JNIEXPORT void JNICALL Java_com_muhammetgecgil_modelviewer_CadNative_clearModel(JNIEnv*,jclass){gShape.Nullify();gTriangles.clear();gInfo.clear();}
