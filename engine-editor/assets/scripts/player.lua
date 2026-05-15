
function onStart()
    Debug.log("onStart: " .. entity.name)
end

function onStop()
    Debug.log("onStop: " .. entity.name)
end

function onUpdate(dt)
    if Input.getKey("W") then
        transform.position.y = transform.position.y + dt * 3
    end
    if Input.getKey("S") then
        transform.position.y = transform.position.y - dt * 3
    end
    if Input.getKey("A") then
        transform.position.x = transform.position.x - dt * 3
    end
    if Input.getKey("D") then
        transform.position.x = transform.position.x + dt * 3
    end
end
