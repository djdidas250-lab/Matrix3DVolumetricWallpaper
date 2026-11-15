package c.m
import android.content.SharedPreferences;import android.opengl.GLES30;import android.opengl.Matrix;import android.view.SurfaceHolder;import java.nio.*;import javax.microedition.khronos.egl.*;import kotlin.math.*;import kotlin.random.Random

class Renderer3D(val h: SurfaceHolder, val p: SharedPreferences) : Thread() {
    @Volatile var r = true; @Volatile var tx = 0f; @Volatile var ty = 0f; @Volatile var px = -99f; @Volatile var py = -99f; @Volatile var pf = 0f
    val g = arrayOf(intArrayOf(14,17,19,21,25,17,14),intArrayOf(4,12,4,4,4,4,14),intArrayOf(31,1,2,12,4,4,4),intArrayOf(4,31,5,5,9,17,19),intArrayOf(31,4,4,4,4,8,16),intArrayOf(17,17,17,17,17,18,20),intArrayOf(31,4,4,4,4,4,31),intArrayOf(8,8,14,9,8,8,8))
    override fun run() {
        val e = EGLContext.getEGL() as EGL10; val d = e.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY); e.eglInitialize(d, IntArray(2))
        val cf = arrayOfNulls<EGLConfig>(1)
        e.eglChooseConfig(d, intArrayOf(EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE), cf, 1, IntArray(1))
        val c = e.eglCreateContext(d, cf.get(0), EGL10.EGL_NO_CONTEXT, intArrayOf(0x3098, 3, EGL10.EGL_NONE))
        val s = e.eglCreateWindowSurface(d, cf.get(0), h, null); e.eglMakeCurrent(d, s, s, c)
        val pr = createInstancedProgram(); val ms = g.map { buildGlyphVbo(it) }
        val maxCols = 120
        val cxArr = FloatArray(maxCols) { (Random.nextFloat() - 0.5f) * 14f }; val cyArr = FloatArray(maxCols) { Random.nextFloat() * 24f - 6f }; val czArr = FloatArray(maxCols) { -Random.nextFloat() * 13f - 1f }
        val cspArr = FloatArray(maxCols) { 0.7f + Random.nextFloat() * 0.75f }; val clnArr = IntArray(maxCols) { 10 + Random.nextInt(8) }; val csnArr = FloatArray(maxCols) { Random.nextFloat() * 6.28f }
        val cchArr = IntArray(maxCols * 18) { Random.nextInt(ms.size) }
        val bktData = Array(ms.size) { FloatArray(320 * 8) }; val bktCnt = IntArray(ms.size)
        val instBuf = ByteBuffer.allocateDirect(320 * 32).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val instVbo = IntArray(1); GLES30.glGenBuffers(1, instVbo, 0)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST); GLES30.glEnable(GLES30.GL_BLEND); GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        val pm = FloatArray(16); val vm = FloatArray(16); val vp = FloatArray(16)
        val uVP = GLES30.glGetUniformLocation(pr, "uVP"); val uBl = GLES30.glGetUniformLocation(pr, "uBl"); val uSc = GLES30.glGetUniformLocation(pr, "uSc")
        var tm = 0f; var lastNs = System.nanoTime()

        while (r) {
            val now = System.nanoTime(); val dtSec = ((now - lastNs) / 1_000_000_000f).coerceIn(0.005f, 0.033f); lastNs = now
            val fr = h.surfaceFrame; val w = max(fr.width(), 1); val ht = max(fr.height(), 1)
            GLES30.glViewport(0, 0, w, ht); GLES30.glClearColor(0f, 0.015f, 0.008f, 1f); GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
            val dn = p.getInt("dn", 85).coerceIn(30, maxCols); val asp = w.toFloat() / ht
            val fov = if (p.getBoolean("f3", true) && asp > 0.75f) 65f else 50f
            Matrix.perspectiveM(pm, 0, fov, asp, 0.5f, 40f)
            val camX = if (p.getBoolean("f4", true)) tx * 2.5f else 0f; val camY = if (p.getBoolean("f4", true)) -ty * 2.5f else 0f
            Matrix.setLookAtM(vm, 0, camX, camY, 8f, 0f, 0f, -4f, 0f, 1f, 0f); Matrix.multiplyMM(vp, 0, pm, 0, vm, 0)

            val sp = p.getInt("sp", 12) * 0.48f * dtSec; val sc = p.getInt("sz", 42) / 42f; val step = 0.78f * sc
            val inten = (0.55f + 0.45f * (p.getInt("in", 65) / 100f)).coerceIn(0.2f, 1.3f)
            val blm = if (p.getBoolean("f1", true)) (p.getInt("bl", 35) / 100f) * 1.2f else 0f
            val vor = p.getBoolean("f2", true); val fly = p.getBoolean("f5", true); val th = p.getInt("th", 0) % 6
            tm += dtSec * 1.5f; if (pf > 0f) pf = (pf - dtSec * 1.8f).coerceAtLeast(0f)

            for (m in ms.indices) bktCnt.set(m, 0)
            val twX = ((px / w) - 0.5f) * 12f; val twY = (0.5f - (py / ht)) * 18f

            for (cIdx in 0 until dn) {
                var y = cyArr.get(cIdx) - sp * cspArr.get(cIdx); var z = czArr.get(cIdx) + (if (fly) sp * 0.35f else 0f)
                val spin = csnArr.get(cIdx) + dtSec * 1.8f; csnArr.set(cIdx, spin); val len = clnArr.get(cIdx)
                if (Random.nextFloat() < 0.08f) cchArr.set(cIdx * 18 + Random.nextInt(len), Random.nextInt(ms.size))
                if (y + len * step < -11.5f || z > 6.5f) { y = 12.5f; cxArr.set(cIdx, (Random.nextFloat() - 0.5f) * 14f); z = if (fly) -14f else -Random.nextFloat() * 13f - 1f }
                cyArr.set(cIdx, y); czArr.set(cIdx, z); val colX = cxArr.get(cIdx)
                var rCol = 0.05f * inten; var gCol = 1f * inten; var bCol = 0.35f * inten
                if (th == 1) { rCol = 0f; gCol = 0.85f * inten; bCol = 1f * inten }
                else if (th == 2) { rCol = 1f * inten; gCol = 0.14f * inten; bCol = 0.2f * inten }
                else if (th == 3) { rCol = 1f * inten; gCol = 0.78f * inten; bCol = 0.1f * inten }
                else if (th == 4) { rCol = 0.8f * inten; gCol = 0.2f * inten; bCol = 1f * inten }
                else if (th == 5) { rCol = (0.5f + 0.5f * sin(tm + colX)) * inten; gCol = (0.5f + 0.5f * sin(tm + colX + 2.1f)) * inten; bCol = (0.5f + 0.5f * sin(tm + colX + 4.2f)) * inten }

                for (i in 0 until len) {
                    val gy = y + i * step; if (gy > 13f || gy < -13f) continue
                    var gx = colX; var fgy = gy
                    if (vor && pf > 0.02f) {
                        val dx = gx - twX; val dy = fgy - twY; val d2 = dx * dx + dy * dy
                        if (d2 < 20.25f) { val dist = sqrt(d2) + 0.15f; val push = (4.5f - dist) * 0.32f * pf; gx += (dx / dist) * push - (dy / dist) * push * 0.45f; fgy += (dy / dist) * push + (dx / dist) * push * 0.45f }
                    }
                    val mIdx = cchArr.get(cIdx * 18 + i); val cur = bktCnt.get(mIdx)
                    if (cur < 320) {
                        val arr = bktData.get(mIdx); val off = cur * 8; val fade = 1f - (i.toFloat() / len)
                        arr.set(off, gx); arr.set(off + 1, fgy); arr.set(off + 2, z); arr.set(off + 3, spin + i * 0.22f)
                        if (i == 0) { arr.set(off + 4, 0.9f); arr.set(off + 5, 1f); arr.set(off + 6, 0.92f); arr.set(off + 7, 1f) }
                        else { arr.set(off + 4, rCol * fade); arr.set(off + 5, gCol * fade); arr.set(off + 6, bCol * fade); arr.set(off + 7, fade) }
                        bktCnt.set(mIdx, cur + 1)
                    }
                }
            }
            GLES30.glUseProgram(pr); GLES30.glUniformMatrix4fv(uVP, 1, false, vp, 0); GLES30.glUniform1f(uBl, blm); GLES30.glUniform1f(uSc, sc)
            for (mIdx in ms.indices) {
                val cnt = bktCnt.get(mIdx); if (cnt == 0) continue
                instBuf.clear(); instBuf.put(bktData.get(mIdx), 0, cnt * 8); instBuf.position(0)
                val mesh = ms.get(mIdx); GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mesh.first)
                GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 24, 0); GLES30.glVertexAttribDivisor(0, 0)
                GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 24, 12); GLES30.glVertexAttribDivisor(1, 0)
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instVbo.get(0)); GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, cnt * 32, instBuf, GLES30.GL_STREAM_DRAW)
                GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, 32, 0); GLES30.glVertexAttribDivisor(2, 1)
                GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, 32, 16); GLES30.glVertexAttribDivisor(3, 1)
                GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, mesh.second, cnt)
            }
            e.eglSwapBuffers(d, s); val em = (System.nanoTime() - now) / 1_000_000L; if (em < 15L) sleep(15L - em)
        }
        e.eglMakeCurrent(d, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT); e.eglDestroySurface(d, s); e.eglDestroyContext(d, c); e.eglTerminate(d)
    }
}
