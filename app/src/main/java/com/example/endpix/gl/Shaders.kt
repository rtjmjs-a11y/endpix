package com.example.endpix.gl

object Shaders {

    val vertex: String = """
        #version 300 es
        uniform mat4 uMVP;
        uniform vec2 uViewport;
        uniform vec2 uOffset;
        uniform float uScale;
        in vec2 aPos;
        out vec2 vCanvasPixel;
        void main() {
            vec2 screen = aPos * uViewport;
            vCanvasPixel = (screen - uOffset) / uScale;
            gl_Position = uMVP * vec4(screen, 0.0, 1.0);
        }
    """.trimIndent()

    val fragment: String = """
        #version 300 es
        precision highp float;
        uniform sampler2D uTex;
        uniform vec2 uTexSize;
        uniform float uShowGrid;
        uniform float uPixelScale;
        uniform float uMode;
        uniform float uOpacity;
        in vec2 vCanvasPixel;
        out vec4 fragColor;

        vec3 checker(vec2 cp) {
            vec2 c = floor(cp / 8.0);
            float chk = mod(c.x + c.y, 2.0);
            return mix(vec3(0.82), vec3(1.0), chk);
        }

        void main() {
            vec2 cp = vCanvasPixel;
            bool inside = cp.x >= 0.0 && cp.x < uTexSize.x && cp.y >= 0.0 && cp.y < uTexSize.y;
            if (uMode > 2.5) {
                if (inside) {
                    fragColor = vec4(checker(cp), 1.0);
                } else {
                    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
                }
            } else if (uMode > 1.5) {
                if (inside && uShowGrid > 0.5 && uPixelScale > 5.0) {
                    vec2 pc = fract(cp);
                    float lw = 1.0 / uPixelScale;
                    float e = min(min(pc.x, 1.0 - pc.x), min(pc.y, 1.0 - pc.y));
                    if (e < lw) { fragColor = vec4(0.0, 0.0, 0.0, 0.25); return; }
                }
                discard;
            } else if (uMode > 0.5) {
                if (inside) {
                    vec4 c = texture(uTex, cp / uTexSize);
                    c.a *= uOpacity;
                    fragColor = c;
                } else {
                    discard;
                }
            } else {
                if (inside) {
                    vec4 c = texture(uTex, cp / uTexSize);
                    if (c.a < 0.004) {
                        fragColor = vec4(checker(cp), 1.0);
                    } else {
                        fragColor = c;
                    }
                    if (uShowGrid > 0.5 && uPixelScale > 5.0) {
                        vec2 pc = fract(cp);
                        float lw = 1.0 / uPixelScale;
                        float e = min(min(pc.x, 1.0 - pc.x), min(pc.y, 1.0 - pc.y));
                        if (e < lw) { fragColor.rgb = mix(fragColor.rgb, vec3(0.0), 0.25); }
                    }
                } else {
                    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
                }
            }
        }
    """.trimIndent()
}
