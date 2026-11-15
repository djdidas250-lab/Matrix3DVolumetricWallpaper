package c.m
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder

class W : WallpaperService() {
    override fun onCreateEngine(): Engine = E()
    inner class E : Engine(), SensorEventListener {
        private var t: Renderer3D? = null
        private var sm: SensorManager? = null
        override fun onCreate(h: SurfaceHolder) { super.onCreate(h); setTouchEventsEnabled(true); sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
        override fun onVisibilityChanged(v: Boolean) {
            if (v) {
                sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
                if (t == null) t = Renderer3D(surfaceHolder, getSharedPreferences("m", 0)).apply { start() }
            } else {
                sm?.unregisterListener(this); t?.r = false; t?.join(400); t = null
            }
        }
        override fun onSurfaceDestroyed(h: SurfaceHolder) { super.onSurfaceDestroyed(h); t?.r = false; t?.join(400); t = null }
        override fun onTouchEvent(e: MotionEvent) {
            if (e.action == 0 || e.action == 2) { t?.px = e.x; t?.py = e.y; t?.pf = 1f }
            else if (e.action == 1 || e.action == 3) t?.pf = 0f
        }
        override fun onSensorChanged(e: SensorEvent) {
            t?.let { it.tx = it.tx * 0.9f + (e.values.get(0) / 9.8f) * 0.1f; it.ty = it.ty * 0.9f + (e.values.get(1) / 9.8f) * 0.1f }
        }
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    }
}
