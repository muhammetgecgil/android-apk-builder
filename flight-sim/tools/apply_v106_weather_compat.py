from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
P=ROOT/'tools/apply_v106_dynamic_real_weather_world.py'
s=P.read_text()

# WeatherEffectsView has android.view.View between Shader and java.util imports
# after the historical patch chain. Anchor only on java.util.Locale.
s=s.replace("'import android.graphics.Shader;\\n\\nimport java.util.Locale;'", "'import java.util.Locale;'")
s=s.replace("'''import android.graphics.Shader;\\n\\nimport com.mg.fixturecockpitsim.sim.RealWeatherCoordinator;", "'''import com.mg.fixturecockpitsim.sim.RealWeatherCoordinator;")

# Avoid relying on a float-only clamp helper for live-weather double values.
s=s.replace("sharedCloudLayerCoverage=(float)clamp(Math.max(x.lowCloud01*.96,x.cloud01*.78),0,1);",
            "sharedCloudLayerCoverage=(float)Math.max(0.0,Math.min(1.0,Math.max(x.lowCloud01*.96,x.cloud01*.78)));" )
s=s.replace("windStrength=(float)clamp(x.windSpeedMps/18.0,.05,1);",
            "windStrength=(float)Math.max(.05,Math.min(1.0,x.windSpeedMps/18.0));")
s=s.replace("float d=(float)clamp(.45+(1-x.visibilityM/10000.0)*.50,.45,.94);",
            "float d=(float)Math.max(.45,Math.min(.94,.45+(1-x.visibilityM/10000.0)*.50));")

P.write_text(s)
print('v106 weather compatibility enabled')
