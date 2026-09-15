package pixel;

import com.jme3.app.Application;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 * The first playable slice of Unravel's prologue.
 *
 * <p>It provides an opening message, a persistent goals panel (A), and an
 * alien radar with the blue Musafir signal (M). It deliberately contains no
 * camera, map, or NPC logic so those systems can remain separate.</p>
 */
public final class PrologueAppState extends BaseAppState implements ActionListener {

    private static final String INTRO_CONTINUE = "prologue-continue";
    private static final String OBJECTIVES_TOGGLE = "prologue-objectives";
    private static final String RADAR_TOGGLE = "prologue-radar";

    private static final ColorRGBA PANEL_COLOR =
            new ColorRGBA(0.02f, 0.04f, 0.10f, 0.88f);
    private static final ColorRGBA PANEL_EDGE_COLOR =
            new ColorRGBA(0.20f, 0.65f, 0.92f, 0.96f);
    private static final ColorRGBA TEXT_COLOR =
            new ColorRGBA(0.86f, 0.94f, 1.00f, 1f);
    private static final ColorRGBA MUTED_TEXT_COLOR =
            new ColorRGBA(0.61f, 0.75f, 0.87f, 1f);
    private static final ColorRGBA SIGNAL_COLOR =
            new ColorRGBA(0.12f, 0.82f, 1.00f, 1f);

    private SimpleApplication simpleApplication;
    private Node guiNode;
    private BitmapFont font;

    private Node prologueGui;
    private Node introPanel;
    private Node objectivesPanel;
    private Node radarPanel;

    private boolean introVisible = true;
    private boolean objectivesVisible;
    private boolean radarVisible;
    //223
    private final Spatial campus;
    private final Spatial player;

    private Camera radarCamera;
    private ViewPort radarView;
    private FrameBuffer radarBuffer;
    private Texture2D radarTexture;
    private Geometry playerMarker;

    private float radarMapX;
    private float radarMapY;
    private float radarMapWidth;
    private float radarMapHeight;

    public PrologueAppState(Spatial campus, Spatial player) {
        this.campus = campus;
        this.player = player;
    }
    @Override
    protected void initialize(Application application) {
        if (!(application instanceof SimpleApplication)) {
            throw new IllegalStateException(
                    "PrologueAppState requires a SimpleApplication."
            );
        }

        simpleApplication = (SimpleApplication) application;
        guiNode = simpleApplication.getGuiNode();
        font = application.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        float screenWidth = simpleApplication.getCamera().getWidth();
        float screenHeight = simpleApplication.getCamera().getHeight();

        prologueGui = new Node("PrologueGui");
        introPanel = createIntroPanel(screenWidth, screenHeight);
        objectivesPanel = createObjectivesPanel(screenWidth, screenHeight);
        radarPanel = createRadarPanel(screenWidth, screenHeight);

        prologueGui.attachChild(introPanel);
        prologueGui.attachChild(objectivesPanel);
        prologueGui.attachChild(radarPanel);

        setPanelVisible(introPanel, true);
        setPanelVisible(objectivesPanel, false);
        setPanelVisible(radarPanel, false);

        application.getInputManager().addMapping(
                INTRO_CONTINUE,
                new KeyTrigger(KeyInput.KEY_RETURN),
                new KeyTrigger(KeyInput.KEY_SPACE)
        );
        application.getInputManager().addMapping(
                OBJECTIVES_TOGGLE,
                new KeyTrigger(KeyInput.KEY_A)
        );
        application.getInputManager().addMapping(
                RADAR_TOGGLE,
                new KeyTrigger(KeyInput.KEY_M)
        );
        application.getInputManager().addListener(
                this,
                INTRO_CONTINUE,
                OBJECTIVES_TOGGLE,
                RADAR_TOGGLE
        );
    }

    @Override
    protected void cleanup(Application application) {
        application.getInputManager().removeListener(this);
        application.getInputManager().deleteMapping(INTRO_CONTINUE);
        application.getInputManager().deleteMapping(OBJECTIVES_TOGGLE);
        application.getInputManager().deleteMapping(RADAR_TOGGLE);

        if (prologueGui != null) {
            prologueGui.removeFromParent();
        }

        if (radarView != null) {
            radarView.setEnabled(false);
            radarView.clearScenes();
            simpleApplication.getRenderManager().removePreView(radarView);
            radarView = null;
        }

        if (radarBuffer != null) {
            radarBuffer.dispose();
            radarBuffer = null;
        }

        if (radarTexture != null) {
            radarTexture.getImage().dispose();
            radarTexture = null;
        }
    }
    @Override
    protected void onEnable() {
        if (prologueGui.getParent() == null) {
            guiNode.attachChild(prologueGui);
        }
    }

