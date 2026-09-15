package pixel;

import java.util.function.BooleanSupplier;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/** Seven Episode 4 traces, each paired with its same-numbered video. */
public final class EpisodeFourAppState extends BaseAppState {

    private static final float TRIGGER_RADIUS = 2.5f;

    private static final int[] NUMBERS = {
        11, 22, 33, 44, 55, 66, 77
    };

    private static final Vector3f[] POSITIONS = {
        new Vector3f(-69.3762f, 1.0397f, -70.8941f),
        new Vector3f(-123.1982f, 0.9012f, -70.8907f),
        new Vector3f(-128.7655f, 0.9021f, -95.7054f),
        new Vector3f(-164.6184f, 1.0398f, -111.4736f),
        new Vector3f(-164.6182f, 0.9024f, -121.2260f),
        new Vector3f(-81.8545f, 1.0395f, -49.6336f),
        new Vector3f(-111.7705f, 0.9022f, 3.5703f)
    };

    private final Spatial player;
    private final BooleanSupplier episodeTwoFinished;
    private final Runnable clearMovement;

    private final Node world = new Node("EpisodeFourWorld");
    private final Node[] sprites = new Node[NUMBERS.length];
    private final boolean[] completed = new boolean[NUMBERS.length];
    private final boolean[] inside = new boolean[NUMBERS.length];
    private final EpisodeThreeVideo video = new EpisodeThreeVideo();

    private SimpleApplication app;
    private boolean activated;
    private boolean playing;
    private boolean oldCursorVisible;
    private boolean removed;

    public EpisodeFourAppState(
            Spatial player,
            BooleanSupplier episodeTwoFinished,
            Runnable clearMovement
    ) {
        this.player = player;
        this.episodeTwoFinished = episodeTwoFinished;
        this.clearMovement = clearMovement;
    }

    @Override
    protected void initialize(Application application) {
        app = (SimpleApplication) application;

        for (int i = 0; i < NUMBERS.length; i++) {
            sprites[i] = createSprite(NUMBERS[i]);
            sprites[i].setLocalTranslation(POSITIONS[i]);
            world.attachChild(sprites[i]);
        }

        app.getRootNode().attachChild(world);
        world.setCullHint(Spatial.CullHint.Always);
    }

    private Node createSprite(int number) {
        Texture texture = app.getAssetManager().loadTexture(
                "assets/Models/" + number + ".png"
        );

        float height = 2f;
        float width = height
                * texture.getImage().getWidth()
                / texture.getImage().getHeight();

        Geometry image = new Geometry(
                "EpisodeFourSprite" + number,
                new Quad(width, height)
        );
        image.setLocalTranslation(-width / 2f, 0f, 0f);

        Material material = new Material(
                app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"
        );
        material.setColor("Color", ColorRGBA.White);
        material.setTexture("ColorMap", texture);
        material.getAdditionalRenderState().setBlendMode(
                RenderState.BlendMode.Alpha
        );
        material.getAdditionalRenderState().setFaceCullMode(
                RenderState.FaceCullMode.Off
        );
        material.getAdditionalRenderState().setDepthWrite(false);

        image.setMaterial(material);
        image.setQueueBucket(RenderQueue.Bucket.Transparent);

        Node sprite = new Node("EpisodeFourMemory" + number);
        BillboardControl billboard = new BillboardControl();
        billboard.setAlignment(BillboardControl.Alignment.AxialY);
        sprite.addControl(billboard);
        sprite.attachChild(image);
        return sprite;
    }

    @Override
    public void update(float tpf) {
        if (!activated) {
            if (!episodeTwoFinished.getAsBoolean()) {
                return;
            }
            activated = true;
            world.setCullHint(Spatial.CullHint.Inherit);
        }

        if (playing) {
            return;
        }

        Vector3f position = player.getWorldTranslation();
        for (int i = 0; i < NUMBERS.length; i++) {
            if (completed[i]) {
                continue;
            }

            float dx = position.x - POSITIONS[i].x;
            float dz = position.z - POSITIONS[i].z;
            boolean nearby =
                    dx * dx + dz * dz <= TRIGGER_RADIUS * TRIGGER_RADIUS
                    && Math.abs(position.y - POSITIONS[i].y) < 4f;

            if (nearby && !inside[i]) {
                inside[i] = true;
                startVideo(i);
                return;
            }
            inside[i] = nearby;
        }
    }

    private void startVideo(int index) {
        playing = true;
        clearMovement.run();

        oldCursorVisible = app.getInputManager().isCursorVisible();
        app.getInputManager().setCursorVisible(true);

        try {
            video.play(NUMBERS[index], error -> {
                app.enqueue(() -> {
                    if (!removed) {
                        finishVideo(index, error);
                    }
                    return null;
                });
            });
        } catch (RuntimeException error) {
            finishVideo(index, error.toString());
        }
    }

    private void finishVideo(int index, String error) {
        playing = false;
        clearMovement.run();
        app.getInputManager().setCursorVisible(oldCursorVisible);

        if (error == null) {
            completed[index] = true;
            sprites[index].removeFromParent();
        } else {
            System.err.println(
                    "Episode 4 video " + NUMBERS[index] + ": " + error
            );
        }
    }

    public boolean isVideoPlaying() {
        return playing;
    }

    public boolean isComplete() {
        if (!activated) {
            return false;
        }
        for (boolean itemComplete : completed) {
            if (!itemComplete) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onEnable() {
        world.setCullHint(
                activated ? Spatial.CullHint.Inherit : Spatial.CullHint.Always
        );
    }

    @Override
    protected void onDisable() {
        world.setCullHint(Spatial.CullHint.Always);
        video.cancel();
    }

    @Override
    protected void cleanup(Application application) {
        removed = true;
        if (playing) {
            app.getInputManager().setCursorVisible(oldCursorVisible);
        }
        playing = false;
        clearMovement.run();
        video.shutdown();
        world.removeFromParent();
    }
}
