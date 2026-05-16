{
    "entities": [
        {
            "name": "Main Camera",
            "components": [
                {
                    "class": "TransformComponent"
                },
                {
                    "class": "CameraComponent"
                }
            ]
        },
        {
            "name": "Player",
            "components": [
                {
                    "class": "TransformComponent",
                    "scale": [
                        3.0,
                        3.0,
                        1.0
                    ]
                },
                {
                    "class": "SpriteRendererComponent",
                    "textureHandle": "assets/textures/holy_mantle.png"
                },
                {
                    "class": "PlayerScript",
                    "scriptClass": "PlayerScript",
                    "properties": {
                        "speed": "5.0"
                    }
                }
            ]
        },
        {
            "name": "Point Light",
            "components": [
                {
                    "class": "TransformComponent"
                },
                {
                    "class": "PointLight2DComponent",
                    "intensity": 1.5,
                    "radius": 3.0,
                    "color": {
                        "r": 1.0,
                        "g": 0.9,
                        "b": 0.7
                    }
                }
            ]
        }
    ]
}