/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaiasky.util.Constants;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.TextUtils;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IPosition;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

/**
 * A scene graph entity.
 *
 * @author Toni Sagrista
 */
public class SceneGraphNode implements IStarContainer, IPosition {
    public static final String ROOT_NAME = "Universe";

    protected static class TLV3D extends ThreadLocal<Vector3d> {
        @Override
        protected Vector3d initialValue() {
            return new Vector3d();
        }
    }

    protected static class TLV3 extends ThreadLocal<Vector3> {
        @Override
        protected Vector3 initialValue() {
            return new Vector3();
        }
    }

    protected static TLV3D aux3d1 = new TLV3D(), aux3d2 = new TLV3D(), aux3d3 = new TLV3D(), aux3d4 = new TLV3D();
    protected static TLV3 aux3f1 = new TLV3(), aux3f2 = new TLV3(), aux3f3 = new TLV3(), aux3f4 = new TLV3();


    /**
     * Reference to scene graph
     **/
    public static ISceneGraph sg;

    /**
     * Inserts the given node into the default scene graph, if it exists.
     * @param node The node to insert
     * @param addToIndex Whether to add to the index
     * @return True if it was inserted, false otherwise
     */
    public static boolean insert(SceneGraphNode node, boolean addToIndex) {
        if (sg != null) {
            sg.insert(node, addToIndex);
            return true;
        }
        return false;
    }

    /**
     * The internal identifier
     **/
    public long id = -1;

    /**
     * The parent entity.
     */
    public SceneGraphNode parent;

    /**
     * List of children entities.
     */
    public Array<SceneGraphNode> children;

    /**
     * Translation object.
     */
    public Vector3d translation;

    /**
     * Local transform matrix. Contains the transform matrix and the
     * transformations that will be applied to this object and not to its
     * children.
     */
    public Matrix4 localTransform;

    /**
     * This transform stores only the orientation of the object. For example in
     * planets, it stores their orientation with respect to their equatorial
     * plane, but not other transformations applied to the object such as the
     * size or the rotation angle at the time.
     */
    public Matrix4d orientation;

    /**
     * The name(s) of the node, if any.
     */
    public String[] names;

    /**
     * The key to the name in the i18n system.
     */
    protected String namekey = null;

    /**
     * The first name of the parent object.
     */
    public String parentName = null;

    /**
     * The key of the parent
     */
    protected String parentkey = null;

    /**
     * The total number of descendants under this node.
     */
    public int numChildren;

    /**
     * Flag indicating whether the object has been computed in this step.
     */
    public boolean computed = true;

    /**
     * The ownOpacity value (alpha)
     */
    public float opacity = 1f;

    /**
     * Component types, for managing visibility
     */
    public ComponentTypes ct;

    public SceneGraphNode() {
        // Identity
        this.translation = new Vector3d();
    }

    public SceneGraphNode(int id) {
        this();
        this.id = id;
    }

    public SceneGraphNode(ComponentType ct) {
        super();
        this.ct = new ComponentTypes(ct);
    }

    public SceneGraphNode(String[] names, SceneGraphNode parent) {
        this();
        this.names = names;
        this.parent = parent;
    }

    public SceneGraphNode(String name, SceneGraphNode parent) {
        this(new String[]{name}, parent);
    }

    public SceneGraphNode(String name) {
        this(name, null);
    }

    public SceneGraphNode(SceneGraphNode parent) {
        this((String[]) null, parent);
    }

    /**
     * Adds the given SceneGraphNode list as children to this node.
     *
     * @param children
     */
    public final void add(SceneGraphNode... children) {
        if (this.children == null) {
            initChildren(this.parent == null || this instanceof AbstractOctreeWrapper ? 300000 : children.length * 5, this.parent == null ? 1000 : children.length);
        }
        for (int i = 0; i < children.length; i++) {
            SceneGraphNode child = children[i];
            this.children.add(child);
            child.parent = this;
        }
        numChildren += children.length;
    }

