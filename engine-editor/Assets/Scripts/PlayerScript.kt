import org.flux.scripting.Script
import org.flux.core.input.Input
import org.flux.core.input.Key
import org.flux.core.scene.SingleComponent
import org.flux.core.scene.SpriteAnimatorComponent
import org.joml.Vector2f

@SingleComponent
class PlayerScript : Script() {

    var speed = 5f

    private var animator: SpriteAnimatorComponent? = null

    override fun start() {
        animator = entity.getComponent<SpriteAnimatorComponent>()
        animator?.play("Idle")
    }

    override fun update(dt: Float) {
        val dir = Vector2f()

        if (Input.getKey(Key.W))
            dir.y += 1f
        if (Input.getKey(Key.S))
            dir.y -= 1f
        if (Input.getKey(Key.A))
            dir.x -= 1f
        if (Input.getKey(Key.D))
            dir.x += 1f

        val moving = dir.length() > 0f
        if (moving) {
            dir.normalize().mul(speed * dt)
            transform.position.x += dir.x
            transform.position.y += dir.y

            transform.rotation.y = if (dir.x < 0f) Math.PI.toFloat() else 0f
        }

        animator?.play(if (moving) "Run" else "Idle")
    }
}
