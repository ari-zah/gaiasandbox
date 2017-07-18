package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureAdapter;
import com.badlogic.gdx.math.Vector3;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.XBox360Mappings;
import gaia.cu9.ari.gaiaorbit.scenegraph.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * Implements a spacecraft-like movement. The spacecraft is modeled as a rigid
 * solid and it has a mass and an engine model. The rest is physics.
 * 
 * @author tsagrista
 *
 */
public class SpacecraftCamera extends AbstractCamera implements IObserver {

    /** Direction and up vectors **/
    public Vector3d direction, up, relpos;

    private Spacecraft sc;

    /** Camera to render the attitude indicator system **/
    private PerspectiveCamera guiCam;

    /** The input controller attached to this camera **/
    private SpacecraftInputController inputController;

    /** Controller listener **/
    private SpacecraftControllerListener controllerListener;

    /** Crosshair **/
    private SpriteBatch spriteBatch;
    private Texture crosshairTex;
    private float chw2, chh2;

    private Vector3d aux2, todesired, desired, scthrust, scforce, scaccel, scvel, scpos;

    private double targetDistance;

    public SpacecraftCamera(AssetManager assetManager, CameraManager parent) {
        super(parent);

        // orientation
        direction = new Vector3d(1, 0, 0);
        up = new Vector3d(0, 1, 0);
        relpos = new Vector3d();

        todesired = new Vector3d();
        desired = new Vector3d();
        aux2 = new Vector3d();
        scthrust = new Vector3d();
        scforce = new Vector3d();
        scaccel = new Vector3d();
        scvel = new Vector3d();
        scpos = new Vector3d();

        // init camera
        camera = new PerspectiveCamera(GlobalConf.scene.CAMERA_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = (float) (100000d * Constants.M_TO_U);
        camera.far = (float) CAM_FAR;

        // init cameras vector
        cameras = new PerspectiveCamera[] { camera, camLeft, camRight };

        // init gui camera
        guiCam = new PerspectiveCamera(30, 300, 300);
        guiCam.near = (float) CAM_NEAR;
        guiCam.far = (float) CAM_FAR;

        // aspect ratio
        ar = (float) Gdx.graphics.getWidth() / (float) Gdx.graphics.getHeight();

        // fov factor
        fovFactor = camera.fieldOfView / 40f;

        inputController = new SpacecraftInputController(new GestureAdapter());
        controllerListener = new SpacecraftControllerListener();

        // Init sprite batch for crosshair and cockpit
        spriteBatch = new SpriteBatch();
        crosshairTex = new Texture(Gdx.files.internal("img/crosshair-sc-yellow.png"));
        chw2 = crosshairTex.getWidth() / 2f;
        chh2 = crosshairTex.getHeight() / 2f;

        // Focus is changed from GUI
        EventManager.instance.subscribe(this, Events.FOV_CHANGED_CMD, Events.SPACECRAFT_LOADED);
    }

    @Override
    public PerspectiveCamera getCamera() {
        return this.camera;
    }

    @Override
    public void setCamera(PerspectiveCamera cam) {
        this.camera = cam;
    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        return new PerspectiveCamera[] { camera };
    }

    @Override
    public Vector3d getDirection() {
        return direction;
    }

    @Override
    public void setDirection(Vector3d dir) {
        this.direction.set(dir);
    }

    @Override
    public Vector3d getUp() {
        return up;
    }

    @Override
    public Vector3d[] getDirections() {
        return new Vector3d[] { direction };
    }

    @Override
    public int getNCameras() {
        return 1;
    }

    double lastangle = 0;

    @Override
    public void update(double dt, ITimeFrameProvider time) {

        // FUTURE POS OF SC
        scthrust.set(sc.thrust);
        scforce.set(sc.force);
        scaccel.set(sc.accel);
        scvel.set(sc.vel);
        scpos.set(sc.pos);
        scpos = sc.computePosition(dt, sc.enginePower, scthrust, sc.direction, scforce, scaccel, scvel, scpos);

        // POSITION
        double tgfac = targetDistance * sc.factor;
        relpos.scl(sc.factor);
        aux2.set(sc.up).nor().scl(tgfac / 2d);
        desired.set(sc.direction).nor().scl(-tgfac).add(aux2);
        todesired.set(desired).sub(relpos);
        todesired.scl(dt * GlobalConf.spacecraft.SC_RESPONSIVENESS / .5e6);
        relpos.add(todesired);
        pos.set(scpos).add(relpos);
        relpos.scl(1 / sc.factor);
        distance = pos.len();

        // DIRECTION
        aux2.set(sc.direction).nor().scl(tgfac);
        direction.set(scpos).add(aux2).sub(pos).nor();

        // UP
        desired.set(sc.up);
        todesired.set(desired).sub(up);
        todesired.scl(dt * GlobalConf.spacecraft.SC_RESPONSIVENESS / .5e6);
        up.add(todesired).nor();

        // Update camera
        updatePerspectiveCamera();
        updateFrustum(frustum, camera, pos, direction, up);

        String clname = null;
        double cldist = -1d;
        if (closestStar != null) {
            double closestStarDist = closestStar.getClosestDist();
            String closestStarName = closestStar.getClosestName();
            if (closest != null) {
                if (closest.distToCamera < closestStarDist) {
                    clname = closest.name;
                    cldist = closest.distToCamera;
                } else {
                    clname = closestStarName;
                    cldist = closestStarDist;
                }
            } else {
                clname = closestStarName;
                cldist = closestStarDist;
            }
        }

        // TODO Broadcast clname and cldist

    }

    public double convertAngle(double angle) {
        if (angle <= 180)
            return angle;
        else
            return angle - 360;
    }

    protected void updatePerspectiveCamera() {

        camera.near = (float) (1 * Constants.KM_TO_U);
        if (closest != null) {
            camera.near = Math.min(camera.near, ((float) closest.pos.dst(pos) - (float) closest.getRadius()) * (float) sc.factor / 2.5f) * (float) sc.factor;
        }
        camera.position.set(0, 0, 0);
        direction.put(camera.direction);
        up.put(camera.up);

        camera.update();

        posinv.set(pos).scl(-1);

        //camera.view.getRotation(qf);

    }

    @Override
    public void updateMode(CameraMode mode, boolean postEvent) {
        InputMultiplexer im = (InputMultiplexer) Gdx.input.getInputProcessor();
        if (mode == CameraMode.Spacecraft) {
            // Register input controller
            if (!im.getProcessors().contains(inputController, true))
                im.addProcessor(im.size(), inputController);
            // Register controller listener
            Controllers.clearListeners();
            Controllers.addListener(controllerListener);
            sc.stopAllMovement();
        } else {
            // Unregister input controller
            im.removeProcessor(inputController);
            // Unregister controller listener
            Controllers.removeListener(controllerListener);
            sc.stopAllMovement();
        }
    }

    @Override
    public CameraMode getMode() {
        return parent.mode;
    }

    @Override
    public double getSpeed() {
        return parent.getSpeed();
    }

    @Override
    public IFocus getFocus() {
        return null;
    }

    @Override
    public boolean isFocus(IFocus cb) {
        return false;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case SPACECRAFT_LOADED:
            this.sc = (Spacecraft) data[0];
            this.targetDistance = sc.size * 3;
            this.relpos.set(targetDistance, targetDistance, 0);
            break;
        case FOV_CHANGED_CMD:
            float fov = MathUtilsd.clamp((float) data[0], Constants.MIN_FOV, Constants.MAX_FOV);

            for (PerspectiveCamera cam : cameras) {
                cam.fieldOfView = fov;
            }

            fovFactor = camera.fieldOfView / 40f;
            if (parent.current == this) {
                EventManager.instance.post(Events.FOV_CHANGE_NOTIFICATION, fov, fovFactor);
            }
            break;
        default:
            break;
        }

    }