    @Override
    protected void onDisable() {
        prologueGui.removeFromParent();

        if (radarView != null) {
            radarView.setEnabled(false);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            return;
        }

        if (INTRO_CONTINUE.equals(name) && introVisible) {
            introVisible = false;
            setPanelVisible(introPanel, false);
            objectivesVisible = true;
            setPanelVisible(objectivesPanel, true);
            return;
        }

        if (OBJECTIVES_TOGGLE.equals(name) && !introVisible) {
            objectivesVisible = !objectivesVisible;
            setPanelVisible(objectivesPanel, objectivesVisible);
            return;
        }

        if (RADAR_TOGGLE.equals(name) && !introVisible) {
            radarVisible = !radarVisible;
            setPanelVisible(radarPanel, radarVisible);
        }
    }
    @Override
    public void update(float tpf) {
        if (radarView == null) {
            return;
        }

        radarView.setEnabled(radarVisible);

        if (!radarVisible) {
            return;
        }

        Vector3f mapPosition = radarCamera.getScreenCoordinates(
                player.getWorldTranslation()
        );

        float u = mapPosition.x / radarCamera.getWidth();
        float v = mapPosition.y / radarCamera.getHeight();

        boolean insideMap =
                u >= 0f && u <= 1f &&
                v >= 0f && v <= 1f;

        playerMarker.setCullHint(
                insideMap ? Spatial.CullHint.Never : Spatial.CullHint.Always
        );

        if (insideMap) {
            float markerX = radarMapX + u * radarMapWidth;
            float markerY = radarMapY + v * radarMapHeight;

            markerX = Math.max(
                    radarMapX + 5f,
                    Math.min(radarMapX + radarMapWidth - 5f, markerX)
            );

            markerY = Math.max(
                    radarMapY + 5f,
                    Math.min(radarMapY + radarMapHeight - 5f, markerY)
            );

            playerMarker.setLocalTranslation(
                    markerX - 5f, markerY - 5f, 3f
            );
        }
    }
    private Node createIntroPanel(float screenWidth, float screenHeight) {
        Node panel = new Node("PrologueIntroPanel");
        panel.attachChild(createRectangle(
                0f,
                0f,
                screenWidth,
                screenHeight,
                PANEL_COLOR
        ));

        float x = 110f;
        float y = screenHeight - 120f;

        panel.attachChild(createText(
                "UNRAVEL",
                42f,
                PANEL_EDGE_COLOR,
                x,
                y
        ));
        panel.attachChild(createText(
                "A record made by humanity crossed the dark between stars.",
                22f,
                TEXT_COLOR,
                x,
                y - 82f
        ));
        panel.attachChild(createText(
                "Its map of Earth led an alien observer to IUT.",
                22f,
                TEXT_COLOR,
                x,
                y - 118f
        ));
        panel.attachChild(createText(
                "A strange signal survives somewhere on campus.",
                22f,
                SIGNAL_COLOR,
                x,
                y - 154f
        ));
        panel.attachChild(createText(
                "Press ENTER or SPACE to begin",
                20f,
                MUTED_TEXT_COLOR,
                x,
                92f
        ));
        panel.attachChild(createText(
                "A: objectives    M: alien radar",
                17f,
                MUTED_TEXT_COLOR,
                x,
                58f
        ));

        return panel;
    }

