package c.m
import android.content.SharedPreferences;import android.opengl.GLES30;import android.opengl.Matrix;import android.view.SurfaceHolder;import java.nio.*;import javax.microedition.khronos.egl.*;import kotlin.math.*;import kotlin.random.Random

class Renderer3D(val h: SurfaceHolder, val p: SharedPreferences) : Thread() {
    @Volatile var r = true
    var tx = 0f; var ty = 0f; var px = -99f; var py = -99f; var pf = 0f
    val g = arrayOf(
        intArrayOf(14,17,19,21,25,17,14), intArrayOf(4,12,4,4,4,4,14), intArrayOf(31,1,2,12,4,4,4), intArrayOf(4,31,5,5,9,17,19),
        intArrayOf(31,4,4,4,4,8,16), intArrayOf(17,17,17,17,17,18,20), intArrayOf(31,4,4,4,4,4,31), intArrayOf(8,8,14,9,8,8,8),
        intArrayOf(14,17,1,6,8,16,31), intArrayOf(31,2,4,2,1,17,14), intArrayOf(4,31,17,17,1,2,12), intArrayOf(2,31,2,6,10,18,2)
    )
    override fun run() {
        val e = EGLContext.getEGL() as EGL10; val d = e.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY); e.eglInitialize(d, IntArray(2))
        val cf = arrayOfNulls<EGLConfig>(1)
        e.eglChooseConfig(d, intArrayOf(EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE), cf, 1, IntArray(1))
        val c = e.eglCreateContext(d, cf.get(0), EGL10.EGL_NO_CONTEXT, intArrayOf(0x3098, 3, EGL10.EGL_NONE))
        val s = e.eglCreateWindowSurface(d, cf.get(0), h, null); e.eglMakeCurrent(d, s, s, c)

        val vs = "#version 300 es\nuniform mat4 uVP;uniform float uSc;layout(location=0)in vec3 aP;layout(location=1)in vec3 aN;layout(location=2)in vec4 iPos;layout(location=3)in vec4 iCol;out vec3 vN;out vec4 vC;void main(){float a=iPos.w;float cs=cos(a);float sn=sin(a);vec3 sp=aP*uSc;vec3 p=vec3(sp.x*cs-sp.z*sn,sp.y,sp.x*sn+sp.z*cs);vN=vec3(aN.x*cs-aN.z*sn,aN.y,aN.x*sn+aN.z*cs);vC=iCol;gl_Position=uVP*vec4(p+iPos.xyz,1.0);}"
        val fs = "#version 300 es\nprecision mediump float;uniform float uBl;in vec3 vN;in vec4 vC;out vec4 oC;void main(){vec3 n=normalize(vN);float df=max(dot(n,normalize(vec3(0.4,0.7,1.0))),0.25);float rim=uBl*(1.0-abs(n.z));oC=vec4(vC.rgb*(df+rim),vC.a);}"
        val pr = GLES30.glCreateProgram().also { GLES30.glAttachShader(it, compileShader(GLES30.GL_VERTEX_SHADER, vs)); GLES30.glAttachShader(it, compileShader(GLES30.GL_FRAGMENT_SHADER, fs)); GLES30.glLinkProgram(it) }

        val ms = g.map { buildGlyphVbo(it) }
        class Col(var x: Float, var y: Float, var z: Float, var spd: Float, var len: Int, var spin: Float, val ch: IntArray)
        val cols = ArrayList<Col>()
        val instBuf = ByteBuffer.allocateDirect(2400 * 32).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val instVbo = IntArray(1); GLES30.glGenBuffers(1, instVbo, 0)
        val buckets = Array(ms.size) { ArrayList<Float>() }

        GLES30.glEnable(GLES30.GL_DEPTH_TEST); GLES30.glEnable(GLES30.GL_BLEND); GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        val pm = FloatArray(16); val vm = FloatArray(16); val vp = FloatArray(16)
        val uVP = GLES30.glGetUniformLocation(pr, "uVP"); val uBl = GLES30.glGetUniformLocation(pr, "uBl"); val uSc = GLES30.glGetUniformLocation(pr, "uSc")
        var tm = 0f