    @Override
    public void render(int rw, int rh) {
        // Renders crosshair if focus mode
        if (GlobalConf.scene.CROSSHAIR && !GlobalConf.program.STEREOSCOPIC_MODE && !GlobalConf.program.CUBEMAP360_MODE) {
            spriteBatch.begin();
            spriteBatch.draw(crosshairTex, rw / 2f - chw2, rh / 2f - chh2);
            spriteBatch.end();
        }
    }

    /**
     * Input controller for the spacecraft camera
     * 
     * @author tsagrista
     *
     */
    private class SpacecraftInputController extends GestureDetector {

        public SpacecraftInputController(GestureListener listener) {
            super(listener);
        }

        @Override
        public boolean keyDown(int keycode) {
            if (GlobalConf.runtime.INPUT_ENABLED) {
                double step = 0.01;
                switch (keycode) {
                case Keys.W:
                    // power 1
                    sc.setEnginePower(sc.enginePower + step);
                    EventManager.instance.post(Events.SPACECRAFT_STOP_CMD, false);
                    break;
                case Keys.S:
                    // power -1
                    sc.setEnginePower(sc.enginePower - step);
                    EventManager.instance.post(Events.SPACECRAFT_STOP_CMD, false);
                    break;
                case Keys.A:
                    // roll 1
                    sc.setRollPower(sc.rollp + step);
                    EventManager.instance.post(Events.SPACECRAFT_STOP_CMD, false);
                    break;
                case Keys.D:
                    // roll -1
                    sc.setRollPower(sc.rollp - step);
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, false);
                    break;
                case Keys.DOWN:
                    // pitch 1
                    sc.setPitchPower(sc.pitchp + step);
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, false);
                    break;
                case Keys.UP:
                    // pitch -1
                    sc.setPitchPower(sc.pitchp - step);
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, false);
                    break;
                case Keys.LEFT:
                    // yaw 1
                    sc.setYawPower(sc.yawp + step);
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, false);
                    break;
                case Keys.RIGHT:
                    // yaw -1
                    sc.setYawPower(sc.yawp - step);
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, false);
                    break;
                case Keys.PAGE_UP:
                    // Increase thrust factor
                    sc.increaseThrustFactorIndex(true);
                    break;
                case Keys.PAGE_DOWN:
                    // Decrease thrust length
                    sc.decreaseThrustFactorIndex(true);
                    break;
                }
            }
            return false;

        }

        @Override
        public boolean keyUp(int keycode) {
            if (GlobalConf.runtime.INPUT_ENABLED) {
                switch (keycode) {
                case Keys.W:
                case Keys.S:
                    // power 0
                    sc.setEnginePower(0);
                    break;
                case Keys.D:
                case Keys.A:
                    // roll 0
                    sc.setRollPower(0);
                    break;
                case Keys.UP:
                case Keys.DOWN:
                    // pitch 0
                    sc.setPitchPower(0);
                    break;
                case Keys.RIGHT:
                case Keys.LEFT:
                    // yaw 0
                    sc.setYawPower(0);
                    break;
                case Keys.L:
                    // level spaceship
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, true);
                    break;
                case Keys.P:
                    // stop spaceship
                    EventManager.instance.post(Events.SPACECRAFT_STOP_CMD, true);
                    break;
                }
            }
            return false;

        }

    }

    private class SpacecraftControllerListener implements ControllerListener {

        public SpacecraftControllerListener() {
            super();
        }

        @Override
        public void connected(Controller controller) {
            EventManager.instance.post(Events.POST_NOTIFICATION, this.getClass().getSimpleName(), "Controller connected: " + controller.getName());
        }

        @Override
        public void disconnected(Controller controller) {
            EventManager.instance.post(Events.POST_NOTIFICATION, this.getClass().getSimpleName(), "Controller disconnected: " + controller.getName());
        }

        @Override
        public boolean buttonDown(Controller controller, int buttonCode) {
            switch (buttonCode) {
            case XBox360Mappings.BUTTON_A:
                sc.decreaseThrustFactorIndex(true);
                break;
            case XBox360Mappings.BUTTON_X:
                sc.increaseThrustFactorIndex(true);
                break;
            case XBox360Mappings.BUTTON_LB:
                System.out.println("LB");
                EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, true);
                break;
            case XBox360Mappings.BUTTON_RB:
                EventManager.instance.post(Events.SPACECRAFT_STOP_CMD, true);
                break;
            }
            return true;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            return false;
        }

        @Override
        public boolean axisMoved(Controller controller, int axisCode, float value) {
            boolean treated = false;
            // y = x^4
            // http://www.wolframalpha.com/input/?i=y+%3D+sign%28x%29+*+x%5E2+%28x+from+-1+to+1%29}
            value = Math.signum(value) * value * value * value * value;

            switch (axisCode) {
            case XBox360Mappings.AXIS_JOY2HOR:
                sc.setRollPower(-value);
                if (Math.abs(value) > 0.3)
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, false);
                treated = true;
                break;
            case XBox360Mappings.AXIS_JOY1VERT:
                sc.setPitchPower(value);
                if (Math.abs(value) > 0.3)
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, false);
                treated = true;
                break;
            case XBox360Mappings.AXIS_JOY1HOR:
                sc.setYawPower(-value);
                if (Math.abs(value) > 0.3)
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, false);
                treated = true;
                break;
            case XBox360Mappings.AXIS_JOY2VERT:
                treated = true;
                break;
            case XBox360Mappings.AXIS_RT:
                sc.setEnginePower((value + 1) / 2);
                if (Math.abs(value) > 0.3)
                    EventManager.instance.post(Events.SPACECRAFT_STOP_CMD, false);
                treated = true;
                break;
            case XBox360Mappings.AXIS_LT:
                sc.setEnginePower(-(value + 1) / 2);
                if (Math.abs(value) > 0.3)
                    EventManager.instance.post(Events.SPACECRAFT_STOP_CMD, false);
                treated = true;
                break;

            }
            return treated;
        }

        @Override
        public boolean povMoved(Controller controller, int povCode, PovDirection value) {
            return false;
        }

        @Override
        public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
            return false;
        }

        @Override
        public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
            return false;
        }

        @Override
        public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
            return false;
        }

    }

    @Override
    public void resize(int width, int height) {
        spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

}
