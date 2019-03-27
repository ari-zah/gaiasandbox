package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.CatalogInfo;
import gaia.cu9.ari.gaiaorbit.util.CatalogManager;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DatasetsComponent extends GuiComponent implements IObserver {
    private VerticalGroup group;
    private float pad = 3 * GlobalConf.SCALE_FACTOR;

    private Map<String, HorizontalGroup> groupMap;
    private Map<String, OwnImageButton> imageMap;

    public DatasetsComponent(Skin skin, Stage stage) {
        super(skin, stage);
        groupMap = new HashMap<>();
        imageMap = new HashMap<>();
        EventManager.instance.subscribe(this, Events.CATALOG_ADD, Events.CATALOG_REMOVE, Events.CATALOG_VISIBLE, Events.CATALOG_HIGHLIGHT);
    }

    @Override
    public void initialize() {

        group = new VerticalGroup();
        group.space(pad);
        group.align(Align.left);

        Collection<CatalogInfo> cis = CatalogManager.instance().getCatalogInfos();
        if (cis != null) {
            Iterator<CatalogInfo> it = cis.iterator();
            while (it.hasNext()) {
                CatalogInfo ci = it.next();
                addCatalogInfo(ci);
            }
        }

        component = group;
    }

    private void addCatalogInfo(CatalogInfo ci) {

        Table t = new Table();
        t.add(new OwnLabel(ci.name, skin, "hud-subheader")).left().row();
        t.add(new OwnLabel(I18n.txt("gui.dataset.type") + ": " + ci.type.toString(), skin)).left().row();
        t.add(new OwnLabel(ci.description, skin)).left().padBottom(pad).row();

        HorizontalGroup ciGroup = new HorizontalGroup();
        ciGroup.space(pad);
        ciGroup.align(Align.left);

        // Info
        ScrollPane scroll = new OwnScrollPane(t, skin, "minimalist-nobg");
        scroll.setScrollingDisabled(false, true);
        scroll.setForceScroll(false, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setSmoothScrolling(true);
        scroll.setWidth(155 * GlobalConf.SCALE_FACTOR);
        scroll.setHeight(GlobalConf.SCALE_FACTOR > 1 ? 90 : 50);

        // Controls
        VerticalGroup controls = new VerticalGroup();
        controls.space(pad);
        OwnImageButton eye = new OwnImageButton(skin, "eye-toggle");
        eye.addListener(new TextTooltip(I18n.txt("gui.tooltip.dataset.toggle"), skin));
        eye.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Toggle visibility
                if (ci.object != null) {
                    boolean newvis = !ci.object.isVisible();
                    EventManager.instance.post(Events.CATALOG_VISIBLE, ci.name, newvis, true);
                }
                return true;
            }
            return false;
        });
        imageMap.put(ci.name, eye);

        ImageButton rubbish = new OwnImageButton(skin, "rubbish-bin");
        rubbish.addListener(new TextTooltip(I18n.txt("gui.tooltip.dataset.remove"), skin));
        rubbish.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Remove dataset
                EventManager.instance.post(Events.CATALOG_REMOVE, ci.name);
                return true;
            }
            return false;
        });

        ImageButton mark = new OwnImageButton(skin, "highlight-ds");
        mark.addListener(new TextTooltip(I18n.txt("gui.tooltip.dataset.highlight"), skin));
        mark.addListener((event) -> {
           if(event instanceof ChangeEvent){
               EventManager.instance.post(Events.CATALOG_HIGHLIGHT, ci.name);
               return true;
           }
           return false;
        });

        controls.addActor(eye);
        controls.addActor(rubbish);
        controls.addActor(mark);

        ciGroup.addActor(controls);
        ciGroup.addActor(scroll);

        group.addActor(ciGroup);

        groupMap.put(ci.name, ciGroup);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case CATALOG_ADD:
            addCatalogInfo((CatalogInfo) data[0]);
            break;
        case CATALOG_REMOVE:
            String ciName = (String) data[0];
            if(groupMap.containsKey(ciName)){
                groupMap.get(ciName).remove();
                groupMap.remove(ciName);
                imageMap.remove(ciName);
            }
            break;
        case CATALOG_VISIBLE:
            boolean ui = false;
            if (data.length > 2)
                ui = (Boolean) data[2];
            if (!ui) {
                ciName = (String) data[0];
                boolean visible = (Boolean) data[1];
                OwnImageButton eye = imageMap.get(ciName);
                eye.setCheckedNoFire(!visible);
            }
            break;
        case CATALOG_HIGHLIGHT:
            break;
        default:
            break;
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

}
