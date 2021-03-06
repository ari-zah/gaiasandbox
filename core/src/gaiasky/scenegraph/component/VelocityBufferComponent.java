/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.shader.Matrix4Attribute;
import gaiasky.util.gdx.shader.Vector3Attribute;
import gaiasky.util.math.Vector3d;

import java.util.Map;
import java.util.Set;

public class VelocityBufferComponent {

    public void doneLoading(Map<String, Material> materials) {
        Set<String> keys = materials.keySet();
        for (String key : keys) {
            Material mat = materials.get(key);
            setUpVelocityBufferMaterial(mat);
        }
    }

    public void doneLoading(Material mat) {
        setUpVelocityBufferMaterial(mat);
    }

    public void setUpVelocityBufferMaterial(Array<Material> materials) {
        for (Material material : materials) {
            setUpVelocityBufferMaterial(material);
        }
    }

    public void setUpVelocityBufferMaterial(Material mat) {
        mat.set(new Matrix4Attribute(Matrix4Attribute.PrevProjView, new Matrix4()));
        mat.set(new Vector3Attribute(Vector3Attribute.DCamPos, new Vector3()));
    }

    public void removeVelocityBufferMaterial(Material mat) {
        mat.remove(Matrix4Attribute.PrevProjView);
        mat.remove(Vector3Attribute.DCamPos);
    }

    public void updateVelocityBufferMaterial(Material material, ICamera cam) {
        if (material.get(Matrix4Attribute.PrevProjView) == null) {
            setUpVelocityBufferMaterial(material);
        }

        // Previous projection view matrix
        ((Matrix4Attribute) material.get(Matrix4Attribute.PrevProjView)).value.set(cam.getPreviousProjView());

        // Camera position difference
        Vector3 dcampos = ((Vector3Attribute) material.get(Vector3Attribute.DCamPos)).value;
        Vector3d dp = cam.getPreviousPos();
        Vector3d p = cam.getPos();
        dcampos.set((float) (dp.x - p.x), (float) (dp.y - p.y), (float) (dp.z - p.z));
    }

    public boolean hasVelocityBuffer(Material mat) {
        return mat.get(Matrix4Attribute.PrevProjView) != null;
    }
}
