/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import gaiasky.scenegraph.ParticleGroup;

import java.io.InputStream;
import java.util.Set;

/**
 * Data provider for a star group, which contains an index map with the names
 * and indices of the stars.
 *
 * @author tsagrista
 */
public interface IStarGroupDataProvider extends IParticleGroupDataProvider {
    LongMap<float[]> getColors();


    /**
     * Loads the data applying a factor using a memory mapped file for improved speed.
     *
     * @param file   The file to load
     * @param factor Factor to apply to the positions
     * @param compatibility Use compatibility mode (DR1/DR2)
     * @return Array of particle beans
     */
    Array<ParticleGroup.ParticleBean> loadDataMapped(String file, double factor, boolean compatibility);

    /**
     * Loads the data applying a factor.
     *
     * @param file   The file to load
     * @param factor Factor to apply to the positions
     * @param compatibility Use compatibility mode (DR1/DR2)
     * @return Array of particle beans
     */
    Array<ParticleGroup.ParticleBean> loadData(String file, double factor, boolean compatibility);

    /**
     * Loads the data applying a factor.
     *
     * @param is     Input stream to load the data from
     * @param factor Factor to apply to the positions
     * @param compatibility Use compatibility mode (DR1/DR2)
     * @return Array of particle beans
     */
    Array<ParticleGroup.ParticleBean> loadData(InputStream is, double factor, boolean compatibility);

    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. This applies to
     * faint stars (gmag >= 13.1)
     * More specifically, the following must be met:
     * </p>
     * <code>pllx_err &lt; pllx * pllxErrFactor</code>
     *
     * @param parallaxErrorFactor The percentage value of parallax errors with respect to parallax
     */
    void setParallaxErrorFactorFaint(double parallaxErrorFactor);

    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. This applies to
     * bright stars (gmag < 13.1)
     * More specifically, the following must be met:
     * </p>
     * <code>pllx_err &lt; pllx * pllxErrFactor</code>
     *
     * @param parallaxErrorFactor The percentage value of parallax errors with respect to parallax
     */
    void setParallaxErrorFactorBright(double parallaxErrorFactor);

    /**
     * Whether to use an adaptive threshold, relaxing it for bright (appmag >= 13) stars to let more
     * bright stars in.
     */
    void setAdaptiveParallax(boolean adaptive);

    /**
     * Sets the zero point of the parallax as an addition to the parallax
     * values, in [mas]
     *
     * @param parallaxZeroPoint The parallax zero point
     */
    void setParallaxZeroPoint(double parallaxZeroPoint);

    /**
     * Sets the flag to apply magnitude and color corrections for extinction and
     * reddening
     *
     * @param magCorrections Whether to apply the corrections
     */
    void setMagCorrections(boolean magCorrections);

    /**
     * Set location of additional columns file or directory
     * @param additionalFile File or directory with additional columns per sourceId
     */
    void setAdditionalFiles(String additionalFile);

    /**
     * Sets the RUWE criteria. RUWE file must have been set
     *
     * @param RUWE The criteria (usually 1.4)
     */
    void setRUWECap(double RUWE);

    /**
     * Sets a distance cap. Stars beyond this distance will not be loaded
     *
     * @param distCap The distance cap, in parsecs
     */
    void setDistanceCap(double distCap);

    /**
     * Gets the star counts per magnitude
     **/
    long[] getCountsPerMag();


    /**
     * Adds a set with all the ids which will be loaded regardless of any other
     * conditions (i.e. parallax error thresholds)
     * @param ids The ids that must be loaded
     */
    void setMustLoadIds(Set<Long>ids);

    /**
     * List of column names, separated by commas, indicating the position of each
     * field to load
     * @param columns The column name list
     */
    void setColumns(String columns);
}