float terrainHash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * .1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}
float terrainNoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3. - 2. * f);
    return mix(mix(terrainHash(i), terrainHash(i + vec2(1., 0.)), f.x),
               mix(terrainHash(i + vec2(0., 1.)), terrainHash(i + vec2(1., 1.)), f.x), f.y);
}
vec3 landscapeColor(vec3 p, vec3 n, float kind) {
    vec3 N = normalize(n), L = normalize(uLightDir);
    float h = p.y + 10.8, slope = 1. - clamp(N.y, 0., 1.);
    vec2 uv = p.xz + vec2(p.y * .57, p.y * .31);
    float macro = terrainNoise(uv * .16);
    float grain = terrainNoise(uv * 1.8);
    float fine = terrainNoise(uv * 6.2);
    float distance = length(uCameraPos - p);
    float detailFade = 1. - smoothstep(65., 270., distance);
    float rockMask = smoothstep(.14, .53, slope + (macro - .5) * .18);
    vec3 grass = mix(vec3(.115, .178, .091), vec3(.285, .302, .163), macro);
    vec3 rock = mix(vec3(.265, .253, .224), vec3(.49, .475, .409), macro);
    float strata = .5 + .5 * sin(h * 2.5 + macro * 5. + grain * 1.4);
    rock *= .86 + .14 * strata;
    vec3 base = mix(grass, rock, rockMask);
    float snow = smoothstep(25., 35., h + 3.5 * macro) * (1. - smoothstep(.33, .70, slope));
    if (kind < 60.5) base = mix(base, vec3(.83, .87, .87), snow);
    else if (kind < 61.5) {
        base = mix(mix(vec3(.103, .174, .094), vec3(.27, .31, .17), macro), rock, rockMask);
        float shore = 1. - smoothstep(.65, 2.5, h);
        base = mix(base, vec3(.63, .565, .395) * (.90 + .10 * grain), shore);
        float water = 1. - smoothstep(.74, 1.00, h);
        vec3 sea = mix(vec3(.045, .17, .245), vec3(.09, .38, .39), smoothstep(.4, .8, h));
        sea += vec3(.09, .12, .12) * pow(max(0., sin(p.z * 1.7 + p.x * .37 + uTime * .75)), 10.) * .23;
        base = mix(base, sea, water);
    } else if (kind < 62.5) {
        float ripple = sin(p.x * 5.5 + p.z * 2.1 + macro * 6.);
        base = mix(vec3(.49, .351, .185), vec3(.72, .594, .372), macro);
        base *= 1. + ripple * .035 * detailFade;
    } else if (kind < 63.5) {
        base = mix(vec3(.32, .244, .159), vec3(.56, .453, .300), macro);
        base = mix(base, vec3(.285, .233, .194) * (.9 + .1 * grain), rockMask * .65);
    } else {
        base = mix(vec3(.075, .115, .11), vec3(.22, .249, .24), macro);
        base = mix(base, vec3(.028, .072, .12), 1. - smoothstep(.5, 1.5, h));
    }
    base *= 1. + ((grain - .5) * .13 + (fine - .5) * .055) * detailFade;
    float diffuse = max(dot(N, L), 0.);
    float sky = .5 + .5 * max(N.y, 0.);
    float occlusion = .80 + .20 * smoothstep(0., 14., h);
    vec3 lit = base * (vec3(.25, .30, .36) * sky + vec3(.92, .85, .70) * diffuse * .88) * occlusion;
    // Directional, height-sensitive aerial perspective: distant ridges retain shape.
    float fog = (1. - exp(-distance * .0039)) * (.85 + .15 * exp(-h * .06));
    vec3 haze = mix(vec3(.52, .625, .69), vec3(.66, .70, .70), max(dot(normalize(p - uCameraPos), L), 0.));
    if (kind > 64.5) { lit *= vec3(.39, .49, .68); haze = vec3(.07, .115, .19); fog *= .82; }
    return mix(lit, haze, clamp(fog, 0., .80));
}
