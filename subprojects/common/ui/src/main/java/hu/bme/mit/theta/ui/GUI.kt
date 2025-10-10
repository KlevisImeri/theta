package hu.bme.mit.theta.ui

import com.raylib.java.Raylib
import com.raylib.java.core.Color
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

object GUI {
  val rlj: Raylib by lazy { Raylib() }
  private val SCREEN_WIDTH = 800
  private val SCREEN_HEIGHT = 600
  private val renderAction = AtomicReference<() -> Unit>({})
  private val isRunning = AtomicBoolean(false)

  fun draw(action: () -> Unit) {
    renderAction.set(action)
  }

  fun start() {
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
    isRunning.set(false)
  }
}
