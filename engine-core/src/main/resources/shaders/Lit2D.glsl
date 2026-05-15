#version 460 core

#ifndef MAX_TEXTURE_SLOTS
    #define MAX_TEXTURE_SLOTS 8
#endif

#ifndef MAX_POINT_LIGHTS
    #define MAX_POINT_LIGHTS 16
#endif

#stage vertex

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec4 a_Color;
layout(location = 2) in vec2 a_TexCoord;
layout(location = 3) in float a_TexIndex;

out vec4 v_Color;
out vec2 v_TexCoord;
flat out float v_TexIndex;
out vec2 v_WorldPos;

uniform mat4 u_ViewProjection;

void main()
{
    v_Color = a_Color;
    v_TexCoord = a_TexCoord;
    v_TexIndex = a_TexIndex;
    v_WorldPos = a_Position.xy;

    gl_Position = u_ViewProjection * vec4(a_Position, 1.0);
}

#stage fragment

in vec4 v_Color;
in vec2 v_TexCoord;
flat in float v_TexIndex;
in vec2 v_WorldPos;

layout(location = 0) out vec4 FragColor;

uniform sampler2D u_Textures[MAX_TEXTURE_SLOTS];

struct PointLight2D
{
    vec2 position;
    vec3 color;
    float intensity;
    float radius;
};

uniform vec3 u_AmbientColor;
uniform float u_AmbientIntensity;
uniform int u_LightCount;
uniform PointLight2D u_Lights[MAX_POINT_LIGHTS];

void main()
{
    int index = int(v_TexIndex);
    vec4 texColor = texture(u_Textures[index], v_TexCoord) * v_Color;

    vec3 lighting = u_AmbientColor * u_AmbientIntensity;

    for (int i = 0; i < u_LightCount; i++)
    {
        float dist = distance(v_WorldPos, u_Lights[i].position);
        float attenuation = clamp(1.0 - dist / u_Lights[i].radius, 0.0, 1.0);
        attenuation *= attenuation;
        lighting += u_Lights[i].color * u_Lights[i].intensity * attenuation;
    }

    FragColor = vec4(texColor.rgb * lighting, texColor.a);
}
