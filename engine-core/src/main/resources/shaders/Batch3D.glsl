#version 460 core

#ifndef MAX_TEXTURE_SLOTS
    #define MAX_TEXTURE_SLOTS 8
#endif

#stage vertex

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec4 a_Color;
layout(location = 2) in vec2 a_TexCoord;
layout(location = 3) in float a_TexIndex;

out vec4 v_Color;
out vec2 v_TexCoord;
flat out float v_TexIndex;

uniform mat4 u_ViewProjection;

void main()
{
    v_Color = a_Color;
    v_TexCoord = a_TexCoord;
    v_TexIndex = a_TexIndex;

    gl_Position = u_ViewProjection * vec4(a_Position, 1.0);
}

#stage fragment

in vec4 v_Color;
in vec2 v_TexCoord;
flat in float v_TexIndex;

layout(location = 0) out vec4 FragColor;

uniform sampler2D u_Textures[MAX_TEXTURE_SLOTS];

void main()
{
    int index = int(v_TexIndex);
    FragColor = texture(u_Textures[index], v_TexCoord) * v_Color;
}
