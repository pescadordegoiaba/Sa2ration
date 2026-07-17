#version 140

// SPDX-License-Identifier: GPL-2.0-or-later

#include "colormanagement.glsl"

uniform sampler2D sampler;
uniform vec4 modulation;
uniform float sa2Brightness;
uniform float sa2Contrast;
uniform float sa2Saturation;
uniform float sa2Offset;

in vec2 texcoord0;
out vec4 fragColor;

void main()
{
    vec4 tex = texture(sampler, texcoord0);
    tex = sourceEncodingToNitsInDestinationColorspace(tex);
    tex = nitsToEncoding(tex, gamma22_EOTF, 0.0, destinationReferenceLuminance);

    float originalAlpha = max(0.001, tex.a);
    vec3 color = tex.rgb / originalAlpha;
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));

    color = mix(vec3(luminance), color, sa2Saturation);
    color = (color - vec3(0.5)) * sa2Contrast + vec3(0.5);
    color = color * sa2Brightness + vec3(sa2Offset);
    color = max(color, vec3(0.0));

    tex.rgb = color;
    tex *= modulation;
    tex.rgb *= tex.a;
    tex = encodingToNits(tex, gamma22_EOTF, 0.0, destinationReferenceLuminance);
    fragColor = nitsToDestinationEncoding(tex);
}
