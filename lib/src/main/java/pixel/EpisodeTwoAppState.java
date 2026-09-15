package pixel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import java.util.function.BooleanSupplier;

/** One-time playable signal, language pickup, and MUSAFIR encounter. */
public final class EpisodeTwoAppState extends BaseAppState
        implements ActionListener, PhysicsCollisionListener {
    private static final String CONTINUE = "episode-two-continue";
    private enum Phase { WAITING, OPENING, SEARCHING, BALL, REVEAL, APPROACH, WARNING, DONE }
    private final Spatial player;
    private final PhysicsSpace physics;
    private final BooleanSupplier crowFinished;
    private final ArchaeologicalArchiveAppState archive;
    // Positions denote ground level, not the centre of either sprite.
    private final Vector3f ballPosition;
    private final Vector3f machinePosition;
    private final Node world = new Node("EpisodeTwoWorld");
    private final Node machine = new Node("Musafir");
    private final Node ball = new Node("OldBall");
    private SimpleApplication app;
    private EpisodeTwoDialogue gui;
    private Geometry impulse;
    private RigidBodyControl machineBody;
    private Phase phase = Phase.WAITING;
    private int dialogueStep;
    private float elapsed;
    private float scanDistance;
    private final Vector3f lastPosition = new Vector3f();
    private boolean scanningLineShown;
    private boolean dialogueVisible;
    private boolean physicsRegistered;
    private volatile boolean machineTouched;

    public EpisodeTwoAppState(Spatial player, PhysicsSpace physics,
            BooleanSupplier crowFinished, ArchaeologicalArchiveAppState archive,
            Vector3f ballPosition, Vector3f machinePosition) {
        this.player = player;
        this.physics = physics;
        this.crowFinished = crowFinished;
        this.archive = archive;
        this.ballPosition = ballPosition.clone();
        this.machinePosition = machinePosition.clone();
    }

    @Override protected void initialize(Application application) {
        app = (SimpleApplication) application;
        gui = new EpisodeTwoDialogue(app);
        Geometry oldBall = new Geometry("WornBall", new Sphere(16, 20, 0.55f));
        oldBall.setMaterial(colour(new ColorRGBA(0.42f, 0.31f, 0.19f, 1)));
        oldBall.setLocalTranslation(0, 0.6f, 0);
        ball.attachChild(oldBall);
        impulse = new Geometry("ImpulseField", new Sphere(20, 24, 1.1f));
        Material field = colour(new ColorRGBA(0.15f, 0.8f, 1f, 0.22f));
        field.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        field.getAdditionalRenderState().setDepthWrite(false);
        impulse.setMaterial(field);
        impulse.setQueueBucket(RenderQueue.Bucket.Transparent);
        impulse.setLocalTranslation(0, 0.65f, 0);
        ball.attachChild(impulse);
        ball.setLocalTranslation(ballPosition);
        world.attachChild(ball);

        Texture texture = app.getAssetManager().loadTexture("assets/Models/musafir-scene-2.png");
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        float spriteHeight = 6f;
        float spriteWidth = spriteHeight * texture.getImage().getWidth() / texture.getImage().getHeight();
        Geometry sprite = new Geometry("MusafirSprite", new Quad(spriteWidth, spriteHeight));
        sprite.setLocalTranslation(-spriteWidth / 2, -1.5f, 0);
        Material spriteMaterial = colour(ColorRGBA.White);
        spriteMaterial.setTexture("ColorMap", texture);
        spriteMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        spriteMaterial.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        sprite.setMaterial(spriteMaterial);
        sprite.setQueueBucket(RenderQueue.Bucket.Transparent);
        Node facing = new Node("MusafirArtwork");
        facing.addControl(new BillboardControl());
        facing.attachChild(sprite);
        machine.attachChild(facing);
        machine.setLocalTranslation(machinePosition.add(0, 1.5f, 0));
        machineBody = new RigidBodyControl(new BoxCollisionShape(new Vector3f(2.5f, 1.5f, 2f)), 0);
        machine.addControl(machineBody);
        world.attachChild(machine);
        app.getRootNode().attachChild(world);
        world.setCullHint(Spatial.CullHint.Always);
        machine.setCullHint(Spatial.CullHint.Always);
        app.getInputManager().addMapping(CONTINUE, new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addListener(this, CONTINUE);
    }

    @Override public void update(float tpf) {
        gui.resize();
        if (phase == Phase.WAITING) {
            if (!crowFinished.getAsBoolean()) return;
            phase = Phase.OPENING;
            world.setCullHint(Spatial.CullHint.Inherit);
            lastPosition.set(player.getWorldTranslation());
            say("EXPLORER", "I got stuck here because of one signal.");
        }
        elapsed += tpf;
        impulse.setLocalScale(1f + 0.18f * FastMath.sin(elapsed * 3f));
        if (dialogueVisible) return;
        if (phase == Phase.SEARCHING) {
            Vector3f position = player.getWorldTranslation();
            scanDistance += position.distance(lastPosition);
            lastPosition.set(position);
            if (!scanningLineShown && scanDistance >= 3f) {
                scanningLineShown = true;
                say("EXPLORER", "What's the source of that signal?\nThere's no single living thing here.");
            } else if (scanningLineShown && position.distance(ballPosition) < 3f) {
                phase = Phase.BALL;
                gui.showPrompt("LOOK FRONT  |  [ E ] LOAD LANGUAGE");
            } else {
                gui.showPrompt("SCANNING" + ".".repeat(1 + (int) (elapsed * 2f) % 3));
            }
        } else if (phase == Phase.BALL && player.getWorldTranslation().distance(ballPosition) >= 3f) {
            phase = Phase.SEARCHING;
        } else if (phase == Phase.APPROACH && machineTouched) {
            phase = Phase.WARNING;
            dialogueStep = 0;
            gui.showPrompt("");
            say("UNKNOWN MACHINE", "If you are hearing me, Then humans failed.");
        }
    }

    @Override public void onAction(String name, boolean pressed, float tpf) {
        if (!isEnabled() || !CONTINUE.equals(name) || !pressed) return;
        switch (phase) {
            case OPENING -> {
                hideDialogue();
                phase = Phase.SEARCHING;
                lastPosition.set(player.getWorldTranslation());
            }
            case SEARCHING -> hideDialogue();
            case BALL -> {
                if (player.getWorldTranslation().distance(ballPosition) >= 3f) return;
                ball.removeFromParent();
                archive.addArtifact("Language Module", "Recovered from the old ball in the impulse field.");
                machine.setCullHint(Spatial.CullHint.Inherit);
                registerPhysics();
                phase = Phase.REVEAL;
                dialogueStep = 0;
                gui.showPrompt("LANGUAGE LOADED  |  LOOK FRONT");
                say("EXPLORER", "There's something glowing. I looked towards it.");
            }
            case REVEAL -> {
                if (dialogueStep++ == 0) {
                    say("EXPLORER", "What's that??? Some sort of vehicle??");
                } else {
                    hideDialogue();
                    phase = Phase.APPROACH;
                    gui.showPrompt("LOOK FRONT");
                }
            }
            case WARNING -> {
                if (dialogueStep++ == 0) {
                    say("EXPLORER", "What humans??");
                } else if (dialogueStep == 2) {
                    gui.showPrompt("IDENTIFICATION FOUND  |  UNIT NAME: MUSAFIR");
                    say("MACHINE IDENTIFICATION", "MUSAFIR");
                } else {
                    hideDialogue();
                    gui.showPrompt("");
                    phase = Phase.DONE;
                }
            }
            default -> { }
        }
    }

    @Override public void collision(PhysicsCollisionEvent event) {
        if ((event.getObjectA() == machineBody && event.getNodeB() == player)
                || (event.getObjectB() == machineBody && event.getNodeA() == player)) {
            machineTouched = true;
        }
    }
    public boolean isComplete() {
        return phase == Phase.DONE;
    }
    public boolean isDialogueVisible() {
        return isEnabled() && dialogueVisible;
    }

    private void say(String speaker, String line) {
        dialogueVisible = true;
        gui.showDialogue(speaker, line);
    }

    private void hideDialogue() {
        dialogueVisible = false;
        gui.hideDialogue();
    }

    private Material colour(ColorRGBA colour) {
        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", colour);
        return material;
    }

    private void registerPhysics() {
        if (physicsRegistered) return;
        world.updateGeometricState();
        physics.add(machineBody);
        physics.addCollisionListener(this);
        physicsRegistered = true;
    }

    private void unregisterPhysics() {
        if (!physicsRegistered) return;
        physics.removeCollisionListener(this);
        physics.remove(machineBody);
        physicsRegistered = false;
    }

    @Override protected void onEnable() {
        gui.setVisible(true);
        world.setCullHint(phase == Phase.WAITING ? Spatial.CullHint.Always : Spatial.CullHint.Inherit);
        if (phase.ordinal() >= Phase.REVEAL.ordinal()) registerPhysics();
    }

    @Override protected void onDisable() {
        gui.setVisible(false);
        world.setCullHint(Spatial.CullHint.Always);
        unregisterPhysics();
    }

    @Override protected void cleanup(Application application) {
        unregisterPhysics();
        application.getInputManager().removeListener(this);
        application.getInputManager().deleteMapping(CONTINUE);
        gui.cleanup();
        world.removeFromParent();
    }
}
