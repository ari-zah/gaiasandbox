package gaia.cu9.ari.gaiaorbit.script;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.TimeUtils;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.EventManager.TimeFrame;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.ControlsWindow;
import gaia.cu9.ari.gaiaorbit.interfce.IGui;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.Invisible;
import gaia.cu9.ari.gaiaorbit.scenegraph.Loc;
import gaia.cu9.ari.gaiaorbit.scenegraph.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.Planet;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.LruCache;
import gaia.cu9.ari.gaiaorbit.util.math.Intersectord;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Implementation of the scripting interface using the event system.
 * 
 * @author Toni Sagrista
 *
 */
public class EventScriptingInterface implements IScriptingInterface, IObserver {
    private EventManager em;
    private AssetManager manager;
    private LruCache<String, Texture> textures;

    private static EventScriptingInterface instance = null;

    public static EventScriptingInterface instance() {
        if (instance == null) {
            instance = new EventScriptingInterface();
        }
        return instance;
    }

    private Vector3d aux1, aux2, aux3, aux4;

    private Set<AtomicBoolean> stops;

    private EventScriptingInterface() {
        em = EventManager.instance;
        manager = GaiaSky.instance.manager;

        stops = new HashSet<AtomicBoolean>();

        aux1 = new Vector3d();
        aux2 = new Vector3d();
        aux3 = new Vector3d();
        aux4 = new Vector3d();

        em.subscribe(this, Events.INPUT_EVENT, Events.DISPOSE);
    }

    public void initializeTextures() {
        if (textures == null) {
            textures = new LruCache<String, Texture>(100);
        }
    }