    private Node createObjectivesPanel(float screenWidth, float screenHeight) {
        Node panel = new Node("PrologueObjectivesPanel");
        float x = 32f;
        float y = screenHeight - 248f;

        panel.attachChild(createRectangle(
                x - 18f,
                y - 22f,
                500f,
                210f,
                PANEL_COLOR
        ));
        panel.attachChild(createRectangle(
                x - 18f,
                y + 180f,
                500f,
                5f,
                PANEL_EDGE_COLOR
        ));
        panel.attachChild(createText(
                "ALIEN OBJECTIVES",
                22f,
                PANEL_EDGE_COLOR,
                x,
                y + 142f
        ));
        panel.attachChild(createText(
                "1. Find out where humanity went.",
                18f,
                TEXT_COLOR,
                x,
                y + 96f
        ));
        panel.attachChild(createText(
                "2. Understand the Golden Record.",
                18f,
                TEXT_COLOR,
                x,
                y + 60f
        ));
        panel.attachChild(createText(
                "3. Decide whether humanity was worth contacting.",
                18f,
                TEXT_COLOR,
                x,
                y + 24f
        ));
        panel.attachChild(createText(
                "A: close objectives",
                15f,
                MUTED_TEXT_COLOR,
                x,
                y - 10f
        ));

        return panel;
    }

   /* private Node createRadarPanel(float screenWidth, float screenHeight) {
        Node panel = new Node("AlienRadarPanel");
        float x = screenWidth - 512f;
        float y = 34f;
        float panelWidth = 480f;
        float panelHeight = 344f;
        float mapX = x + 20f;
        float mapY = y + 66f;
        float mapWidth = 440f;
        float mapHeight = 247f;

        panel.attachChild(createRectangle(x, y, panelWidth, panelHeight, PANEL_COLOR));
        panel.attachChild(createRectangle(x, y + panelHeight - 5f, panelWidth, 5f, PANEL_EDGE_COLOR));
        panel.attachChild(createText("ALIEN RADAR", 20f, PANEL_EDGE_COLOR, x + 18f, y + panelHeight - 36f));
        panel.attachChild(createText("IUT campus - signal triangulation", 14f, MUTED_TEXT_COLOR, x + 18f, y + panelHeight - 58f));
        panel.attachChild(createMapPicture(mapX, mapY, mapWidth, mapHeight));

        // The blue dot is the first lead and later points to Musafir.
        panel.attachChild(createRectangle(mapX + 263f, mapY + 118f, 15f, 15f, SIGNAL_COLOR));
        panel.attachChild(createText("BLUE SIGNAL: MUSAFIR", 14f, SIGNAL_COLOR, x + 18f, y + 38f));
        panel.attachChild(createText("M: close radar", 14f, MUTED_TEXT_COLOR, x + 18f, y + 18f));

        return panel;
    }*/
    private Node createRadarPanel(float screenWidth, float screenHeight) {
        Node panel = new Node("AlienRadarPanel");

        float panelWidth = 480f;
        float panelHeight = 344f;
        float x = screenWidth - panelWidth - 32f;
        float y = 34f;

        radarMapX = x + 20f;
        radarMapY = y + 66f;
        radarMapWidth = 440f;
        radarMapHeight = 210f;

        panel.attachChild(createRectangle(
                x, y, panelWidth, panelHeight, PANEL_COLOR
        ));

        panel.attachChild(createRectangle(
                x, y + panelHeight - 5f,
                panelWidth, 5f, PANEL_EDGE_COLOR
        ));

        panel.attachChild(createText(
                "ALIEN RADAR", 20f, PANEL_EDGE_COLOR,
                x + 18f, y + panelHeight - 36f
        ));

        panel.attachChild(createText(
                "IUT campus - live overhead view",
                14f, MUTED_TEXT_COLOR,
                x + 18f, y + panelHeight - 58f
        ));

        panel.attachChild(createMapPicture(
                radarMapX, radarMapY,
                radarMapWidth, radarMapHeight
        ));

        playerMarker = createRectangle(
                0f, 0f, 10f, 10f, ColorRGBA.Yellow
        );
        playerMarker.setLocalTranslation(0f, 0f, 3f);
        panel.attachChild(playerMarker);

        panel.attachChild(createText(
                "YELLOW MARKER: YOU", 14f, ColorRGBA.Yellow,
                x + 18f, y + 38f
        ));

        panel.attachChild(createText(
                "M: close radar", 14f, MUTED_TEXT_COLOR,
                x + 18f, y + 18f
        ));

        return panel;
    }

    private Geometry createRectangle(
            float x,
            float y,
            float width,
            float height,
            ColorRGBA color
    ) {
        Geometry rectangle = new Geometry("ProloguePanelRectangle", new Quad(width, height));
        Material material = new Material(
                simpleApplication.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"
        );
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(
                RenderState.BlendMode.Alpha
        );
        rectangle.setMaterial(material);
        rectangle.setQueueBucket(RenderQueue.Bucket.Gui);
        rectangle.setLocalTranslation(x, y, 0f);
        return rectangle;
    }

