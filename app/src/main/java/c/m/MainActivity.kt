package c.m
import android.app.*;import android.content.*;import android.graphics.*;import android.os.Bundle;import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = getSharedPreferences("m", 0)
        val sv = ScrollView(this).apply { setBackgroundColor(Color.BLACK) }
        val r = LinearLayout(this).apply { orientation = 1; setPadding(56, 90, 56, 64) }
        fun l(s: String) = TextView(this).apply { text = s; setTextColor(Color.GREEN); textSize = 15f; typeface = Typeface.MONOSPACE; setPadding(0, 18, 0, 6) }
        val dev = TextView(this).apply { text = "Developer: Dj DiD@S\n-H@CK the PL@NET-"; setTextColor(Color.rgb(0, 255, 120)); textSize = 18f; typeface = Typeface.MONOSPACE; gravity = 17; setPadding(0, 0, 0, 26) }
        val ths = arrayOf("MATRIX GREEN", "CYBER CYAN", "CRIMSON RED", "SOLAR GOLD", "ULTRAVIOLET", "RAINBOW PRISM")
        val bTh = Button(this).apply {
            text = "THEME: ${ths.get(p.getInt("th", 0) % 6)}"
            setBackgroundColor(Color.DKGRAY); setTextColor(Color.GREEN)
            setOnClickListener { val n = (p.getInt("th", 0) + 1) % 6; p.edit().putInt("th", n).apply(); text = "THEME: ${ths.get(n)}" }
        }
        fun sb(k: String, def: Int, minV: Int, maxV: Int, tv: TextView, prefix: String, suffix: String = "") = SeekBar(this).apply {
            max = maxV - minV; progress = p.getInt(k, def) - minV
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, f: Boolean) { val cur = v + minV; tv.text = "$prefix: $cur$suffix"; p.edit().putInt(k, cur).apply() }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        val t1 = l("FALL SPEED: ${p.getInt("sp", 12)}"); val s1 = sb("sp", 12, 2, 30, t1, "FALL SPEED")
        val t2 = l("3D GLYPH SCALE: ${p.getInt("sz", 42)}"); val s2 = sb("sz", 42, 20, 68, t2, "3D GLYPH SCALE")
        val t3 = l("STREAM DENSITY: ${p.getInt("dn", 85)}"); val s3 = sb("dn", 85, 30, 120, t3, "STREAM DENSITY")
        val t4 = l("EFFECT INTENSITY: ${p.getInt("in", 65)}%"); val s4 = sb("in", 65, 10, 150, t4, "EFFECT INTENSITY", "%")
        val t5 = l("BLOOM & BOKEH: ${p.getInt("bl", 35)}%"); val s5 = sb("bl", 35, 0, 100, t5, "BLOOM & BOKEH", "%")
        fun sw(lb: String, k: String) = Switch(this).apply {
            text = lb; isChecked = p.getBoolean(k, true); setTextColor(Color.WHITE); typeface = Typeface.MONOSPACE; setPadding(0, 14, 0, 14)
            setOnCheckedChangeListener { _, v -> p.edit().putBoolean(k, v).apply() }
        }
        val btn = Button(this).apply {
            text = "SET 3D WALLPAPER"; setBackgroundColor(Color.GREEN); setTextColor(Color.BLACK)
            setOnClickListener {
                try { startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, W::class.java))) }
                catch (e: Exception) { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
            }
        }
        r.addView(dev); r.addView(bTh); r.addView(t1); r.addView(s1); r.addView(t2); r.addView(s2); r.addView(t3); r.addView(s3); r.addView(t4); r.addView(s4); r.addView(t5); r.addView(s5)
        r.addView(sw("LENS BLOOM & BOKEH", "f1")); r.addView(sw("TOUCH 3D VORTEX & SURGE", "f2")); r.addView(sw("RAZR FOLD-ADAPTIVE FOV", "f3")); r.addView(sw("3-AXIS ROTATION TILT", "f4")); r.addView(sw("FLY-OUT 3D GLYPHS", "f5"))
        r.addView(btn); sv.addView(r); setContentView(sv)
    }
}
