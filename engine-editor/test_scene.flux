{
    "entities": [
        {
            "name": "Main Camera",
            "uuid": "2cc37f7d-5742-468d-9959-2e29ddf8a99d",
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
            "uuid": "d97f50f4-e6fa-4d4b-8c83-783eb3b82fc5",
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
                    "class": "SpriteRendererComponent"
                },
                {
                    "class": "SpriteAnimatorComponent",
                    "animationHandle": "Assets/Animations/Player.asset"
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
            "uuid": "3f575969-8da8-41fb-9fbe-ea56f291d4ae",
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