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

fun buildGlyphVbo(rw: IntArray): Pair<Int, Int> {
    val v = ArrayList<Float>()
    val s = 0.09f
    val d = 0.13f
    fun q(a: Float, b: Float, c: Float, e: Float, f: Float, g: Float, h: Float, i: Float, j: Float, k: Float, l: Float, m: Float, nx: Float, ny: Float, nz: Float) {
        val pts = floatArrayOf(a,b,c,nx,ny,nz, e,f,g,nx,ny,nz, h,i,j,nx,ny,nz, a,b,c,nx,ny,nz, h,i,j,nx,ny,nz, k,l,m,nx,ny,nz)
        for (z in pts) v.add(z)
    }
    for (r in 0..6) for (c in 0..4) if ((rw.get(r) and (1 shl (4 - c))) != 0) {
        val x0 = (c - 2.5f) * s; val x1 = x0 + s * 0.92f
        val y1 = (3.5f - r) * s; val y0 = y1 - s * 0.92f
        q(x0,y0,d, x1,y0,d, x1,y1,d, x0,y1,d, 0f,0f,1f)
        q(x1,y0,-d, x0,y0,-d, x0,y1,-d, x1,y1,-d, 0f,0f,-1f)
        q(x0,y0,-d, x0,y0,d, x0,y1,d, x0,y1,-d, -1f,0f,0f)
        q(x1,y0,d, x1,y0,-d, x1,y1,-d, x1,y1,d, 1f,0f,0f)
        q(x0,y1,d, x1,y1,d, x1,y1,-d, x0,y1,-d, 0f,1f,0f)
        q(x0,y0,-d, x1,y0,-d, x1,y0,d, x0,y0,d, 0f,-1f,0f)
    }
    val bb = ByteBuffer.allocateDirect(v.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    for (z in v) bb.put(z); bb.position(0)
    val vbo = IntArray(1)
    GLES30.glGenBuffers(1, vbo, 0)
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo.get(0))
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, v.size * 4, bb, GLES30.GL_STATIC_DRAW)
    return Pair(vbo.get(0), v.size / 6)
}
