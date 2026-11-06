package com.just_sri.suvidha.radar.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min

/** OpenGL ES 2.0 orthographic globe renderer. Drive it with GLSurfaceView.RENDERMODE_WHEN_DIRTY. */
internal class EarthRenderer(context: Context) : GLSurfaceView.Renderer {
    private val appContext: Context = context.applicationContext

    private data class CameraState(val longitude: Float, val latitude: Float, val scale: Float)
    @Volatile private var camera = CameraState(0f, 20f, 1f)

    private val quad: FloatBuffer = ByteBuffer.allocateDirect(QUAD.size * FLOAT_BYTES)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(QUAD); position(0) }

    @Volatile private var surfaceWidth: Int = 1
    @Volatile private var surfaceHeight: Int = 1
    private var program: Int = 0
    private var texture: Int = 0
    private var hasTexture: Boolean = false
    private var positionLocation: Int = -1
    private var resolutionLocation: Int = -1
    private var radiusLocation: Int = -1
    private var cameraLocation: Int = -1
    private var textureLocation: Int = -1
    private var hasTextureLocation: Int = -1

    /** Atomically publishes a complete camera pose for the GL thread. */
    fun updateCamera(lon: Float, lat: Float, zoomScale: Float) {
        val safeLon = if (lon.isFinite()) lon else 0f
        val safeLat = if (lat.isFinite()) lat.coerceIn(-89.9f, 89.9f) else 20f
        val safeScale = if (zoomScale.isFinite()) zoomScale.coerceIn(0.25f, 16f) else 1f
        camera = CameraState(safeLon % 360f, safeLat, safeScale)
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.012f, 0.025f, 0.035f, 1f)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) GLES20.glDeleteShader(vertex)
            if (fragment != 0) GLES20.glDeleteShader(fragment)
            program = 0
            Log.e(TAG, "Unable to create EarthRenderer shaders")
            return
        }
        val linked = GLES20.glCreateProgram()
        GLES20.glAttachShader(linked, vertex)
        GLES20.glAttachShader(linked, fragment)
        GLES20.glLinkProgram(linked)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        val status = IntArray(1)
        GLES20.glGetProgramiv(linked, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "EarthRenderer program link failed: ${GLES20.glGetProgramInfoLog(linked)}")
            GLES20.glDeleteProgram(linked)
            program = 0
            return
        }
        program = linked
        positionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        resolutionLocation = GLES20.glGetUniformLocation(program, "uResolution")
        radiusLocation = GLES20.glGetUniformLocation(program, "uRadius")
        cameraLocation = GLES20.glGetUniformLocation(program, "uCamera")
        textureLocation = GLES20.glGetUniformLocation(program, "uEarth")
        hasTextureLocation = GLES20.glGetUniformLocation(program, "uHasTexture")
        texture = loadEarthTexture()
        hasTexture = texture != 0
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        surfaceWidth = max(1, width)
        surfaceHeight = max(1, height)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return

        val pose = camera
        val radius = min(surfaceWidth, surfaceHeight) * 0.44f * pose.scale
        GLES20.glUseProgram(program)
        quad.position(0)
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glUniform2f(resolutionLocation, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(radiusLocation, max(1f, radius))
        GLES20.glUniform2f(cameraLocation, Math.toRadians(pose.longitude.toDouble()).toFloat(), Math.toRadians(pose.latitude.toDouble()).toFloat())
        GLES20.glUniform1i(hasTextureLocation, if (hasTexture) 1 else 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(textureLocation, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionLocation)
    }

    private fun loadEarthTexture(): Int {
        val bitmap = try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.assets.open(EARTH_ASSET).use { BitmapFactory.decodeStream(it, null, bounds) }
            val maxTextureSize = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
            val targetSize = min(MAX_DECODE_SIZE, maxTextureSize[0].coerceAtLeast(1))
            var sampleSize = 1
            while (max(bounds.outWidth, bounds.outHeight) / sampleSize > targetSize) sampleSize *= 2
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            appContext.assets.open(EARTH_ASSET).use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } catch (error: Exception) {
            Log.w(TAG, "Earth texture unavailable; using procedural globe", error)
            null
        } ?: return 0

        return try {
            val names = IntArray(1)
            GLES20.glGenTextures(1, names, 0)
            if (names[0] == 0) return 0
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, names[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            if (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
                GLES20.glDeleteTextures(1, names, 0)
                Log.w(TAG, "Could not upload Earth texture; using procedural globe")
                0
            } else names[0]
        } catch (error: Exception) {
            Log.w(TAG, "Could not upload Earth texture; using procedural globe", error)
            0
        } finally {
            bitmap.recycle()
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "EarthRenderer shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private companion object {
        const val TAG = "EarthRenderer"
        const val EARTH_ASSET = "earth_night.jpg"
        const val FLOAT_BYTES = 4
        const val MAX_DECODE_SIZE = 2048
        val QUAD = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)

        const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            void main() { gl_Position = vec4(aPosition, 0.0, 1.0); }
        """

        const val FRAGMENT_SHADER = """
            precision highp float;
            uniform vec2 uResolution;
            uniform float uRadius;
            uniform vec2 uCamera;
            uniform sampler2D uEarth;
            uniform int uHasTexture;

            const float PI = 3.14159265358979323846;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }
            float noise(vec2 p) {
                vec2 i = floor(p), f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
                           mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
            }
            void main() {
                vec2 pixel = gl_FragCoord.xy - 0.5 * uResolution;
                vec2 p = vec2(pixel.x, pixel.y) / uRadius;
                float r2 = dot(p, p);
                float atmosphereDist = length(p);
                if (r2 > 1.0) {
                    float glow = exp(-max(0.0, atmosphereDist - 1.0) * 18.0) * (1.0 - smoothstep(1.0, 1.15, atmosphereDist));
                    gl_FragColor = vec4(vec3(0.012, 0.025, 0.035) + vec3(0.025, 0.18, 0.16) * glow, 1.0);
                    return;
                }
                float z = sqrt(max(0.0, 1.0 - r2));
                float sinLat0 = sin(uCamera.y), cosLat0 = cos(uCamera.y);
                float sinLat = p.y * cosLat0 + z * sinLat0;
                float lat = asin(clamp(sinLat, -1.0, 1.0));
                float cosLatCosDelta = z * cosLat0 - p.y * sinLat0;
                float deltaLon = atan(p.x, cosLatCosDelta);
                float lon = uCamera.x + deltaLon;
                vec2 uv = vec2(fract(lon / (2.0 * PI) + 0.5), 0.5 - lat / PI);
                float edge = smoothstep(0.0, 0.12, z);
                float limbShade = 0.48 + 0.52 * sqrt(z);
                vec3 color;
                if (uHasTexture == 1) {
                    color = texture2D(uEarth, uv).rgb;
                    color *= limbShade;
                } else {
                    float waterDetail = noise(uv * vec2(90.0, 45.0)) * 0.012;
                    color = vec3(0.006, 0.035, 0.055) + vec3(0.3, 0.8, 0.9) * waterDetail;
                    color *= limbShade;
                }
                float rim = pow(1.0 - z, 3.0);
                color += vec3(0.012, 0.09, 0.30) * rim;
                color *= mix(0.88, 1.0, edge);
                gl_FragColor = vec4(color, 1.0);
            }
        """
    }
}
