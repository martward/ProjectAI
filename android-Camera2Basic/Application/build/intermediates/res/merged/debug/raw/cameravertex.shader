attribute vec4 position;
attribute vec2 inputTextureCoordinate;
varying vec2 textureCoordinate;
void main()
{
    gl_Position = vec4 (position.x, position.y, 0, 1.0);

    textureCoordinate = vec2((inputTextureCoordinate[0]/2.0f+0.5f), 1.0f-(inputTextureCoordinate[1]/2.0f+0.5f));
}