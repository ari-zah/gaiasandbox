package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.desktop.util.camera.Keyframe;
import gaia.cu9.ari.gaiaorbit.render.I3DTextRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.FovCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class KeyframesPathObject extends VertsObject implements I3DTextRenderable {
    private static float[] ggreen = new float[] { 0f / 255f, 135f / 255f, 68f / 255f, 1f };
    private static float[] gblue = new float[] { 0f / 255f, 87f / 255f, 231f / 255f, 1f };
    private static float[] gred = new float[] { 214f / 255f, 45f / 255f, 32f / 255f, 1f };
    private static float[] gyellow = new float[] { 255f / 255f, 167f / 255f, 0f / 255f, 1f };
    private static float[] gwhite = new float[] { 255f / 255f, 255f / 255f, 255f / 255f, 1f };
    private static float[] gpink = new float[] { 255f / 255f, 102f / 255f, 255f / 255f, 1f };

    /**
     * Keyframe objects
     */
    public Array<Keyframe> keyframes;
    /** Selected keyframe **/
    public Keyframe selected = null;

    /** The actual path **/
    public VertsObject path;
    /** The segments joining the knots **/
    public VertsObject segments;
    /** The knots, or keyframe positions **/
    public VertsObject knots;
    /** Selected knot **/
    public VertsObject selectedKnot;

    /** Contains pairs of {direction, up} representing the orientation at each knot **/
    public Array<VertsObject> orientations;

    /** Objects **/
    private Array<VertsObject> objects;

    public KeyframesPathObject() {
        super(null);

    }

    public void initialize() {
        orientations = new Array<>();

        path = new Polyline();
        path.setName("Keyframes.path");
        path.ct = this.ct;
        path.setColor(ggreen);
        path.setClosedLoop(false);
        path.setPrimitiveSize(2f);
        path.initialize();

        segments = new Polyline();
        segments.setName("Keyframes.segments");
        segments.ct = this.ct;
        segments.setColor(gyellow);
        segments.setClosedLoop(false);
        segments.setPrimitiveSize(1f);
        segments.initialize();

        knots = new VertsObject(RenderGroup.POINT_GPU);
        knots.setName("Keyframes.knots");
        knots.ct = this.ct;
        knots.setColor(gwhite);
        knots.setClosedLoop(false);
        knots.setPrimitiveSize(4f);
        knots.initialize();

        selectedKnot = new VertsObject(RenderGroup.POINT_GPU);
        selectedKnot.setName("Keyframes.selknot");
        selectedKnot.ct = this.ct;
        selectedKnot.setColor(gpink);
        selectedKnot.setClosedLoop(false);
        selectedKnot.setPrimitiveSize(8f);
        selectedKnot.setDepth(false);
        selectedKnot.initialize();

        objects = new Array<>();
        objects.add(path);
        objects.add(segments);
        objects.add(knots);
        objects.add(selectedKnot);

    }

    public void setKeyframes(Array<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }

    public void setPathKnots(double[] kts, double[] dirs, double[] ups) {
        // Points
        knots.setPoints(kts);
        clearOrientations();
        int n = kts.length;
        for (int i = 0; i < n; i += 3) {
            addKnotOrientation(i / 3, kts[i], kts[i + 1], kts[i + 2], dirs[i], dirs[i + 1], dirs[i + 2], ups[i], ups[i + 1], ups[i + 2]);
        }
    }

    public void addKnot(Vector3d knot, Vector3d dir, Vector3d up) {
        knots.addPoint(knot);
        addKnotOrientation(orientations.size / 2, knot.x, knot.y, knot.z, dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    private void addKnotOrientation(int idx, double px, double py, double pz, double dx, double dy, double dz, double ux, double uy, double uz) {
        VertsObject dir = new Polyline();
        dir.setName("Keyframes.dir" + idx);
        dir.ct = this.ct;
        dir.setColor(gred);
        dir.setClosedLoop(false);
        dir.setPrimitiveSize(1f);
        dir.initialize();

        VertsObject up = new Polyline();
        up.setName("Keyframes.up" + idx);
        up.ct = this.ct;
        up.setColor(gblue);
        up.setClosedLoop(false);
        up.setPrimitiveSize(1f);
        up.initialize();

        dir.setPoints(new double[] { px, py, pz, px + dx, py + dy, pz + dz });
        up.setPoints(new double[] { px, py, pz, px + ux, py + uy, pz + uz });

        objects.add(dir);
        objects.add(up);

        orientations.add(dir);
        orientations.add(up);
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        for (VertsObject vo : objects)
            vo.update(time, parentTransform, camera, opacity);

        // Update length of orientations
        for (VertsObject vo : orientations) {
            Vector3d p0 = aux3d1.get();
            Vector3d p1 = aux3d2.get();
            PointCloudData p = vo.pointCloudData;
            p0.set(p.x.get(0), p.y.get(0), p.z.get(0));
            p1.set(p.x.get(1), p.y.get(1), p.z.get(1));

            Vector3d c = aux3d3.get().set(camera.getPos());
            double len = Math.max(0.00005, Math.atan(0.02) * c.dst(p0));

            Vector3d v = c.set(p1).sub(p0).nor().scl(len);
            p.x.set(1, p0.x + v.x);
            p.y.set(1, p0.y + v.y);
            p.z.set(1, p0.z + v.z);
            vo.markForUpdate();
        }

        this.addToRenderLists(camera);
    }

    public boolean select(int screenX, int screenY, int w, int h, int minPixDist, NaturalCamera camera) {

        Vector3 pos = aux3f1.get();
        Vector3d aux = aux3d1.get();
        for (Keyframe kf : keyframes) {
            Vector3d posd = aux.set(kf.pos).add(camera.getInversePos());
            pos.set(posd.valuesf());

            if (camera.direction.dot(posd) > 0) {
                // The object is in front of us
                double angle = 0.001;

                PerspectiveCamera pcamera;
                if (GlobalConf.program.STEREOSCOPIC_MODE) {
                    if (screenX < Gdx.graphics.getWidth() / 2f) {
                        pcamera = camera.getCameraStereoLeft();
                        pcamera.update();
                    } else {
                        pcamera = camera.getCameraStereoRight();
                        pcamera.update();
                    }
                } else {
                    pcamera = camera.camera;
                }

                angle = (float) Math.toDegrees(angle * camera.getFovFactor()) * (40f / pcamera.fieldOfView);
                double pixelSize = Math.max(minPixDist, ((angle * pcamera.viewportHeight) / pcamera.fieldOfView) / 2);
                pcamera.project(pos);
                pos.y = pcamera.viewportHeight - pos.y;
                if (GlobalConf.program.STEREOSCOPIC_MODE) {
                    pos.x /= 2;
                }
                // Check click distance
                if (checkClickDistance(screenX, screenY, pos, camera, pcamera, pixelSize)) {
                    //Hit
                    select(kf);
                    return true;
                }
            }
        }
        // Not hit, unselect
        unselect();
        return false;
    }

    protected boolean checkClickDistance(int screenX, int screenY, Vector3 pos, NaturalCamera camera, PerspectiveCamera pcamera, double pixelSize) {
        return pos.dst(screenX % pcamera.viewportWidth, screenY, pos.z) <= pixelSize;
    }

    public void select(Keyframe kf) {
        selected = kf;
        selectedKnot.setPoints(kf.pos.values());
    }

    public void unselect() {
        selected = null;
        selectedKnot.clear();
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (selected != null) {
            addToRender(this, RenderGroup.FONT_LABEL);
        }
    }

    @Override
    public void clear() {
        // Clear GPU objects
        for (VertsObject vo : objects)
            vo.clear();

        // Clear orientations
        clearOrientations();

        // Unselect
        unselect();
    }

    public boolean isEmpty() {
        return keyframes.isEmpty() && knots.isEmpty() && path.isEmpty() && segments.isEmpty();
    }

    public void clearOrientations() {
        for (VertsObject vo : orientations)
            objects.removeValue(vo, true);
        orientations.clear();
    }

    @Override
    public boolean renderText() {
        return selected != null;
    }

    @Override
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (camera.getCurrent() instanceof FovCamera) {
        } else {
            // 3D distance font
            Vector3d pos = aux3d1.get();
            textPosition(camera, pos);
            float distToCam = (float) aux3d2.get().set(selected.pos).add(camera.getInversePos()).len();
            shader.setUniformf("u_viewAngle", 90f);
            shader.setUniformf("u_viewAnglePow", 1);
            shader.setUniformf("u_thOverFactor", 1);
            shader.setUniformf("u_thOverFactorScl", 1);

            render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor() * distToCam);
        }

    }

    @Override
    public float[] textColour() {
        return gpink;
    }

    @Override
    public float textSize() {
        return 1e-3f;
    }

    @Override
    public float textScale() {
        return .3f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        selected.pos.put(out).add(cam.getInversePos());

        Vector3d aux = aux3d2.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        aux.add(cam.getUp()).nor().scl(-2e-3f);

        out.add(aux);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return selected.name;
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    @Override
    public boolean isLabel() {
        return false;
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }
}
