/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.util.GlobalConf;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.math.MathUtilsd;

/**
 * Same as a regular slider, but contains the title (name) and the value within its bounds.
 */
public class OwnSliderPlus extends Slider {
    private static INumberFormat nf = NumberFormatFactory.getFormatter("####0.#");

    private float ownwidth = 0f, ownheight = 0f;
    private float mapMin, mapMax, min, max;
    private boolean map = false;
    private Skin skin;
    private OwnLabel title, value;
    private boolean displayValueMapped = false;
    private String valuePrefix, valueSuffix;
    private float padX = 3f * GlobalConf.UI_SCALE_FACTOR;
    private float padY = 3f * GlobalConf.UI_SCALE_FACTOR;

    public OwnSliderPlus(String title, float min, float max, float stepSize, float mapMin, float mapMax, Skin skin) {
        super(min, max, stepSize, false, skin, "big-horizontal");
        this.skin = skin;
        setUp(title, min, max, mapMin, mapMax);
    }

    public OwnSliderPlus(String title, float min, float max, float stepSize, Skin skin) {
        super(min, max, stepSize, false, skin, "big-horizontal");
        this.skin = skin;
        setUp(title, min, max, min, max);
    }

    public OwnSliderPlus(String title, float min, float max, float stepSize, boolean vertical, Skin skin) {
        super(min, max, stepSize, vertical, skin, "big-horizontal");
        this.skin = skin;
        setUp(title, min, max, min, max);
    }

    public OwnSliderPlus(String title, float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
        super(min, max, stepSize, vertical, skin, styleName);
        this.skin = skin;
        setUp(title, min, max, min, max);
    }

    public void setUp(String title, float min, float max, float mapMin, float mapMax) {
        setMapValues(min, max, mapMin, mapMax);

        if (title != null && !title.isEmpty()) {
            this.title = new OwnLabel(title, skin);
        } else {
            this.title = null;
        }

        this.value = new OwnLabel(getValueString(), skin);
        this.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                this.value.setText(getValueString());
                return true;
            }
            return false;
        });
    }

    public void setDisplayValueMapped(boolean displayValueMapped) {
        this.displayValueMapped = displayValueMapped;
    }

    public void setMapValues(float min, float max, float mapMin, float mapMax) {
        this.min = min;
        this.max = max;
        this.mapMin = mapMin;
        this.mapMax = mapMax;
        this.map = mapMin != min && mapMax != max;
    }

    public void removeMapValues() {
        this.mapMin = 0;
        this.mapMax = 0;
        this.map = false;
    }

    public String getValueString() {
        return (valuePrefix != null ? valuePrefix : "") + nf.format((displayValueMapped ? getMappedValue() : getValue())) + (valueSuffix != null ? valueSuffix : "");
    }

    public float getMappedValue() {
        if (map) {
            return MathUtilsd.lint(getValue(), getMinValue(), getMaxValue(), mapMin, mapMax);
        } else {
            return getValue();
        }
    }

    public void setMappedValue(double mappedValue) {
        setMappedValue((float) mappedValue);
    }

    public void setMappedValue(float mappedValue) {
        if (map) {
            setValue(MathUtilsd.lint(mappedValue, mapMin, mapMax, getMinValue(), getMaxValue()));
        } else {
            setValue(mappedValue);
        }
    }

    public void setValuePrefix(String valuePrefix) {
        this.valuePrefix = valuePrefix;
    }

    public void setValueSuffix(String valueSuffix) {
        this.valueSuffix = valueSuffix;
    }

    @Override
    public void setWidth(float width) {
        ownwidth = width;
        super.setWidth(width);
    }

    @Override
    public void setHeight(float height) {
        ownheight = height;
        super.setHeight(height);
    }

    @Override
    public void setSize(float width, float height) {
        ownwidth = width;
        ownheight = height;
        super.setSize(width, height);
    }

    @Override
    public float getPrefWidth() {
        if (ownwidth != 0) {
            return ownwidth;
        } else {
            return super.getPrefWidth();
        }
    }

    @Override
    public float getPrefHeight() {
        if (ownheight != 0) {
            return ownheight;
        } else {
            return super.getPrefHeight();
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (title != null) {
            title.setPosition(getX() + padX, getY() + padY);
            title.draw(batch, parentAlpha);
        }
        if (value != null) {
            value.setPosition(getX() + getPrefWidth() - (value.getPrefWidth() + padX * 2f), getY() + padY);
            value.draw(batch, parentAlpha);
        }
    }

}