 /*   private Picture createMapPicture(
            float x,
            float y,
            float width,
            float height
    ) {
        Texture2D mapTexture = (Texture2D) simpleApplication
                .getAssetManager()
                .loadTexture("assets/Models/iut-campus-pixel-map.png");
        mapTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        mapTexture.setMagFilter(Texture.MagFilter.Nearest);

        Picture picture = new Picture("IutCampusPixelMap");
        picture.setTexture(simpleApplication.getAssetManager(), mapTexture, false);
        picture.setPosition(x, y);
        picture.setWidth(width);
        picture.setHeight(height);
        picture.setQueueBucket(RenderQueue.Bucket.Gui);
        return picture;
    }
*/
    private Picture createMapPicture(
            float x, float y, float width, float height
    ) {
        int textureWidth = 880;
        int textureHeight = 420;

        simpleApplication.getRootNode().updateGeometricState();

        /*BoundingBox bounds = new BoundingBox();
        bounds.setMinMax(
                campus.getWorldBound().getMin(null),
                campus.getWorldBound().getMax(null)
        );*/
        BoundingBox bounds = new BoundingBox();
        bounds.mergeLocal(campus.getWorldBound());

        Vector3f center = bounds.getCenter();
        float aspect = (float) textureWidth / textureHeight;

        // Fit the entire campus, with a little space around its edges.
        float halfHeight = Math.max(
                bounds.getZExtent(),
                bounds.getXExtent() / aspect
        ) * 1.08f;

        halfHeight = Math.max(halfHeight, 1f);
        float halfWidth = halfHeight * aspect;

        float cameraY = center.y + bounds.getYExtent() + 100f;
        float farDistance = 2f * bounds.getYExtent() + 200f;

        radarCamera = new Camera(textureWidth, textureHeight);
        radarCamera.setParallelProjection(true);
        radarCamera.setFrustum(
                1f, farDistance,
                -halfWidth, halfWidth,
                halfHeight, -halfHeight
        );

        radarCamera.setLocation(new Vector3f(
                center.x, cameraY, center.z
        ));

        // Look straight down; negative Z points toward the map's top.
        radarCamera.lookAtDirection(
                new Vector3f(0f, -1f, 0f),
                new Vector3f(0f, 0f, -1f)
        );
        radarCamera.update();

        radarTexture = new Texture2D(
                textureWidth, textureHeight, Image.Format.RGBA8
        );
        radarTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        radarTexture.setMagFilter(Texture.MagFilter.Bilinear);

        radarBuffer = new FrameBuffer(textureWidth, textureHeight, 1);
        radarBuffer.setDepthTarget(
                FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth)
        );
        radarBuffer.addColorTarget(
                FrameBuffer.FrameBufferTarget.newTarget(radarTexture)
        );

        radarView = simpleApplication.getRenderManager()
                .createPreView("CampusRadarView", radarCamera);

        radarView.setClearFlags(true, true, true);
        radarView.setBackgroundColor(new ColorRGBA(
                0.05f, 0.08f, 0.12f, 1f
        ));
        radarView.setOutputFrameBuffer(radarBuffer);
        radarView.attachScene(simpleApplication.getRootNode());
        radarView.setEnabled(false);

        Picture picture = new Picture("LiveCampusMap");
        picture.setTexture(
                simpleApplication.getAssetManager(), radarTexture, false
        );
        picture.setPosition(x, y);
        picture.setWidth(width);
        picture.setHeight(height);
        picture.setQueueBucket(RenderQueue.Bucket.Gui);
        picture.setLocalTranslation(x, y, 1f);

        return picture;
    }
    private BitmapText createText(
            String text,
            float size,
            ColorRGBA color,
            float x,
            float y
    ) {
        BitmapText label = new BitmapText(font);
        label.setSize(size);
        label.setColor(color);
        label.setText(text);
        label.setQueueBucket(RenderQueue.Bucket.Gui);
        label.setLocalTranslation(x, y, 1f);
        return label;
    }

    private void setPanelVisible(Node panel, boolean visible) {
        panel.setCullHint(
                visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always
        );
    }
}
