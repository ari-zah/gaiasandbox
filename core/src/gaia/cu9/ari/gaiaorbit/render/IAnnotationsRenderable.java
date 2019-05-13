/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.gdx.g2d.BitmapFont;
import gaia.cu9.ari.gaiaorbit.util.gdx.g2d.ExtSpriteBatch;

public interface IAnnotationsRenderable extends IRenderable {

    void render(ExtSpriteBatch spriteBatch, ICamera camera, BitmapFont font, float alpha);
}
