#version 460 core

#stage vertex

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec4 a_Color;
layout(location = 2) in vec2 a_TexCoord;
layout(location = 3) in float a_TexIndex;
layout(location = 4) in float a_EntityID;

flat out float v_EntityID;

uniform mat4 u_ViewProjection;

void main()
{
    v_EntityID = a_EntityID;

    gl_Position = u_ViewProjection * vec4(a_Position, 1.0);
}

#stage fragment

flat in float v_EntityID;

layout(location = 0) out int EntityID;

void main()
{
    EntityID = int(v_EntityID);
}
