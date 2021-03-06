/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.SysUtils;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.scene2d.*;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Displays info about the current crash.
 *
 * @author Toni Sagrista
 */
public class CrashGui extends AbstractGui {
    protected Throwable crash;
    protected CrashWindow crashWindow;

    public CrashGui(Lwjgl3Graphics graphics, Float unitsPerPixel, Throwable crash) {
        this(graphics, unitsPerPixel, crash, 0, false);
    }

    public CrashGui(Lwjgl3Graphics graphics, Float unitsPerPixel, Throwable crash, Integer hoffset, Boolean vr) {
        super(graphics, unitsPerPixel);
        this.crash = crash;
        this.vr = vr;
        this.hoffset = hoffset;
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
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

        // Dialog
        crashWindow = new CrashWindow(ui, skin, crash);

        rebuildGui();

    }

    @Override
    public void update(double dt) {
        super.update(dt);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    public void rebuildGui() {
        if (ui != null) {
            ui.clear();
            crashWindow.show(ui);
        }
    }


    private class CrashWindow extends GenericDialog {

        public CrashWindow(Stage ui, Skin skin, Throwable crash) {
            super(I18n.txt("gui.crash.title"), skin, ui);

            setAcceptText(I18n.txt("gui.exit"));

            buildSuper();
        }

        @Override
        protected void build() {
            content.clear();


            // Crash image
            Image img = new Image(new Texture(Gdx.files.internal("img/crash.png")));
            content.add(img).center().padBottom(pad10 * 2f).row();

            // Delete data folder and try again
            content.add(new OwnLabel(I18n.txt("gui.crash.info.1"), skin)).left().padBottom(pad5).row();
            OwnLabel dloc = new OwnLabel(TextUtils.capString(GlobalConf.data.DATA_LOCATION, 50), skin, "hud-subheader");
            dloc.addListener(new OwnTextTooltip(GlobalConf.data.DATA_LOCATION, skin));
            content.add(dloc).left().padBottom(pad10 * 3f).row();

            // Crash log
            content.add(new OwnLabel(I18n.txt("gui.crash.info.2"), skin)).left().padBottom(pad5).row();
            OwnLabel cloc = new OwnLabel(TextUtils.capString(SysUtils.getCrashReportsDir().toString(), 50), skin, "hud-subheader");
            cloc.addListener(new OwnTextTooltip(SysUtils.getCrashReportsDir().toString(), skin));
            content.add(cloc).left().padBottom(pad5).row();
            content.add(new OwnLabel(I18n.txt("gui.crash.info.3"), skin)).left().padBottom(pad5).row();
            content.add(new Link(GlobalConf.REPO_ISSUES, skin.get("link", Label.LabelStyle.class), GlobalConf.REPO_ISSUES)).left().padBottom(pad10 * 3f).row();

            // Stack trace
            float taw = 720f;
            float tah = 240f;
            content.add(new OwnLabel(I18n.txt("gui.crash.stack"), skin, "ui-19")).left().padBottom(pad5).row();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            crash.printStackTrace(pw);
            String sts = sw.toString();
            int lines = 1;
            for(char c : sts.toCharArray()){
                if(c == '\n')
                    lines++;
            }

            TextArea stackTrace = new OwnTextArea(sts, skin.get("regular", TextField.TextFieldStyle.class));
            stackTrace.setDisabled(true);
            stackTrace.setPrefRows(lines);
            stackTrace.setWidth(1120f);
            OwnScrollPane stScroll = new OwnScrollPane(stackTrace, skin, "default-nobg");
            stScroll.setWidth(taw);
            stScroll.setHeight(tah);
            stScroll.setForceScroll(false, true);
            stScroll.setSmoothScrolling(true);
            stScroll.setFadeScrollBars(false);
            content.add(stScroll).center();


        }

        @Override
        protected void accept() {
            GaiaSky.postRunnable(() -> Gdx.app.exit());
        }

        @Override
        protected void cancel() {
            GaiaSky.postRunnable(() -> Gdx.app.exit());
        }

    }

}
