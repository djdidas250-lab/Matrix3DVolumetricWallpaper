package c.m
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun compileShader(t: Int, s: String): Int {
    val id = GLES30.glCreateShader(t)
    GLES30.glShaderSource(id, s)
    GLES30.glCompileShader(id)
    return id
}

fun createInstancedProgram(): Int {
    val vs = "#version 300 es\nuniform mat4 uVP;uniform float uSc;layout(location=0)in vec3 aP;layout(location=1)in vec3 aN;layout(location=2)in vec4 iPos;layout(location=3)in vec4 iCol;out vec3 vN;out vec4 vC;void main(){float a=iPos.w;float cs=cos(a);float sn=sin(a);vec3 sp=aP*uSc;vec3 p=vec3(sp.x*cs-sp.z*sn,sp.y,sp.x*sn+sp.z*cs);vN=vec3(aN.x*cs-aN.z*sn,aN.y,aN.x*sn+aN.z*cs);vC=iCol;gl_Position=uVP*vec4(p+iPos.xyz,1.0);}"
    val fs = "#version 300 es\nprecision mediump float;uniform float uBl;in vec3 vN;in vec4 vC;out vec4 oC;void main(){vec3 n=normalize(vN);float df=max(dot(n,normalize(vec3(0.4,0.7,1.0))),0.28);float rim=uBl*(1.0-abs(n.z));oC=vec4(vC.rgb*(df+rim),vC.a);}"
    val pr = GLES30.glCreateProgram()
    GLES30.glAttachShader(pr, compileShader(GLES30.GL_VERTEX_SHADER, vs))
    GLES30.glAttachShader(pr, compileShader(GLES30.GL_FRAGMENT_SHADER, fs))
    GLES30.glLinkProgram(pr)
    return pr
}

fun buildGlyphVbo(rw: IntArray): Pair<Int, Int> {
    val v = ArrayList<Float>(); val s = 0.09f; val d = 0.13f
    fun q(a: Float, b: Float, c: Float, e: Float, f: Float, g: Float, h: Float, i: Float, j: Float, k: Float, l: Float, m: Float, nx: Float, ny: Float, nz: Float) {
        for (z in floatArrayOf(a,b,c,nx,ny,nz, e,f,g,nx,ny,nz, h,i,j,nx,ny,nz, a,b,c,nx,ny,nz, h,i,j,nx,ny,nz, k,l,m,nx,ny,nz)) v.add(z)
    }
    for (r in 0..6) for (c in 0..4) if ((rw.get(r) and (1 shl (4 - c))) != 0) {
        val x0 = (c - 2.5f) * s; val x1 = x0 + s * 0.92f; val y1 = (3.5f - r) * s; val y0 = y1 - s * 0.92f
        q(x0,y0,d, x1,y0,d, x1,y1,d, x0,y1,d, 0f,0f,1f); q(x1,y0,-d, x0,y0,-d, x0,y1,-d, x1,y1,-d, 0f,0f,-1f)
        q(x0,y0,-d, x0,y0,d, x0,y1,d, x0,y1,-d, -1f,0f,0f); q(x1,y0,d, x1,y0,-d, x1,y1,-d, x1,y1,d, 1f,0f,0f)
        q(x0,y1,d, x1,y1,d, x1,y1,-d, x0,y1,-d, 0f,1f,0f); q(x0,y0,-d, x1,y0,-d, x1,y0,d, x0,y0,d, 0f,-1f,0f)
    }
    val bb = ByteBuffer.allocateDirect(v.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    for (z in v) bb.put(z); bb.position(0)
    val vbo = IntArray(1); GLES30.glGenBuffers(1, vbo, 0)
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo.get(0))
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, v.size * 4, bb, GLES30.GL_STATIC_DRAW)
    return Pair(vbo.get(0), v.size / 6)
}
