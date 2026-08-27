#include <jni.h>
#include <STEPControl_Reader.hxx>
#include <IGESControl_Reader.hxx>
#include <BRepTools.hxx>
#include <BRep_Builder.hxx>
#include <BRepMesh_IncrementalMesh.hxx>
#include <BRep_Tool.hxx>
#include <TopExp_Explorer.hxx>
#include <TopoDS.hxx>
#include <TopoDS_Face.hxx>
#include <TopoDS_Shape.hxx>
#include <TopLoc_Location.hxx>
#include <Poly_Triangulation.hxx>
#include <Interface_Static.hxx>
#include <IFSelect_ReturnStatus.hxx>
#include <gp_Pnt.hxx>
#include <sstream>
#include <string>
#include <vector>

namespace {
struct CadDoc { TopoDS_Shape shape; std::string format; };

std::string jstr(JNIEnv* env, jstring s){
    if(!s) return {};
    const char* p=env->GetStringUTFChars(s,nullptr);
    std::string out=p?p:"";
    if(p) env->ReleaseStringUTFChars(s,p);
    return out;
}

TopoDS_Shape readShape(const std::string& path,const std::string& format){
    // Normalize all transferred CAD geometry to millimetres. This means the tessellated
    // coordinates have an authoritative 0.001 m/source-unit scale regardless of the
    // original STEP/IGES unit declaration; OCCT performs the source-unit conversion.
    Interface_Static::SetCVal("xstep.cascade.unit","MM");
    Interface_Static::SetCVal("xiges.cascade.unit","MM");
    if(format=="STEP"){
        STEPControl_Reader r;
        if(r.ReadFile(path.c_str())!=IFSelect_RetDone) return {};
        if(r.TransferRoots()<=0) return {};
        return r.OneShape();
    }
    if(format=="IGES"){
        IGESControl_Reader r;
        if(r.ReadFile(path.c_str())!=IFSelect_RetDone) return {};
        if(r.TransferRoots()<=0) return {};
        return r.OneShape();
    }
    if(format=="BREP"){
        BRep_Builder b; TopoDS_Shape s;
        if(!BRepTools::Read(s,path.c_str(),b)) return {};
        return s;
    }
    return {};
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_mg_structuralai_NativeOcctBridge_importCadFile(JNIEnv* env,jclass,jstring jpath,jstring jformat){
    try{
        std::string path=jstr(env,jpath), format=jstr(env,jformat);
        TopoDS_Shape shape=readShape(path,format);
        if(shape.IsNull()) return 0;
        auto* d=new CadDoc(); d->shape=shape; d->format=format;
        return reinterpret_cast<jlong>(d);
    }catch(...){return 0;}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mg_structuralai_NativeOcctBridge_assemblyTreeJson(JNIEnv* env,jclass,jlong handle){
    auto* d=reinterpret_cast<CadDoc*>(handle);
    if(!d) return env->NewStringUTF("{}");
    int faces=0; for(TopExp_Explorer ex(d->shape,TopAbs_FACE);ex.More();ex.Next()) faces++;
    std::ostringstream s; s<<"{\"source\":\"OCCT\",\"faces\":"<<faces<<"}";
    return env->NewStringUTF(s.str().c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mg_structuralai_NativeOcctBridge_metadataJson(JNIEnv* env,jclass,jlong handle){
    auto* d=reinterpret_cast<CadDoc*>(handle);
    if(!d) return env->NewStringUTF("{}");
    std::ostringstream s;
    s<<"{\"format\":\""<<d->format<<"\",\"normalizedUnit\":\"mm\",\"unitScaleM\":0.001,\"unitAuthoritative\":true}";
    return env->NewStringUTF(s.str().c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mg_structuralai_NativeOcctBridge_validityReportJson(JNIEnv* env,jclass,jlong handle){
    auto* d=reinterpret_cast<CadDoc*>(handle);
    const char* out=(d&&!d->shape.IsNull())?"{\"shapePresent\":true}":"{\"shapePresent\":false}";
    return env->NewStringUTF(out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mg_structuralai_NativeOcctBridge_tessellateToObj(JNIEnv* env,jclass,jlong handle,jdouble linearDeflection,jdouble angularDeflection){
    auto* d=reinterpret_cast<CadDoc*>(handle);
    if(!d||d->shape.IsNull()) return env->NewStringUTF("");
    try{
        BRepMesh_IncrementalMesh mesh(d->shape,linearDeflection,false,angularDeflection,true);
        mesh.Perform();
        std::ostringstream out;
        long vertexOffset=0;
        for(TopExp_Explorer ex(d->shape,TopAbs_FACE);ex.More();ex.Next()){
            TopoDS_Face face=TopoDS::Face(ex.Current());
            TopLoc_Location loc;
            Handle(Poly_Triangulation) tri=BRep_Tool::Triangulation(face,loc);
            if(tri.IsNull()) continue;
            const gp_Trsf tr=loc.Transformation();
            for(int i=1;i<=tri->NbNodes();++i){
                gp_Pnt p=tri->Node(i).Transformed(tr);
                out<<"v "<<p.X()<<" "<<p.Y()<<" "<<p.Z()<<"\n";
            }
            for(int i=1;i<=tri->NbTriangles();++i){
                Standard_Integer a,b,c; tri->Triangle(i).Get(a,b,c);
                if(face.Orientation()==TopAbs_REVERSED) std::swap(b,c);
                out<<"f "<<(vertexOffset+a)<<" "<<(vertexOffset+b)<<" "<<(vertexOffset+c)<<"\n";
            }
            vertexOffset+=tri->NbNodes();
        }
        return env->NewStringUTF(out.str().c_str());
    }catch(...){return env->NewStringUTF("");}
}

extern "C" JNIEXPORT void JNICALL
Java_com_mg_structuralai_NativeOcctBridge_release(JNIEnv*,jclass,jlong handle){delete reinterpret_cast<CadDoc*>(handle);}
