/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;

public class TextureExtAttribute extends TextureAttribute {

    public final static String HeightAlias = "heightTexture";
    public final static long Height = register(HeightAlias);

    public final static String NightAlias = "nightTexture";
    public final static long Night = register(NightAlias);

    static {
        TextureAttribute.Mask |= Height | Night;
    }

    public static TextureAttribute createHeight (final Texture texture) {
        return new TextureAttribute(Height, texture);
    }

    public static TextureAttribute createHeight (final TextureRegion region) {
        return new TextureAttribute(Height, region);
    }


    public static TextureAttribute createNight (final Texture texture) {
        return new TextureAttribute(Night, texture);
    }

    public static TextureAttribute createNight (final TextureRegion region) {
        return new TextureAttribute(Night, region);
    }

    public TextureExtAttribute (final long type) {
        super(type);
    }

    public <T extends Texture> TextureExtAttribute (final long type, final TextureDescriptor<T> textureDescription) {
        super(type, textureDescription);
    }

    public <T extends Texture> TextureExtAttribute (final long type, final TextureDescriptor<T> textureDescription, float offsetU,
            float offsetV, float scaleU, float scaleV, int uvIndex) {
        super(type, textureDescription, offsetU, offsetV, scaleU, scaleV, uvIndex);
    }

    public <T extends Texture> TextureExtAttribute (final long type, final TextureDescriptor<T> textureDescription, float offsetU,
            float offsetV, float scaleU, float scaleV) {
        super(type, textureDescription, offsetU, offsetV, scaleU, scaleV);
    }

    public TextureExtAttribute (final long type, final Texture texture) {
        super(type, texture);
    }

    public TextureExtAttribute (final long type, final TextureRegion region) {
        super(type, region);
    }

    public TextureExtAttribute (final TextureAttribute copyFrom) {
        super(copyFrom);
    }
}
