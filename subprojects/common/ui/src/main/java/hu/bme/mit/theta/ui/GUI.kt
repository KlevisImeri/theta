package hu.bme.mit.theta.ui

import com.raylib.java.Raylib
import com.raylib.java.core.Color
import com.raylib.java.core.rcamera.Camera2D
import com.raylib.java.raymath.Vector2
import com.raylib.java.utils.Tracelog
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import com.raylib.java.core.input.Keyboard.KEY_R
import com.raylib.java.core.input.Mouse.MouseButton.MOUSE_BUTTON_RIGHT
import com.raylib.java.core.input.Mouse.MouseButton.MOUSE_BUTTON_LEFT


object GUI {
  const val enabled = false;

  val rlj: Raylib by lazy { Raylib() }

  private val SCREEN_WIDTH = 1480
  private val SCREEN_HEIGHT = 900
  private val renderAction = AtomicReference<() -> Unit>({})
  private val isRunning = AtomicBoolean(false)
  private var prevMousePos = Vector2(0f, 0f);
  
  private val camera = AtomicReference(Camera2D().apply {
    target = Vector2(0f, 0f)
    offset = Vector2(0f, 0f)
    rotation = 0.0f
    zoom = 1.0f
  })
  
  private val zoomMin = 0.1f
  private val zoomMax = 5.0f
  private val zoomSpeed = 0.1f

  fun draw(action: () -> Unit) {
    if(!enabled) return;
    renderAction.set(action)
  }

  fun start() {
    if(!enabled) return;
    if (isRunning.compareAndSet(false, true)) {
      thread(isDaemon = true, name = "Raylib-GUI-Thread") {
        try {
          Tracelog.SetTraceLogLevel(Tracelog.TracelogType.LOG_NONE)
          rlj.core.InitWindow(SCREEN_WIDTH, SCREEN_HEIGHT, "Theta UI")
          rlj.core.SetTargetFPS(60)

          while (!rlj.core.WindowShouldClose() && isRunning.get()) {      
            handleCameraInput()
            
            rlj.core.BeginDrawing()
            rlj.core.ClearBackground(Color.RAYWHITE)
            
            rlj.core.BeginMode2D(camera.get())
            
            renderAction.get().invoke()
            
            rlj.core.EndMode2D()
            
            // drawUI()
            
            rlj.core.EndDrawing()
          }
        } finally {
          rlj.core.CloseWindow()
          isRunning.set(false)
        }
      }
    }
  }

  
  var move = false;
  private fun handleCameraInput() {
    val currentCamera = camera.get()
    val updatedCamera = Camera2D().apply {
        target = Vector2(currentCamera.target.x, currentCamera.target.y)
        offset = Vector2(currentCamera.offset.x, currentCamera.offset.y)
        rotation = currentCamera.rotation
        zoom = currentCamera.zoom
    }

    if (rlj.core.IsMouseButtonDown(MOUSE_BUTTON_LEFT.ordinal)) {
      var delta = Vector2(0f,0f);
      if(move==false) {
        prevMousePos = rlj.core.GetMousePosition();
        move = true;
      } else {
        val mousePos = rlj.core.GetMousePosition()
        delta = Vector2(mousePos.x - prevMousePos.x, mousePos.y - prevMousePos.y)
        prevMousePos = Vector2(mousePos.x, mousePos.y)
      }
      val worldDeltaX = -delta.x / updatedCamera.zoom
      val worldDeltaY = -delta.y / updatedCamera.zoom
      updatedCamera.target = Vector2(
          updatedCamera.target.x + worldDeltaX,
          updatedCamera.target.y + worldDeltaY
      )
    } else {
      move = false;
    }

    val mouseWheelMove = rlj.core.GetMouseWheelMoveV()
    if (mouseWheelMove != null && (mouseWheelMove.x != 0f || mouseWheelMove.y != 0f)) {
        val wheel = if (kotlin.math.abs(mouseWheelMove.x) > kotlin.math.abs(mouseWheelMove.y))
                        mouseWheelMove.x else mouseWheelMove.y
        if (wheel != 0f) {
            val mousePos = rlj.core.GetMousePosition()
            val mouseWorldPos = rlj.core.GetScreenToWorld2D(mousePos, updatedCamera)

            updatedCamera.offset = Vector2(mousePos.x, mousePos.y)
            updatedCamera.target = Vector2(mouseWorldPos.x, mouseWorldPos.y)

            val scale = 0.2f * wheel
            val newZoom = kotlin.math.exp(kotlin.math.ln(updatedCamera.zoom.toDouble()) + scale).toFloat()
            updatedCamera.zoom = newZoom.coerceIn(zoomMin, zoomMax)
        }
    }

    if (rlj.core.IsKeyPressed(KEY_R)) {
        updatedCamera.target = Vector2(0f, 0f)
        updatedCamera.offset = Vector2(SCREEN_WIDTH / 2.0f, SCREEN_HEIGHT / 2.0f)
        updatedCamera.zoom = 1.0f
        updatedCamera.rotation = 0.0f
    }

    camera.set(updatedCamera)
}

  private fun drawUI() {
    rlj.text.DrawText("Zoom: ${String.format("%.1f", camera.get().zoom)}x", 10, 10, 8, Color.BLACK)
    rlj.text.DrawText("Use mouse wheel to zoom, left-click drag to pan, R to reset", 10, 35, 8, Color.DARKGRAY)
  }

  fun stop() {
    if(!enabled) return;
    isRunning.set(false)
  }
  
  fun getCamera(): Camera2D = camera.get()
  
  fun resetCamera() {
    camera.set(Camera2D().apply {
      target = Vector2(0f, 0f)
      offset = Vector2(0f, 0f)
      rotation = 0.0f
      zoom = 1.0f
    })
  }
}
