/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.LoadingTextGenerator;
import gaiasky.util.TipGenerator;
import gaiasky.util.math.StdRandom;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextIconButton;

import java.math.BigInteger;

/**
 * Displays the loading screen.
 *
 * @author Toni Sagrista
 */
public class LoadingGui extends AbstractGui {
    protected Table center, topLeft, bottomMiddle, screenMode;

    public NotificationsInterface notificationsInterface;
    private TipGenerator tipGenerator;
    private OwnLabel spin;
    private HorizontalGroup tip;
    private BigInteger m1, m2;
    private long i;
    private long lastFunnyTime;
    private long lastTipTime;

    public LoadingGui(Lwjgl3Graphics graphics, Float unitsPerPixel) {
        this(graphics, unitsPerPixel, 0, false);
    }

    public LoadingGui(Lwjgl3Graphics graphics, Float unitsPerPixel, Boolean vr) {
        this(graphics, unitsPerPixel, 0, vr);
    }

    public LoadingGui(Lwjgl3Graphics graphics, Float unitsPerPixel, Integer hoffset, Boolean vr) {
        super(graphics, unitsPerPixel);
        this.vr = vr;
        this.hoffset = hoffset;
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        interfaces = new Array<>();
        float pad30 = 48f;
        float pad10 = 16f;
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);
        if (vr) {
            vp.update(GlobalConf.screen.BACKBUFFER_WIDTH, GlobalConf.screen.BACKBUFFER_HEIGHT, true);
        } else {
            vp.update(GaiaSky.graphics.getWidth(), GaiaSky.graphics.getHeight(), true);
        }

        skin = GlobalResources.skin;


        center = new Table(skin);
        if(!vr) {
            Texture tex = new Texture(Gdx.files.internal("img/splash/splash.jpg"));
            Drawable bg = new SpriteDrawable(new Sprite(tex));
            center.setBackground(bg);
        }
        center.setFillParent(true);
        center.center();
        if (hoffset > 0)
            center.padLeft(hoffset);
        else if (hoffset < 0)
            center.padRight(-hoffset);

        OwnLabel gaiasky = new OwnLabel(GlobalConf.getApplicationTitle(GlobalConf.runtime.OPENVR), skin, "main-title");

        // Funny text
        lastFunnyTime = 0;
        i = -1;
        m1 = BigInteger.ZERO;
        m2 = BigInteger.ZERO;
        spin = new OwnLabel("0", skin, "main-title-xs");
        spin.setColor(skin.getColor("theme"));

        center.add(gaiasky).center().padBottom(pad10 * 2f).row();
        center.add(spin).padBottom(pad30).row();

        // Tips
        tipGenerator = new TipGenerator(skin);
        tip = new HorizontalGroup();
        tip.space(pad10);
        bottomMiddle = new Table(skin);
        bottomMiddle.setFillParent(true);
        bottomMiddle.center().bottom();
        bottomMiddle.padLeft(pad30).padBottom(pad10);
        bottomMiddle.add(tip);

        // Version and build
        topLeft = new VersionLineTable(skin);

        // SCREEN MODE BUTTON - TOP RIGHT
        screenMode = new Table(skin);
        screenMode.setFillParent(true);
        screenMode.top().right();
        screenMode.pad(pad10);
        OwnTextIconButton screenModeButton = new OwnTextIconButton("", skin, "screen-mode");
        screenModeButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                GlobalConf.screen.FULLSCREEN = !GlobalConf.screen.FULLSCREEN;
                EventManager.instance.post(Events.SCREEN_MODE_CMD);
                return true;
            }
            return false;
        });
        screenMode.add(screenModeButton);

        // MESSAGE INTERFACE - BOTTOM
        notificationsInterface = new NotificationsInterface(skin, lock, false, false, false);
        center.add(notificationsInterface);

        interfaces.add(notificationsInterface);

        rebuildGui();

    }

    private final long tipTime = 4000;
    private long funnyTextTime = 1400;

    @Override
    public void update(double dt) {
        super.update(dt);
        // Fibonacci numbers
        long currTime = System.currentTimeMillis();
        if (currTime - lastFunnyTime > funnyTextTime) {
            randomFunnyText();
            lastFunnyTime = currTime;
            funnyTextTime = StdRandom.uniform(1500, 3000);
        }
        if (currTime - lastTipTime > tipTime) {
            tipGenerator.newTip(tip);
            lastTipTime = currTime;
        }
    }

    /**
     * Return the i fibonacci number
     **/
    private void fibonacci() {
        i++;
        BigInteger next;
        if (i == 0l) {
            next = BigInteger.ZERO;
        } else if (i == 1l) {
            next = BigInteger.ONE;
        } else {
            next = m1.add(m2);
        }
        m2 = m1;
        m1 = next;

        spin.setText(next.toString());
    }

    private void randomFunnyText() {
        if (GlobalConf.runtime.OPENVR) {
            spin.setText("Loading...");
        } else {
            try {
                spin.setText(LoadingTextGenerator.next());
            } catch (Exception e) {
                spin.setText("Loading...");
            }
        }
    }

    private void reset() {
        i = 0l;
        m1 = BigInteger.ZERO;
        m2 = BigInteger.ZERO;
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    public void rebuildGui() {
        if (ui != null) {
            ui.clear();
            ui.addActor(center);
            ui.addActor(screenMode);
            if(!vr) {
                ui.addActor(bottomMiddle);
                ui.addActor(topLeft);
            }
        }
    }

}