    @Override
    public void activateRealTimeFrame() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.EVENT_TIME_FRAME_CMD, TimeFrame.REAL_TIME);
            }
        });
    }

    @Override
    public void activateSimulationTimeFrame() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.EVENT_TIME_FRAME_CMD, TimeFrame.SIMULATION_TIME);
            }
        });
    }

    @Override
    public void setHeadlineMessage(final String headline) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.POST_HEADLINE_MESSAGE, headline);
            }
        });
    }

    @Override
    public void setSubheadMessage(final String subhead) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.POST_SUBHEAD_MESSAGE, subhead);
            }
        });
    }

    @Override
    public void clearHeadlineMessage() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CLEAR_HEADLINE_MESSAGE);
            }
        });
    }

    @Override
    public void clearSubheadMessage() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CLEAR_SUBHEAD_MESSAGE);
            }
        });
    }

    @Override
    public void clearAllMessages() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CLEAR_MESSAGES);
            }
        });
    }

    @Override
    public void disableInput() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.INPUT_ENABLED_CMD, false);
            }
        });
    }

    @Override
    public void enableInput() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.INPUT_ENABLED_CMD, true);
            }
        });
    }

    @Override
    public void setCameraFocus(final String focusName) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.FOCUS_CHANGE_CMD, focusName, true);
            }
        });
    }

    @Override
    public void setCameraLock(final boolean lock) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.FOCUS_LOCK_CMD, I18n.bundle.get("gui.camera.lock"), lock);
            }
        });
    }

    @Override
    public void setCameraFree() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_MODE_CMD, CameraMode.Free_Camera);
            }
        });
    }

    @Override
    public void setCameraFov1() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_MODE_CMD, CameraMode.Gaia_FOV1);
            }
        });
    }

    @Override
    public void setCameraFov2() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_MODE_CMD, CameraMode.Gaia_FOV2);
            }
        });
    }

    @Override
    public void setCameraFov1and2() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_MODE_CMD, CameraMode.Gaia_FOV1and2);
            }
        });
    }

    @Override
    public void setCameraPostion(final double[] vec) {
        setCameraPosition(vec);
    }

    @Override
    public void setCameraPosition(final double[] vec) {
        if (vec.length != 3)
            throw new RuntimeException("vec parameter must have three components");
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                // Convert to km
                vec[0] = vec[0] * Constants.KM_TO_U;
                vec[1] = vec[1] * Constants.KM_TO_U;
                vec[2] = vec[2] * Constants.KM_TO_U;
                // Send event
                em.post(Events.CAMERA_POS_CMD, vec);
            }
        });
    }

    @Override
    public double[] getCameraPosition() {
        Vector3d campos = GaiaSky.instance.cam.getPos();
        return new double[] { campos.x * Constants.U_TO_KM, campos.y * Constants.U_TO_KM, campos.z * Constants.U_TO_KM };
    }

    @Override
    public void setCameraDirection(final double[] dir) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_DIR_CMD, dir);
            }
        });
    }

    @Override
    public double[] getCameraDirection() {
        Vector3d camdir = GaiaSky.instance.cam.getDirection();
        return new double[] { camdir.x, camdir.y, camdir.z };
    }

    @Override
    public void setCameraUp(final double[] up) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_UP_CMD, up);
            }
        });
    }

    @Override
    public double[] getCameraUp() {
        Vector3d camup = GaiaSky.instance.cam.getUp();
        return new double[] { camup.x, camup.y, camup.z };
    }

    @Override
    public void setCameraSpeed(final float speed) {
        assert speed >= Constants.MIN_SLIDER && speed <= Constants.MAX_SLIDER : "Speed must be between " + Constants.MIN_SLIDER + " and " + Constants.MAX_SLIDER;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_SPEED_CMD, speed / 10f, false);
            }
        });
    }

    @Override
    public double getCameraSpeed() {
        return GaiaSky.instance.cam.getSpeed();
    }

    @Override
    public void setRotationCameraSpeed(final float speed) {
        assert speed >= Constants.MIN_SLIDER && speed <= Constants.MAX_SLIDER : "Speed must be between " + Constants.MIN_SLIDER + " and " + Constants.MAX_SLIDER;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.ROTATION_SPEED_CMD, MathUtilsd.lint(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED), false);
            }
        });
    }

    @Override
    public void setTurningCameraSpeed(final float speed) {
        assert speed >= Constants.MIN_SLIDER && speed <= Constants.MAX_SLIDER : "Speed must be between " + Constants.MIN_SLIDER + " and " + Constants.MAX_SLIDER;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.TURNING_SPEED_CMD, MathUtilsd.lint(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED), false);

            }
        });
    }

    @Override
    public void cameraForward(final double value) {
        assert value >= 0d && value <= 1d : "Value must be between 0 and 1";
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_FWD, value);
            }
        });

    }

    @Override
    public void cameraRotate(final double deltaX, final double deltaY) {
        assert deltaX >= 0d && deltaX <= 1d && deltaY >= 0d && deltaY <= 1d : "DeltaX and deltaY must be between 0 and 1";
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_ROTATE, deltaX, deltaY);
            }
        });

    }

    @Override
    public void cameraRoll(final double roll) {
        assert roll >= 0d && roll <= 1d : "Roll must be between 0 and 1";
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_ROLL, roll);
            }
        });
    }

    @Override
    public void cameraTurn(final double deltaX, final double deltaY) {
        assert deltaX >= 0d && deltaX <= 1d && deltaY >= 0d && deltaY <= 1d : "DeltaX and deltaY must be between 0 and 1";
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_TURN, deltaX, deltaY);
            }
        });
    }

    @Override
    public void cameraStop() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_STOP);
            }
        });

    }

    @Override
    public void cameraCenter() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.CAMERA_CENTER);
            }
        });
    }

    @Override
    public CelestialBody getClosestObjectToCamera() {
        return GaiaSky.instance.cam.getClosest();
    }

    @Override
    public void setFov(final float newFov) {
        assert newFov >= Constants.MIN_FOV && newFov <= Constants.MAX_FOV : "Fov value must be between " + Constants.MIN_FOV + " and " + Constants.MAX_FOV;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.FOV_CHANGED_CMD, newFov);
            }
        });
    }

    @Override
    public void setVisibility(final String key, final boolean visible) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.TOGGLE_VISIBILITY_CMD, key, false, visible);
            }
        });
    }

    @Override
    public void setAmbientLight(final float value) {
        assert value >= Constants.MIN_SLIDER && value <= Constants.MAX_SLIDER : "Value must be between 0 and 100";
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.AMBIENT_LIGHT_CMD, value / 100f);
            }
        });
    }

    @Override
    public void setSimulationTime(final long time) {
        assert time > 0 : "Time can not be negative";
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.TIME_CHANGE_CMD, new Date(time));
            }
        });
    }

    @Override
    public void startSimulationTime() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.TOGGLE_TIME_CMD, true, false);
            }
        });
    }

    @Override
    public void stopSimulationTime() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.TOGGLE_TIME_CMD, false, false);
            }
        });
    }

    @Override
    public void setSimulationPace(final double pace) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.PACE_CHANGE_CMD, pace);
            }
        });
    }

    @Override
    public void setStarBrightness(final float brightness) {
        assert brightness >= Constants.MIN_SLIDER && brightness <= Constants.MAX_SLIDER : "Brightness value must be between 0 and 100";
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.STAR_BRIGHTNESS_CMD, MathUtilsd.lint(brightness, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_STAR_BRIGHT, Constants.MAX_STAR_BRIGHT), false);
            }
        });
    }

    @Override
    public void configureRenderOutput(int width, int height, int fps, String folder, String namePrefix) {
        assert width > 0 : "Width must be positive";
        assert height > 0 : "Height must be positive";
        assert fps > 0 : "FPS must be positive";
        assert folder != null && namePrefix != null : "Folder and file name prefix must not be null";
        em.post(Events.CONFIG_PIXEL_RENDERER, width, height, fps, folder, namePrefix);
    }

    @Override
    public boolean isRenderOutputActive() {
        return GlobalConf.frame.RENDER_OUTPUT;
    }

    @Override
    public int getRenderOutputFps() {
        return GlobalConf.frame.RENDER_TARGET_FPS;
    }

    @Override
    public void setFrameOutput(boolean active) {
        em.post(Events.FRAME_OUTPUT_CMD, active);
    }

    @Override
    public CelestialBody getObject(String name) {
        ISceneGraph sg = GaiaSky.instance.sg;
        return sg.findFocus(name);
    }

    @Override
    public double getObjectRadius(String name) {
        ISceneGraph sg = GaiaSky.instance.sg;
        CelestialBody obj = sg.findFocus(name);
        if (obj == null)
            return -1;
        else
            return obj.getRadius() * Constants.U_TO_KM;
    }

    @Override
    public void goToObject(String name) {
        goToObject(name, -1);
    }

    @Override
    public void goToObject(String name, double angle) {
        goToObject(name, angle, -1);
    }

    @Override
    public void goToObject(String name, double angle, float focusWait) {
        goToObject(name, angle, focusWait, null);
    }

    public void goToObject(String name, double angle, float focusWait, AtomicBoolean stop) {
        assert name != null : "Name can't be null";
        assert angle > 0 : "Angle must be larger than zero";

        stops.add(stop);
        ISceneGraph sg = GaiaSky.instance.sg;
        if (sg.containsNode(name)) {
            CelestialBody focus = sg.findFocus(name);
            NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;

            // Post focus change
            em.post(Events.CAMERA_MODE_CMD, CameraMode.Focus);
            em.post(Events.FOCUS_CHANGE_CMD, name);

            // Wait til camera is facing focus
            if (focusWait < 0) {
                while (!cam.facingFocus) {
                    // Wait
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                }
            } else {
                this.sleep(focusWait);
            }

            /* target angle */
            double target = Math.toRadians(angle);
            if (target < 0)
                target = Math.toRadians(20d);

            // Add forward movement while distance > target distance
            long prevtime = TimeUtils.millis();
            while (focus.viewAngleApparent < target && (stop == null || (stop != null && !stop.get()))) {
                // dt in ms
                long dt = TimeUtils.timeSinceMillis(prevtime);
                prevtime = TimeUtils.millis();

                em.post(Events.CAMERA_FWD, 1d * dt);
                try {
                    Thread.sleep(5);
                } catch (Exception e) {
                }
            }

            // We can stop now
            em.post(Events.CAMERA_STOP);

        }
    }

    @Override
    public void landOnObject(String name) {
        landOnObject(name, null);
    }

    public void landOnObject(String name, AtomicBoolean stop) {
        assert name != null : "Name can't be null";

        stops.add(stop);
        ISceneGraph sg = GaiaSky.instance.sg;
        if (sg.containsNode(name)) {
            CelestialBody focus = sg.findFocus(name);
            if (focus instanceof Planet) {
                NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;
                // Focus wait - 2 seconds
                float focusWait = -1;

                // Post focus change
                em.post(Events.CAMERA_MODE_CMD, CameraMode.Focus);
                em.post(Events.FOCUS_CHANGE_CMD, name);

                // Wait til camera is facing focus
                if (focusWait < 0) {
                    while (!cam.facingFocus) {
                        // Wait
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                        }
                    }
                } else {
                    this.sleep(focusWait);
                }

                /* target distance */
                double target = 100 * Constants.M_TO_U;

                focus.getAbsolutePosition(aux1).add(cam.posinv).nor();
                Vector3d dir = cam.direction;

                // Save speed, set it to 50
                double speed = GlobalConf.scene.CAMERA_SPEED;
                em.post(Events.CAMERA_SPEED_CMD, 25f / 10f, false);

                // Save turn speed, set it to 50
                double turnSpeedBak = GlobalConf.scene.TURNING_SPEED;
                em.post(Events.TURNING_SPEED_CMD, (float) MathUtilsd.lint(50d, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED), false);

                // Save cinematic
                boolean cinematic = GlobalConf.scene.CINEMATIC_CAMERA;
                GlobalConf.scene.CINEMATIC_CAMERA = true;

                // Add forward movement while distance > target distance
                boolean distanceNotMet = (focus.distToCamera - focus.getRadius()) > target;
                boolean viewNotMet = Math.abs(dir.angle(aux1)) < 90;
                long prevtime = TimeUtils.millis();
                while ((distanceNotMet || viewNotMet) && (stop == null || (stop != null && !stop.get()))) {
                    // dt in ms
                    long dt = TimeUtils.timeSinceMillis(prevtime);
                    prevtime = TimeUtils.millis();

                    if (distanceNotMet)
                        em.post(Events.CAMERA_FWD, 0.1d * dt);
                    else
                        cam.stopForwardMovement();

                    if (viewNotMet) {
                        if (focus.distToCamera - focus.getRadius() < focus.getRadius() * 5)
                            // Start turning where we are at n times the radius
                            em.post(Events.CAMERA_TURN, 0d, dt / 500d);
                    } else {
                        cam.stopTurnMovement();
                    }

                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }

                    // focus.transform.getTranslation(aux);
                    viewNotMet = Math.abs(dir.angle(aux1)) < 90;
                    distanceNotMet = (focus.distToCamera - focus.getRadius()) > target;
                }

                // Restore cinematic
                GlobalConf.scene.CINEMATIC_CAMERA = cinematic;

                // Restore speed
                em.post(Events.CAMERA_SPEED_CMD, (float) speed, false);

                // Restore turning speed
                em.post(Events.TURNING_SPEED_CMD, (float) turnSpeedBak, false);

                // We can stop now
                em.post(Events.CAMERA_STOP);
            }
        }

    }

    @Override
    public void landOnObjectLocation(String name, String locationName) {
        landOnObjectLocation(name, locationName, null);
    }

    public void landOnObjectLocation(String name, String locationName, AtomicBoolean stop) {
        assert name != null : "Name can't be null";
        assert locationName != null : "locationName can't be null";

        stops.add(stop);
        ISceneGraph sg = GaiaSky.instance.sg;
        if (sg.containsNode(name)) {
            CelestialBody focus = sg.findFocus(name);
            if (focus instanceof Planet) {
                Planet planet = (Planet) focus;
                SceneGraphNode sgn = planet.getChildByNameAndType(locationName, Loc.class);
                if (sgn != null) {
                    Loc location = (Loc) sgn;
                    landOnObjectLocation(name, location.getLocation().x, location.getLocation().y, stop);
                    return;
                }
                Logger.info("Location '" + locationName + "' not found on object '" + name + "'");
            }
        }
    }

    @Override
    public void landOnObjectLocation(String name, double longitude, double latitude) {
        landOnObjectLocation(name, longitude, latitude, null);
    }

    public void landOnObjectLocation(String name, double longitude, double latitude, AtomicBoolean stop) {
        assert name != null : "Name can't be null";
        assert latitude >= -90 && latitude <= 90 && longitude >= 0 && longitude <= 360 : "Latitude must be in [-90..90] and longitude must be in [0..360]";

        stops.add(stop);
        ISceneGraph sg = GaiaSky.instance.sg;
        String nameStub = name + " ";

        if (!sg.containsNode(nameStub)) {
            Invisible invisible = new Invisible(nameStub);
            sg.insert(invisible, true);
        }
        Invisible invisible = (Invisible) sg.getNode(nameStub);

        if (sg.containsNode(name)) {
            CelestialBody focus = sg.findFocus(name);
            if (focus instanceof Planet) {
                Planet planet = (Planet) focus;
                NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;

                // Save speed, set it to 50
                double speed = GlobalConf.scene.CAMERA_SPEED;
                em.post(Events.CAMERA_SPEED_CMD, 25f / 10f, false);

                // Save turn speed, set it to 50
                double turnSpeedBak = GlobalConf.scene.TURNING_SPEED;
                em.post(Events.TURNING_SPEED_CMD, (float) MathUtilsd.lint(50d, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED), false);

                // Save rotation speed, set it to 20
                double rotationSpeedBak = GlobalConf.scene.ROTATION_SPEED;
                em.post(Events.ROTATION_SPEED_CMD, (float) MathUtilsd.lint(20d, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED), false);

                // Save cinematic
                boolean cinematic = GlobalConf.scene.CINEMATIC_CAMERA;
                GlobalConf.scene.CINEMATIC_CAMERA = true;

                // Save crosshair
                boolean crosshair = GlobalConf.scene.CROSSHAIR;
                GlobalConf.scene.CROSSHAIR = false;

                // Get target position
                Vector3d target = aux1;
                planet.getPositionAboveSurface(longitude, latitude, 50, target);

                // Get object position
                Vector3d objectPosition = planet.getAbsolutePosition(aux2);

                // Check intersection with object
                boolean intersects = Intersectord.checkIntersectSegmentSphere(cam.pos, target, objectPosition, planet.getRadius());

                if (intersects) {
                    cameraRotate(5, 5);
                }

                while (intersects && (stop == null || (stop != null && !stop.get()))) {
                    sleep(0.1f);

                    objectPosition = planet.getAbsolutePosition(aux2);
                    intersects = Intersectord.checkIntersectSegmentSphere(cam.pos, target, objectPosition, planet.getRadius());
                }

                cameraStop();

                invisible.ct = planet.ct;
                invisible.pos.set(target);

                // Go to object
                goToObject(nameStub, 20, 0, stop);

                // Restore cinematic
                GlobalConf.scene.CINEMATIC_CAMERA = cinematic;

                // Restore speed
                em.post(Events.CAMERA_SPEED_CMD, (float) speed, false);

                // Restore turning speed
                em.post(Events.TURNING_SPEED_CMD, (float) turnSpeedBak, false);

                // Restore rotation speed
                em.post(Events.ROTATION_SPEED_CMD, (float) rotationSpeedBak, false);

                // Restore crosshair
                GlobalConf.scene.CROSSHAIR = crosshair;

                // Land
                landOnObject(name, stop);
            }
        }

        sg.remove(invisible, true);
    }

    @Override
    public double getDistanceTo(String name) {
        ISceneGraph sg = GaiaSky.instance.sg;
        if (sg.containsNode(name)) {
            SceneGraphNode object = sg.getNode(name);
            if (object instanceof AbstractPositionEntity) {
                AbstractPositionEntity ape = (AbstractPositionEntity) object;
                return (ape.distToCamera - ape.getRadius()) * Constants.U_TO_KM;
            }
        }
        return -1;
    }

    @Override
    public void setGuiScrollPosition(final float pixelY) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.GUI_SCROLL_POSITION_CMD, pixelY);
            }
        });
    }

    @Override
    public void displayMessageObject(final int id, final String message, final float x, final float y, final float r, final float g, final float b, final float a, final float fontSize) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.ADD_CUSTOM_MESSAGE, id, message, x, y, r, g, b, a, fontSize);
            }
        });
    }

    @Override
    public void displayTextObject(final int id, final String text, final float x, final float y, final float maxWidth, final float maxHeight, final float r, final float g, final float b, final float a, final float fontSize) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.ADD_CUSTOM_TEXT, id, text, x, y, maxWidth, maxHeight, r, g, b, a, fontSize);
            }
        });

    }

    @Override
    public void displayImageObject(final int id, final String path, final float x, final float y, final float r, final float g, final float b, final float a) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                Texture tex = getTexture(path);
                em.post(Events.ADD_CUSTOM_IMAGE, id, tex, x, y, r, g, b, a);
            }
        });

    }

    @Override
    public void displayImageObject(final int id, final String path, final float x, final float y) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                Texture tex = getTexture(path);
                em.post(Events.ADD_CUSTOM_IMAGE, id, tex, x, y);
            }
        });
    }

    @Override
    public void removeAllObjects() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.REMOVE_ALL_OBJECTS);
            }
        });
    }

    @Override
    public void removeObject(final int id) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.REMOVE_OBJECTS, new int[] { id });
            }
        });
    }

    @Override
    public void removeObjects(final int[] ids) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.REMOVE_OBJECTS, ids);
            }
        });
    }

    @Override
    public void maximizeInterfaceWindow() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.GUI_FOLD_CMD, false);
            }
        });
    }

    @Override
    public void minimizeInterfaceWindow() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.GUI_FOLD_CMD, true);
            }
        });
    }

    @Override
    public void setGuiPosition(final float x, final float y) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                em.post(Events.GUI_MOVE_CMD, x, y);
            }
        });
    }

    @Override
    public void waitForInput() {
        while (inputCode < 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                em.post(Events.JAVA_EXCEPTION, e);
            }
        }
        // Consume
        inputCode = -1;

    }

    @Override
    public void waitForEnter() {
        while (inputCode != Keys.ENTER) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                em.post(Events.JAVA_EXCEPTION, e);
            }
        }
        // Consume
        inputCode = -1;
    }

    @Override
    public void waitForInput(int keyCode) {
        while (inputCode != keyCode) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                em.post(Events.JAVA_EXCEPTION, e);
            }
        }
        // Consume
        inputCode = -1;
    }

    int inputCode = -1;

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case INPUT_EVENT:
            inputCode = (Integer) data[0];
            break;
        case DISPOSE:
            // Stop all
            for (AtomicBoolean stop : stops) {
                stop.set(true);
            }
            break;
        default:
            break;
        }

    }

    @Override
    public int getScreenWidth() {
        return Gdx.graphics.getWidth();
    }

    @Override
    public int getScreenHeight() {
        return Gdx.graphics.getHeight();
    }

    @Override
    public float[] getPositionAndSizeGui(String name) {
        IGui gui = GaiaSky.instance.mainGui;
        Actor actor = gui.getGuiStage().getRoot().findActor(name);
        if (actor != null) {
            float x = actor.getX();
            float y = actor.getY();
            // x and y relative to parent, so we need to add coordinates of
            // parents up to top
            Group parent = actor.getParent();
            while (parent != null) {
                x += parent.getX();
                y += parent.getY();
                parent = parent.getParent();
            }
            return new float[] { x, y, actor.getWidth(), actor.getHeight() };
        } else {
            return null;
        }

    }

    @Override
    public void expandGuiComponent(String name) {
        IGui gui = GaiaSky.instance.mainGui;
        ControlsWindow controls = (ControlsWindow) gui.getGuiStage().getRoot().findActor(I18n.bundle.get("gui.controlpanel"));
        controls.getCollapsiblePane(name).expandPane();
    }

    @Override
    public void collapseGuiComponent(String name) {
        IGui gui = GaiaSky.instance.mainGui;
        ControlsWindow controls = (ControlsWindow) gui.getGuiStage().getRoot().findActor(I18n.bundle.get("gui.controlpanel"));
        controls.getCollapsiblePane(name).collapsePane();
    }

    @Override
    public String getVersionNumber() {
        return GlobalConf.version.version;
    }

    @Override
    public boolean waitFocus(String name, long timeoutMs) {
        long iniTime = TimeUtils.millis();
        NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;
        while (cam.focus == null || !cam.focus.name.equalsIgnoreCase(name)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                em.post(Events.JAVA_EXCEPTION, e);
            }
            long spent = TimeUtils.millis() - iniTime;
            if (timeoutMs > 0 && spent > timeoutMs) {
                // Timeout!
                return true;
            }
        }
        return false;
    }

    private Texture getTexture(String path) {
        if (textures == null || !textures.containsKey(path)) {
            preloadTextures(path);
        }
        return textures.get(path);
    }

    @Override
    public void preloadTextures(String... paths) {
        initializeTextures();
        for (final String path : paths) {
            // This only works in async mode!
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    manager.load(path, Texture.class);
                }
            });
            while (!manager.isLoaded(path)) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    em.post(Events.JAVA_EXCEPTION, e);
                }
            }
            Texture tex = manager.get(path, Texture.class);
            textures.put(path, tex);
        }
    }

    @Override
    public void startRecordingCameraPath() {
        em.post(Events.RECORD_CAMERA_CMD, true);
    }

    @Override
    public void stopRecordingCameraPath() {
        em.post(Events.RECORD_CAMERA_CMD, false);
    }

    @Override
    public void runCameraRecording(String path) {
        em.post(Events.PLAY_CAMERA_CMD, path);
    }

    @Override
    public void sleep(float seconds) {
        if (this.isRenderOutputActive()) {
            this.sleepFrames(Math.round(this.getRenderOutputFps() * seconds));
        } else {
            try {
                Thread.sleep(Math.round(seconds * 1000));
            } catch (InterruptedException e) {
                em.post(Events.JAVA_EXCEPTION, e);
            }
        }

    }

    @Override
    public void sleepFrames(int frames) {
        long iniframe = GaiaSky.instance.frames;
        while (GaiaSky.instance.frames - iniframe < frames) {
            // Active wait, fix this
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                em.post(Events.JAVA_EXCEPTION, e);
            }
        }

    }

}
