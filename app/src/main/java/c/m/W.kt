package c.m

import android.opengl.GLES20
import android.opengl.Matrix
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import kotlin.math.max
import kotlin.random.Random

class W : WallpaperService() {
    override fun onCreateEngine(): Engine = E()

    inner class E : Engine() {
        private var t: T? = null

        override fun onVisibilityChanged(v: Boolean) {
            if (v) { if (t == null) t = T(surfaceHolder).apply { start() } }
            else { t?.r = false; t?.join(400); t = null }
        }

        override fun onSurfaceDestroyed(h: SurfaceHolder) {
            super.onSurfaceDestroyed(h); t?.r = false; t?.join(400); t = null
        }

        private inner class T(val h: SurfaceHolder) : Thread() {
            @Volatile var r = true
            val p = getSharedPreferences("m", 0)
            val g = arrayOf(
                intArrayOf(14,17,19,21,25,17,14), intArrayOf(4,12,4,4,4,4,14),
                intArrayOf(31,1,2,12,4,4,4), intArrayOf(4,31,5,5,9,17,19),
                intArrayOf(31,4,4,4,4,8,16), intArrayOf(17,17,17,17,17,18,20),
                intArrayOf(31,4,4,4,4,4,31), intArrayOf(8,8,14,9,8,8,8)
            )

            override fun run() {
                val e = EGLContext.getEGL() as EGL10
                val d = e.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
                e.eglInitialize(d, IntArray(2))
                val cf = arrayOfNulls<EGLConfig>(1)
                e.eglChooseConfig(d, intArrayOf(EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE), cf, 1, IntArray(1))
                val cfg = cf.get(0)
                val c = e.eglCreateContext(d, cfg, EGL10.EGL_NO_CONTEXT, intArrayOf(0x3098, 2, EGL10.EGL_NONE))
                val s = e.eglCreateWindowSurface(d, cfg, h, null)
                e.eglMakeCurrent(d, s, s, c)

                val vs = "uniform mat4 uM,uP;attribute vec4 aP;attribute vec3 aN;varying vec3 vN;void main(){vN=mat3(uM)*aN;gl_Position=uP*aP;}"
                val fs = "precision mediump float;uniform vec3 uC;uniform float uA;varying vec3 vN;void main(){gl_FragColor=vec4(uC*max(dot(normalize(vN),normalize(vec3(0.4,0.7,1.0))),0.25),uA);}"
                val pr = GLES20.glCreateProgram().also {
                    GLES20.glAttachShader(it, sh(GLES20.GL_VERTEX_SHADER, vs))
                    GLES20.glAttachShader(it, sh(GLES20.GL_FRAGMENT_SHADER, fs))
                    GLES20.glLinkProgram(it)
                }

                val ms = g.map { bm(it) }
                class Col(var x: Float, var y: Float, var z: Float, var spd: Float, var len: Int, var spin: Float, val ch: IntArray)
                val cols = ArrayList<Col>()

                GLES20.glEnable(GLES20.GL_DEPTH_TEST)
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

                val pm = FloatArray(16); val vm = FloatArray(16); val mm = FloatArray(16); val mvp = FloatArray(16); val vp = FloatArray(16)
                val uM = GLES20.glGetUniformLocation(pr, "uM"); val uP = GLES20.glGetUniformLocation(pr, "uP")
                val uC = GLES20.glGetUniformLocation(pr, "uC"); val uA = GLES20.glGetUniformLocation(pr, "uA")
                val aP = GLES20.glGetAttribLocation(pr, "aP"); val aN = GLES20.glGetAttribLocation(pr, "aN")

                while (r) {
                    val t0 = System.nanoTime()
                    val fr = h.surfaceFrame
                    val w = max(fr.width(), 1); val ht = max(fr.height(), 1)
                    GLES20.glViewport(0, 0, w, ht)
                    GLES20.glClearColor(0f, 0.015f, 0.008f, 1f)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

                    val dn = p.getInt("dn", 45)
                    while (cols.size < dn) cols.add(Col((Random.nextFloat() - 0.5f) * 14f, Random.nextFloat() * 24f - 6f, -Random.nextFloat() * 12f - 1f, 0.7f + Random.nextFloat() * 0.8f, 8 + Random.nextInt(10), Random.nextFloat() * 360f, IntArray(18) { Random.nextInt(ms.size) }))
                    while (cols.size > dn) cols.removeAt(cols.lastIndex)
                    cols.sortBy { it.z }

                    Matrix.perspectiveM(pm, 0, 50f, w.toFloat() / ht, 0.5f, 35f)
                    Matrix.setLookAtM(vm, 0, 0f, 0f, 8f, 0f, 0f, -4f, 0f, 1f, 0f)
                    Matrix.multiplyMM(vp, 0, pm, 0, vm, 0)

                    GLES20.glUseProgram(pr)
                    GLES20.glEnableVertexAttribArray(aP)
                    GLES20.glEnableVertexAttribArray(aN)

                    val sp = p.getInt("sp", 12) * 0.012f
                    val sc = p.getInt("sz", 42) * 0.014f
                    val step = sc * 1.35f
                    val base = when (p.getInt("th", 0) % 6) {
                        1 -> floatArrayOf(0f, 0.85f, 1f); 2 -> floatArrayOf(1f, 0.15f, 0.2f)
                        3 -> floatArrayOf(1f, 0.8f, 0.1f); 4 -> floatArrayOf(0.8f, 0.2f, 1f)
                        5 -> floatArrayOf(0.4f, 1f, 0.85f); else -> floatArrayOf(0.05f, 1f, 0.35f)
                    }

                    for (col in cols) {
                        col.y -= sp * col.spd
                        col.spin += 1.4f
                        if (Random.nextFloat() < 0.08f) col.ch.set(Random.nextInt(col.len), Random.nextInt(ms.size))
                        if (col.y + col.len * step < -11f) { col.y = 12f; col.x = (Random.nextFloat() - 0.5f) * 14f; col.z = -Random.nextFloat() * 12f - 1f }
                        for (i in 0 until col.len) {
                            val gy = col.y + i * step
                            if (gy > 12.5f || gy  {
                val v = ArrayList<Float>(); val s = 0.16f; val d = 0.22f
                fun q(a:Float,b:Float,c:Float,e:Float,f:Float,g:Float,h:Float,i:Float,j:Float,k:Float,l:Float,m:Float,nx:Float,ny:Float,nz:Float) {
                    for (z in floatArrayOf(a,b,c,nx,ny,nz,e,f,g,nx,ny,nz,h,i,j,nx,ny,nz,a,b,c,nx,ny,nz,h,i,j,nx,ny,nz,k,l,m,nx,ny,nz)) v.add(z)
                }
                for (row in 0..6) for (colIdx in 0..4) if ((rw.get(row) and (1 shl (4 - colIdx))) != 0) {
                    val x0 = (colIdx - 2.5f) * s; val x1 = x0 + s * 0.92f
                    val y1 = (3.5f - row) * s; val y0 = y1 - s * 0.92f
                    q(x0,y0,d,x1,y0,d,x1,y1,d,x0,y1,d,0f,0f,1f); q(x1,y0,-d,x0,y0,-d,x0,y1,-d,x1,y1,-d,0f,0f,-1f)
                    q(x0,y0,-d,x0,y0,d,x0,y1,d,x0,y1,-d,-1f,0f,0f); q(x1,y0,d,x1,y0,-d,x1,y1,-d,x1,y1,d,1f,0f,0f)
                    q(x0,y1,d,x1,y1,d,x1,y1,-d,x0,y1,-d,0f,1f,0f); q(x0,y0,-d,x1,y0,-d,x1,y0,d,x0,y0,d,0f,-1f,0f)
                }
                val bb = ByteBuffer.allocateDirect(v.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                for (z in v) bb.put(z)
                bb.position(0)
                return Pair(bb, v.size / 6)
            }
        }
    }
}
