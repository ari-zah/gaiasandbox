/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class PackTextures {
    public static void main(String[] args) {
        TexturePacker.Settings x1settings = new TexturePacker.Settings();
        TexturePacker.Settings x2settings = new TexturePacker.Settings();
        x2settings.scale[0] = 1.6f;

        // Use current path variable
        String gs = (new java.io.File("")).getAbsolutePath();

        // DARK-GREEN
        TexturePacker.process(x1settings, gs + "/assets/skins/raw/dark-green/", gs + "/assets/skins/dark-green/", "dark-green");

        // DARK-GREEN x2
        TexturePacker.process(x2settings, gs + "/assets/skins/raw/dark-green/", gs + "/assets/skins/dark-green-x2/", "dark-green-x2");

        // DARK-ORANGE
        TexturePacker.process(x1settings, gs + "/assets/skins/raw/dark-orange/", gs + "/assets/skins/dark-orange/", "dark-orange");

        // DARK-ORANGE x2
        TexturePacker.process(x2settings, gs + "/assets/skins/raw/dark-orange/", gs + "/assets/skins/dark-orange-x2/", "dark-orange-x2");

        // DARK-BLUE
        TexturePacker.process(x1settings, gs + "/assets/skins/raw/dark-blue/", gs + "/assets/skins/dark-blue/", "dark-blue");

        // DARK-BLUE x2
        TexturePacker.process(x2settings, gs + "/assets/skins/raw/dark-blue/", gs + "/assets/skins/dark-blue-x2/", "dark-blue-x2");

        // NIGHT-RED
        TexturePacker.process(x1settings, gs + "/assets/skins/raw/night-red/", gs + "/assets/skins/night-red/", "night-red");

        // NIGHT-RED x2
        TexturePacker.process(x2settings, gs + "/assets/skins/raw/night-red/", gs + "/assets/skins/night-red-x2/", "night-red-x2");
    }
}
