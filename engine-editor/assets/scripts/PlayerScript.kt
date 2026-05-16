import org.flux.scripting.Script
import org.flux.core.input.Input
import org.flux.core.input.Key

class PlayerScript : Script() {

    var speed = 5f

    override fun start() {
        println("PlayerScript started!")
    }

    override fun update(dt: Float) {
        val move = speed * dt
        if (Input.getKey(Key.W))
            transform.position.y += move
        if (Input.getKey(Key.S))
            transform.position.y -= move
        if (Input.getKey(Key.A))
            transform.position.x -= move
        if (Input.getKey(Key.D))
            transform.position.x += move
    }
}
