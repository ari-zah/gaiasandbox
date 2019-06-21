/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.assets.OrbitDataLoader.OrbitDataLoaderParameter;
import gaia.cu9.ari.gaiaorbit.data.orbit.OrbitSamplerDataProvider;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.Orbit;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class OrbitRefresher {
    private static final Log logger = Logger.getLogger(OrbitRefresher.class);

    /**
     * Maximum size of load queue
     */
    private static final int LOAD_QUEUE_MAX_SIZE = 15;
    /**
     * Maximum number of pages to send to load every batch
     **/
    protected static final int MAX_LOAD_CHUNK = 5;

    /**
     * The instance
     */
    private static OrbitRefresher instance;

    /**
     * The loading queue
     */
    private Queue<OrbitDataLoaderParameter> toLoadQueue;

    /**
     * The daemon
     */
    private DaemonRefresher daemon;

    /**
     * Loading is paused
     */
    private boolean loadingPaused = false;

    public OrbitRefresher() {
        super();
        toLoadQueue = new ArrayBlockingQueue<>(LOAD_QUEUE_MAX_SIZE);
        OrbitRefresher.instance = this;

        // Start daemon
        daemon = new DaemonRefresher();
        daemon.setDaemon(true);
        daemon.setName("daemon-orbit-refresher");
        daemon.setPriority(Thread.MIN_PRIORITY);
        daemon.start();
    }

    public void queue(OrbitDataLoaderParameter param){
        if (!loadingPaused && toLoadQueue.size() < LOAD_QUEUE_MAX_SIZE - 1) {
            toLoadQueue.remove(param);
            toLoadQueue.add(param);
            param.orbit.refreshing = true;
            flushLoadQueue();
        }

    }

    /**
     * Tells the loader to start loading the octants in the queue.
     */
    public void flushLoadQueue() {
        if (!daemon.awake && !toLoadQueue.isEmpty() && !loadingPaused) {
            EventManager.instance.post(Events.BACKGROUND_LOADING_INFO);
            daemon.interrupt();
        }
    }
    /**
     * The daemon refresher thread.
     *
     * @author Toni Sagrista
     */
    protected static class DaemonRefresher extends Thread {
        private boolean awake;
        private boolean running;
        private AtomicBoolean abort;
        private OrbitSamplerDataProvider provider;

        private Array<OrbitDataLoaderParameter> toLoad;

        public DaemonRefresher() {
            this.awake = false;
            this.running = true;
            this.abort = new AtomicBoolean(false);
            this.toLoad = new Array<>();
            this.provider = new OrbitSamplerDataProvider();
        }

        /**
         * Stops the daemon iterations when
         */
        public void stopDaemon() {
            running = false;
        }

        /**
         * Aborts only the current iteration
         */
        public void abort() {
            abort.set(true);
        }

        @Override
        public void run() {
            while (running) {
                /** ----------- PROCESS REQUESTS ----------- **/
                while (!instance.toLoadQueue.isEmpty()) {
                    toLoad.clear();
                    int i = 0;
                    while (instance.toLoadQueue.peek() != null && i <= MAX_LOAD_CHUNK) {
                        OrbitDataLoaderParameter param = instance.toLoadQueue.poll();
                        toLoad.add(param);
                        i++;
                    }

                    // Generate orbits if any
                    if (toLoad.size > 0) {
                        try {
                           for(OrbitDataLoaderParameter param : toLoad){
                               Orbit orbit = param.orbit;
                               if(orbit != null){
                                   // Generate data
                                   provider.load(null, param);
                                   final PointCloudData pcd = provider.getData();
                                   // Post new data to object
                                   Gdx.app.postRunnable(()->{
                                       // Update orbit object
                                       orbit.setPointCloudData(pcd);
                                       orbit.initOrbitMetadata();

                                       orbit.refreshing = false;
                                   });

                               } else {
                                   // Error, need orbit
                               }
                           }
                        } catch (Exception e) {
                            // This will happen when the queue has been cleared during processing
                            logger.debug("Refreshing orbits operation failed");
                        }
                    }
                }

                /** ----------- SLEEP UNTIL INTERRUPTED ----------- **/
                try {
                    awake = false;
                    abort.set(false);
                    Thread.sleep(Long.MAX_VALUE - 8);
                } catch (InterruptedException e) {
                    // New data!
                    awake = true;
                }
            }
        }
    }

}
