package pixel;

import java.util.function.BooleanSupplier;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
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

public final class EpisodeThreeAppState extends BaseAppState
        implements ActionListener {

    private static final String ESCAPE = "episode-three-escape";
    private static final float TRIGGER_RADIUS = 2.5f;

    private static final Vector3f[] POSITIONS = {
        new Vector3f(30.8021f, 1.0395f, 93.9796f),
        new Vector3f(-19.1397f, 1.0399f, -5.8376f),
        new Vector3f(-36.4585f, 1.0400f, -61.7493f),
        new Vector3f(-60.2901f, 1.0400f, -73.0625f),
        new Vector3f(-71.8739f, 1.0400f, -116.1707f),
        new Vector3f(41.5420f, 0.9024f, -61.4577f),
        new Vector3f(-49.5028f, 1.0400f, 65.8831f)
    };

    private final Spatial player;
    private final BooleanSupplier episodeTwoFinished;
    private final Runnable clearMovement;

    private final Node world = new Node("EpisodeThreeWorld");
    private final Node[] sprites = new Node[7];
    private final boolean[] completed = new boolean[7];
    private final boolean[] inside = new boolean[7];

    private final EpisodeThreeVideo video = new EpisodeThreeVideo();

    private SimpleApplication app;
    private boolean activated;
    private boolean playing;
    private boolean oldCursorVisible;
    private boolean removed;
    private boolean replacedDefaultExit;

    public EpisodeThreeAppState(
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

        for (int i = 0; i < sprites.length; i++) {
            sprites[i] = createSprite(i + 1);
            sprites[i].setLocalTranslation(POSITIONS[i]);
            world.attachChild(sprites[i]);
        }

        app.getRootNode().attachChild(world);
        world.setCullHint(Spatial.CullHint.Always);

        // Prevent jME's default ESC action from closing the game
        // while a video is opening.
        replacedDefaultExit = app.getInputManager().hasMapping(
                SimpleApplication.INPUT_MAPPING_EXIT
        );

        if (replacedDefaultExit) {
            app.getInputManager().deleteMapping(
                    SimpleApplication.INPUT_MAPPING_EXIT
            );
        }

        app.getInputManager().addMapping(
                ESCAPE, new KeyTrigger(KeyInput.KEY_ESCAPE)
        );
        app.getInputManager().addListener(this, ESCAPE);
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
                "EpisodeThreeSprite" + number,
                new Quad(width, height)
        );

        // Coordinate is the sprite's bottom centre.
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

        Node sprite = new Node("Memory" + number);
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

        for (int i = 0; i < sprites.length; i++) {
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
            video.play(index + 1, error -> {
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
            // Leave the sprite available. Walk away and approach to retry.
            System.err.println(
                    "Episode 3 video " + (index + 1) + ": " + error
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
    public void onAction(String name, boolean pressed, float tpf) {
        if (!ESCAPE.equals(name) || !pressed) {
            return;
        }

        // ESC in the video window is handled by JavaFX.
        // Ignore ESC reaching the game while playback is active.
        if (!playing) {
            app.stop();
        }
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

        app.getInputManager().removeListener(this);
        app.getInputManager().deleteMapping(ESCAPE);

        if (replacedDefaultExit) {
            app.getInputManager().addMapping(
                    SimpleApplication.INPUT_MAPPING_EXIT,
                    new KeyTrigger(KeyInput.KEY_ESCAPE)
            );
        }
    }
}