    /**
     * Adds a child to the given node and updates the number of children in this
     * node and in all ancestors.
     *
     * @param child               The child node to add.
     * @param updateAncestorCount Whether to update the ancestors number of children.
     */
    public final void addChild(SceneGraphNode child, boolean updateAncestorCount) {
        if (this.children == null) {
            initChildren(this.parent == null ? 200 : 5, this.parent == null ? 100 : 1);
        }
        this.children.add(child);
        child.parent = this;
        numChildren++;

        if (updateAncestorCount) {
            // Update num children in ancestors
            SceneGraphNode ancestor = this.parent;
            while (ancestor != null) {
                ancestor.numChildren++;
                ancestor = ancestor.parent;
            }
        }
    }

    /**
     * Removes the given child from this node, if it exists.
     *
     * @param child
     * @param updateAncestorCount
     */
    public final void removeChild(SceneGraphNode child, boolean updateAncestorCount) {
        if (this.children.contains(child, true)) {
            this.children.removeValue(child, true);
            child.parent = null;
            numChildren--;
            if (updateAncestorCount) {
                // Update num children in ancestors
                SceneGraphNode ancestor = this.parent;
                while (ancestor != null) {
                    ancestor.numChildren--;
                    ancestor = ancestor.parent;
                }
            }
        }
    }

    /**
     * Adds a child to the given node and updates the number of children in this
     * node and in all ancestors.
     *
     * @param child               The child node to add.
     * @param updateAncestorCount Whether to update the ancestors number of children.
     * @param numChildren         The number of children this will hold.
     */
    public final void addChild(SceneGraphNode child, boolean updateAncestorCount, int numChildren) {
        if (this.children == null) {
            initChildren(numChildren, 1);
        }
        this.children.add(child);
        child.parent = this;
        numChildren++;

        if (updateAncestorCount) {
            // Update num children in ancestors
            SceneGraphNode ancestor = this.parent;
            while (ancestor != null) {
                ancestor.numChildren++;
                ancestor = ancestor.parent;
            }
        }
    }

    /**
     * Adds the given list of children as child nodes.
     *
     * @param children
     */
    public void add(List<? extends SceneGraphNode> children) {
        add(children.toArray(new SceneGraphNode[children.size()]));
    }



    /**
     * Inserts the list of nodes under the parents that match each node's name.
     *
     * @param nodes
     */
    public final void insert(List<? extends SceneGraphNode> nodes) {
        Iterator<? extends SceneGraphNode> it = nodes.iterator();

        // Insert top level
        while (it.hasNext()) {
            SceneGraphNode node = it.next();
            if ((this.names == null && node.parentName == null) || (this.names != null && this.names[0].equals(node.parentName))) {
                // Match, add and remove from list
                addChild(node, false);
                node.setUp();
                it.remove();
            }
        }

        // Add to children
        if (children != null) {
            for (SceneGraphNode child : children) {
                child.insert(nodes);
            }
        }

    }

    private void initChildren(int size, int grow) {
        children = new Array<SceneGraphNode>(false, size);
    }

    public SceneGraphNode getChildByNameAndType(String name, Class<? extends SceneGraphNode> clazz) {
        int size = children.size;
        for (int i = 0; i < size; i++) {
            SceneGraphNode child = children.get(i);
            if (child.getName().equalsIgnoreCase(name) && clazz.isInstance(child)) {
                return child;
            }
        }
        return null;
    }

    public SceneGraphNode getChildByName(String name) {
        int size = children.size;
        for (int i = 0; i < size; i++) {
            SceneGraphNode child = children.get(i);
            if (child.getName().equalsIgnoreCase(name)) {
                return child;
            }
        }
        return null;
    }

