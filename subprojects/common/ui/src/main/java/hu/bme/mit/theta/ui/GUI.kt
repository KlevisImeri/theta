package hu.bme.mit.theta.ui

import com.raylib.java.Raylib
import com.raylib.java.core.Color
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

object GUI {
  // INFO: It has to be compile time constatn for optimizaion to remove GUI parts of code from the binary
  // const val enabled = true;
  const val enabled = false;


  val rlj: Raylib by lazy { Raylib() }

  private val SCREEN_WIDTH = 800
  private val SCREEN_HEIGHT = 600
  private val renderAction = AtomicReference<() -> Unit>({})
  private val isRunning = AtomicBoolean(false)

  fun draw(action: () -> Unit) {
    if(!enabled) return;
    renderAction.set(action)
  }

  fun start() {
    if(!enabled) return;
    if (isRunning.compareAndSet(false, true)) {
      thread(isDaemon = true, name = "Raylib-GUI-Thread") {
        try {
          rlj.core.InitWindow(SCREEN_WIDTH, SCREEN_HEIGHT, "Theta UI")
          rlj.core.SetTargetFPS(60)

          while (!rlj.core.WindowShouldClose() && isRunning.get()) {
            rlj.core.BeginDrawing()
            rlj.core.ClearBackground(Color.RAYWHITE)
            renderAction.get().invoke()
            rlj.core.EndDrawing()
          }
        } finally {
          rlj.core.CloseWindow()
          isRunning.set(false)
        }
      }
    }
  }

  fun stop() {
    if(!enabled) return;
    isRunning.set(false)
  }
}
