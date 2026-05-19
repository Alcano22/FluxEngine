{
    "entities": [
        {
            "name": "Main Camera",
            "uuid": "766d208c-85aa-49eb-ad67-d2665d339b76",
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
            "uuid": "52f0ad74-56b9-4405-aa9a-2b27f1c28b82",
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
                    "textureHandle": "Assets/Textures/player1.png"
                },
                {
                    "class": "org.flux.core.scene.SpriteAnimatorComponent",
                    "animationHandle": "Assets/Animations/Player.anim"
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
            "uuid": "4cea1e76-f0a2-425a-9f8c-ca9c4b43d0c4",
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