        while (r) {
            val t0 = System.nanoTime(); val fr = h.surfaceFrame; val w = max(fr.width(), 1); val ht = max(fr.height(), 1)
            GLES30.glViewport(0, 0, w, ht); GLES30.glClearColor(0f, 0.015f, 0.008f, 1f); GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

            val dn = p.getInt("dn", 85)
            while (cols.size < dn) cols.add(Col((Random.nextFloat() - 0.5f) * 15f, Random.nextFloat() * 24f - 6f, -Random.nextFloat() * 14f - 1f, 0.65f + Random.nextFloat() * 0.85f, 10 + Random.nextInt(10), Random.nextFloat() * 6.28f, IntArray(20) { Random.nextInt(ms.size) }))
            while (cols.size > dn) cols.removeAt(cols.lastIndex)

            val asp = w.toFloat() / ht
            val fov = if (p.getBoolean("f3", true) && asp > 0.75f) 65f else 50f
            Matrix.perspectiveM(pm, 0, fov, asp, 0.5f, 40f)
            val cx = if (p.getBoolean("f4", true)) tx * 2.8f else 0f
            val cy = if (p.getBoolean("f4", true)) -ty * 2.8f else 0f
            Matrix.setLookAtM(vm, 0, cx, cy, 8f, 0f, 0f, -4f, 0f, 1f, 0f)
            Matrix.multiplyMM(vp, 0, pm, 0, vm, 0)

            val sp = p.getInt("sp", 12) * 0.013f; val sc = p.getInt("sz", 42) / 42f; val step = 0.78f * sc
            val inten = (0.55f + 0.45f * (p.getInt("in", 65) / 100f)).coerceIn(0.2f, 1.3f)
            val blm = if (p.getBoolean("f1", true)) (p.getInt("bl", 35) / 100f) * 1.2f else 0f
            val vor = p.getBoolean("f2", true); val fly = p.getBoolean("f5", true)
            val th = p.getInt("th", 0) % 6
            tm += 0.02f; if (pf > 0f) pf *= 0.95f

            for (b in buckets) b.clear()
            for (col in cols) {
                col.y -= sp * col.spd; col.spin += 0.03f
                if (fly) col.z += sp * 0.35f
                if (Random.nextFloat() < 0.09f) col.ch.set(Random.nextInt(col.len), Random.nextInt(ms.size))
                if (col.y + col.len * step < -11.5f || col.z > 6.5f) { col.y = 12.5f; col.x = (Random.nextFloat() - 0.5f) * 15f; col.z = if (fly) -15f else -Random.nextFloat() * 14f - 1f }
                val rgb = when (th) {
                    1 -> floatArrayOf(0f, 0.85f * inten, 1f * inten); 2 -> floatArrayOf(1f * inten, 0.14f * inten, 0.2f * inten)
                    3 -> floatArrayOf(1f * inten, 0.78f * inten, 0.1f * inten); 4 -> floatArrayOf(0.8f * inten, 0.2f * inten, 1f * inten)
                    5 -> floatArrayOf((0.5f + 0.5f * sin(tm + col.x)) * inten, (0.5f + 0.5f * sin(tm + col.x + 2.1f)) * inten, (0.5f + 0.5f * sin(tm + col.x + 4.2f)) * inten)
                    else -> floatArrayOf(0.05f * inten, 1f * inten, 0.35f * inten)
                }
                for (i in 0 until col.len) {
                    val gy = col.y + i * step; if (gy > 13f || gy < -13f) continue
                    var gx = col.x; var fgy = gy
                    if (vor && pf > 0.05f) {
                        val dx = gx - (((px / w) - 0.5f) * 12f); val dy = fgy - ((0.5f - (py / ht)) * 18f)
                        val dist = sqrt(dx * dx + dy * dy) + 0.1f
                        if (dist < 4.5f) { val push = (4.5f - dist) * 0.35f * pf; gx += (dx / dist) * push - (dy / dist) * push * 0.5f; fgy += (dy / dist) * push + (dx / dist) * push * 0.5f }
                    }
                    val fade = 1f - (i.toFloat() / col.len)
                    val bkt = buckets.get(col.ch.get(i))
                    if (bkt.size < 2400 * 8) {
                        bkt.add(gx); bkt.add(fgy); bkt.add(col.z); bkt.add(col.spin + i * 0.22f)
                        if (i == 0) { bkt.add(0.9f); bkt.add(1f); bkt.add(0.92f); bkt.add(1f) }
                        else { bkt.add(rgb.get(0) * fade); bkt.add(rgb.get(1) * fade); bkt.add(rgb.get(2) * fade); bkt.add(fade) }
                    }
                }
            }

            GLES30.glUseProgram(pr)
            GLES30.glUniformMatrix4fv(uVP, 1, false, vp, 0); GLES30.glUniform1f(uBl, blm); GLES30.glUniform1f(uSc, sc)

            for (mIdx in ms.indices) {
                val bkt = buckets.get(mIdx); val cnt = bkt.size / 8; if (cnt == 0) continue
                instBuf.clear(); for (v in bkt) instBuf.put(v); instBuf.position(0)
                val mesh = ms.get(mIdx)
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mesh.first)
                GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 24, 0); GLES30.glVertexAttribDivisor(0, 0)
                GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 24, 12); GLES30.glVertexAttribDivisor(1, 0)

                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instVbo.get(0))
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, cnt * 32, instBuf, GLES30.GL_DYNAMIC_DRAW)
                GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, 32, 0); GLES30.glVertexAttribDivisor(2, 1)
                GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, 32, 16); GLES30.glVertexAttribDivisor(3, 1)

                GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, mesh.second, cnt)
            }

            e.eglSwapBuffers(d, s)
            val dt = (System.nanoTime() - t0) / 1000000L
            if (dt < 16L) sleep(16L - dt)
        }
        e.eglMakeCurrent(d, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT); e.eglDestroySurface(d, s); e.eglDestroyContext(d, c); e.eglTerminate(d)
    }
}