    public Array<SceneGraphNode> getChildrenByType(Class<? extends SceneGraphNode> clazz, Array<SceneGraphNode> list) {
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                if (clazz.isInstance(child))
                    list.add(child);

                child.getChildrenByType(clazz, list);
            }
        }
        return list;
    }

    public SceneGraphNode getNode(String name) {
        if (this.names != null && this.names[0].equals(name)) {
            return this;
        } else if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                SceneGraphNode n = child.getNode(name);
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    public SceneGraphNode getNode(int id) {
        if (this.id >= 0 && this.id == id) {
            return this;
        } else if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                SceneGraphNode n = child.getNode(id);
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity;
        translation.set(parentTransform);

        // Update with translation/rotation/etc
        updateLocal(time, camera);

        if (children != null) {
            for (int i = 0; i < children.size; i++) {
                children.get(i).update(time, translation, camera, opacity);
            }
        }
    }

    /**
     * Updates the transform matrix with the transformations that will apply to
     * the children and the local transform matrix with the transformations that
     * will apply only to this object.
     *
     * @param time
     */
    protected void updateLocal(ITimeFrameProvider time, ICamera camera) {
    }

    public void initialize() {
        if (ct == null)
            ct = new ComponentTypes(ComponentType.Others.ordinal());
    }

    public void doneLoading(AssetManager manager) {
    }

    public void setNames(String... names) {
        this.names = names;
    }

    public void setName(String name) {
        if (names != null)
            names[0] = name;
        else
            names = new String[]{name};
    }

    /**
     * Adds a name to the list of names
     *
     * @param name The name
     */
    public void addName(String name) {
        if (!hasName(name))
            if (names != null) {
                // Extend array
                String[] newNames = new String[names.length + 1];
                System.arraycopy(names, 0, newNames, 0, names.length);
                newNames[names.length] = name;
                names = newNames;
            } else {
                setName(name);
            }
    }

    public String[] getNames() {
        return names;
    }

    public String getName() {
        return names != null ? names[0] : null;
    }

    public String namesConcat() {
        return TextUtils.concatenate(Constants.nameSeparator, names);
    }

    public boolean hasName(String candidate) {
        return hasName(candidate, false);
    }

    public boolean hasName(String candidate, boolean matchCase) {
        if (names == null) {
            return false;
        } else {
            for (String name : names) {
                if (matchCase) {
                    if (name.equals(candidate))
                        return true;
                } else {
                    if (name.equalsIgnoreCase(candidate))
                        return true;
                }
            }
        }
        return false;
    }

    public void setNamekey(String namekey) {
        this.namekey = namekey;
        updateNames();
    }

    /**
     * Updates the name using the key. This must be called when the language
     * changes.
     */
    public void updateNames() {
        if (namekey != null)
            setName(I18n.bundle.get(namekey));
        if (parentkey != null)
            this.parentName = I18n.bundle.get(parentkey);
    }

    /**
     * Recursively updates the name using the key. This must be called when the
     * language changes.
     */
    public void updateNamesRec() {
        this.updateNames();
        if (children != null && children.size > 0) {
            for (SceneGraphNode node : children)
                node.updateNamesRec();
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setParent(String parentName) {
        this.parentName = parentName;
    }

    public void setParentkey(String parentkey) {
        this.parentkey = parentkey;
        this.updateNames();
    }

    public void dispose() {
        if (children != null) {
            for (SceneGraphNode child : children) {
                child.dispose();
            }
        }
    }

    /**
     * Adds all the children that are focusable objects to the list.
     *
     * @param list
     */
    public void addFocusableObjects(Array<IFocus> list) {
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                child.addFocusableObjects(list);
            }
        }
    }

    public void addNodes(Array<SceneGraphNode> nodes) {
        nodes.add(this);
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                child.addNodes(nodes);
            }
        }
    }

    public void setUp() {
    }

    public void setCt(String ct) {
        this.ct = new ComponentTypes();
        if (!ct.isEmpty())
            this.ct.set(ComponentType.valueOf(ct).ordinal());
    }

    public void setCt(String[] cts) {
        this.ct = new ComponentTypes();
        for (int i = 0; i < cts.length; i++) {
            if (!cts[i].isEmpty()) {
                this.ct.set(ComponentType.valueOf(cts[i]).ordinal());
            }
        }
    }

    public ComponentTypes getCt() {
        return ct;
    }

    public ComponentTypes getComponentType() {
        return ct;
    }

    /**
     * Gets the number of nodes contained in this node, including itself
     *
     * @return The number of children of this node and its descendents
     */
    public int getAggregatedChildren() {
        return numChildren + 1;
    }

    public <T extends SceneGraphNode> T getLineCopy() {
        if (this.parent != null) {
            T parentCopy = parent.getLineCopy();
            T me = getSimpleCopy();
            parentCopy.addChild(me, false, 1);
            return me;
        } else {
            return getSimpleCopy();
        }

    }

    /**
     * Gets a copy of this object but does not copy its parent or children
     *
     * @return The copied object
     */
    public <T extends SceneGraphNode> T getSimpleCopy() {
        T copy = null;
        try {
            copy = (T) this.getClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Logger.getLogger(this.getClass()).error(e);
        }
        copy.names = this.names;
        copy.parentName = this.parentName;
        return copy;
    }

    public SceneGraphNode getRoot() {
        if (this.parent == null) {
            return this;
        } else {
            return this.parent.getRoot();
        }
    }

    @Override
    public String toString() {
        if (names != null)
            return names[0];
        return super.toString();
    }

    public void returnToPool() {
        // if (this.children != null) {
        // for (SceneGraphNode child : children)
        // child.returnToPool();
        // this.children.clear();
        // }
        // Class clazz = this.getClass();
        // MyPools.get(clazz).free(this);
    }

    /**
     * Sets the computed flag of the list of nodes and their children to the
     * given value.
     *
     * @param nodes    List of nodes to set the flag to. May be null.
     * @param computed The computed value.
     */
    public void setComputedFlag(Array<SceneGraphNode> nodes, boolean computed) {
        if (nodes != null) {
            int size = nodes.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode node = nodes.get(i);
                node.computed = computed;
                setComputedFlag(node.children, computed);
            }
        }
    }

    /**
     * Adds the given renderable to the given render group list
     *
     * @param renderable The renderable to add
     * @param rg         The render group that identifies the renderable list
     * @return True if added, false otherwise
     */
    protected boolean addToRender(IRenderable renderable, RenderGroup rg) {
        boolean on = ct.isEmpty() || ct.intersects(SceneGraphRenderer.visible);
        if (on || SceneGraphRenderer.alphas[ct.getFirstOrdinal()] > 0) {
            SceneGraphRenderer.render_lists.get(rg.ordinal()).add(renderable);
            return true;
        }
        return false;
    }

    /**
     * Removes the given renderable from the given render group list.
     *
     * @param renderable The renderable to remove
     * @param rg         The render group to remove from
     * @return True if removed, false otherwise
     */
    protected boolean removeFromRender(IRenderable renderable, RenderGroup rg) {
        return SceneGraphRenderer.render_lists.get(rg.ordinal()).remove(renderable);
    }

    protected boolean isInRender(IRenderable renderable, RenderGroup rg) {
        return SceneGraphRenderer.render_lists.get(rg.ordinal()).contains(renderable);
    }

    protected boolean isInRender(IRenderable renderable, RenderGroup... rgs) {
        boolean is = false;
        for (RenderGroup rg : rgs)
            is = is || SceneGraphRenderer.render_lists.get(rg.ordinal()).contains(renderable);
        return is;
    }

    /**
     * Gets the first ancestor of this node that is of type {@link Star}
     *
     * @return The first ancestor of type {@link Star}
     */
    public SceneGraphNode getFirstStarAncestor() {
        if (this instanceof Star) {
            return this;
        } else if (parent != null) {
            return parent.getFirstStarAncestor();
        } else {
            return null;
        }
    }

    @Override
    public int getStarCount() {
        return 0;
    }

    @Override
    public Vector3d getPosition() {
        return null;
    }

    @Override
    public Vector3d getVelocity() {
        return null;
    }

    public Matrix4d getOrientation() {
        return orientation;
    }

    public boolean isVisibilityOn() {
        return GaiaSky.instance.isOn(ct);
    }

    public float getOpacity() {
        return opacity;
    }

    public int getSceneGraphDepth() {
        if (this.parent == null) {
            return 0;
        } else {
            return this.parent.getSceneGraphDepth() + 1;
        }
    }

    /**
     * Special actions to be taken for this object when adding to the index.
     *
     * @param map The index
     */
    protected void addToIndex(ObjectMap<String, SceneGraphNode> map) {
    }

    /**
     * Special actions to be taken for this object when removing from the index. Must implement if addToIndex is implemented
     *
     * @param map The index
     */
    protected void removeFromIndex(ObjectMap<String, SceneGraphNode> map) {
    }

    /**
     * Whether to add this node to the index
     *
     * @return True if the node needs to be added to the index.
     */
    public boolean mustAddToIndex() {
        return true;
    }

    /**
     * Returns whether the current position is valid (usually, when there is no
     * coordinates overflow)
     *
     * @return
     */
    public boolean isValidPosition() {
        return true;
    }
}
