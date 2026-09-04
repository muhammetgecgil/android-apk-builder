from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
P=ROOT/'tools/apply_v106_dynamic_real_weather_world.py'
s=P.read_text()

# WeatherEffectsView has android.view.View between Shader and java.util imports
# after the historical patch chain. Anchor only on java.util.Locale.
s=s.replace("'import android.graphics.Shader;\\n\\nimport java.util.Locale;'", "'import java.util.Locale;'")
s=s.replace("'''import android.graphics.Shader;\\n\\nimport com.mg.fixturecockpitsim.sim.RealWeatherCoordinator;", "'''import com.mg.fixturecockpitsim.sim.RealWeatherCoordinator;")

# v95 already owns getSharedVisibilityM(); do not add a duplicate method.
s=s.replace("    public static float getSharedVisibilityM(){RealWeatherProfile x=RealWeatherCoordinator.getProfile();return x==null?30000f:(float)x.visibilityM;}", "")

# v95 inserts updateSharedEnvironment(now) into the effective WeatherEffectsView
# onDraw() line. Change the OLD v106 anchor itself so it matches the historical
# patched source.
base_on_draw="super.onDraw(c);long now=System.currentTimeMillis();dayPhase=computeDayPhase(now);if(now>=nextWeatherChangeMs)chooseWeather(now,false);"
s=s.replace(base_on_draw,base_on_draw+"updateSharedEnvironment(now);")

# v95 also inserts an entire scheduler method between onDraw() and
# computeDayPhase(). The v106 block replacement must match that method and must
# preserve it in the NEW block. Inject the exact historical method before both
# old and new computeDayPhase variants in the patch source.
v95_scheduler=(
"    private void updateSharedEnvironment(long now){\\n"
"        sharedWeatherCode=weather;sharedDayPhase=dayPhase;\\n"
"        float bank=(weather==RAIN||weather==CLOUDY||dayPhase==DAWN)?(.03f+.08f*(float)(.5+.5*Math.sin(now*.000045))):0f;\\n"
"        sharedFog01=(float)EnvironmentRealismModel.fog01(weather,dayPhase,sharedCloudLayerCoverage,bank);\\n"
"        sharedVisibilityM=(float)EnvironmentRealismModel.visibilityMeters(weather,dayPhase,sharedCloudLayerCoverage,bank);\\n"
"        float target=(float)EnvironmentRealismModel.wetnessTarget(weather);wetness01+=(target-wetness01)*.025f;sharedWetness01=clamp(wetness01,0,1);\\n"
"        sharedNightFactor=(float)EnvironmentRealismModel.nightFactor(dayPhase);\\n"
"        sharedRunwayLightGain=(float)EnvironmentRealismModel.runwayLightGain(dayPhase,weather,sharedFog01);\\n"
"    }\\n\\n"
)
old_compute="    private int computeDayPhase(long now){float f=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(f<0)f+=1f;if(f<.14f)return DAWN;if(f<.34f)return MORNING;if(f<.58f)return NOON;if(f<.72f)return SUNSET;if(f<.82f)return EVENING;return NIGHT;}"
new_compute="    private int computeDayPhase(long now){float f=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(f<0)f+=1f;return computeDayPhase01(f);}"
s=s.replace(old_compute,v95_scheduler+old_compute)
s=s.replace(new_compute,v95_scheduler+new_compute)

# The live branch must let v95 initialise all legacy fog/wet-runway/light state,
# then overwrite those values with the live profile. The offline branch keeps
# v95's synthetic scheduler intact.
old_live="float day01;if(live!=null){applyRealWeather(live);day01=(float)live.localDay01;dayPhase=computeDayPhase01(day01);}else{dayPhase=computeDayPhase(now);if(now>=nextWeatherChangeMs)chooseWeather(now,false);day01=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(day01<0)day01+=1f;}"
new_live="float day01;if(live!=null){day01=(float)live.localDay01;dayPhase=computeDayPhase01(day01);updateSharedEnvironment(now);applyRealWeather(live);}else{dayPhase=computeDayPhase(now);if(now>=nextWeatherChangeMs)chooseWeather(now,false);updateSharedEnvironment(now);day01=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(day01<0)day01+=1f;}"
s=s.replace(old_live,new_live)

# Avoid relying on float-only clamp helpers for live-weather double values.
s=s.replace("sharedCloudLayerCoverage=(float)clamp(Math.max(x.lowCloud01*.96,x.cloud01*.78),0,1);",
            "sharedCloudLayerCoverage=(float)Math.max(0.0,Math.min(1.0,Math.max(x.lowCloud01*.96,x.cloud01*.78)));" )
s=s.replace("windStrength=(float)clamp(x.windSpeedMps/18.0,.05,1);",
            "windStrength=(float)Math.max(.05,Math.min(1.0,x.windSpeedMps/18.0));")
s=s.replace("float d=(float)clamp(.45+(1-x.visibilityM/10000.0)*.50,.45,.94);",
            "float d=(float)Math.max(.45,Math.min(.94,.45+(1-x.visibilityM/10000.0)*.50));")

# Extend applyRealWeather so v95's runway/fog environment state follows the same
# real current conditions used by sky, mountains and sea.
wind_line="windy=x.windSpeedMps>2.2;windStrength=(float)Math.max(.05,Math.min(1.0,x.windSpeedMps/18.0));float cross=RealWeatherCoordinator.crosswindMps(270.0);windSign=cross>=0?1:-1;sharedWindy=windy;sharedWindStrength=windStrength;sharedWindSign=windSign;"
live_env_line=wind_line+"sharedWeatherCode=weather;sharedDayPhase=dayPhase;sharedVisibilityM=(float)x.visibilityM;float liveFog=(float)Math.max(x.kind==RealWeatherProfile.FOG?.78:0.0,Math.max(x.skyHaze01*.28,1.0-Math.min(1.0,x.visibilityM/16000.0)));sharedFog01=clamp(liveFog,0,1);float wetTarget=(float)Math.max(0.0,Math.min(1.0,x.precipitationMm/1.5));wetness01+=(wetTarget-wetness01)*.060f;sharedWetness01=clamp(wetness01,0,1);sharedNightFactor=(float)EnvironmentRealismModel.nightFactor(dayPhase);sharedRunwayLightGain=(float)EnvironmentRealismModel.runwayLightGain(dayPhase,weather,sharedFog01);"
s=s.replace(wind_line,live_env_line)

P.write_text(s)
print('v106 weather compatibility enabled: v95 onDraw + scheduler preserved')
