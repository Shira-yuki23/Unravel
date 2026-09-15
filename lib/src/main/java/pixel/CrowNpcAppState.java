package pixel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
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
import com.jme3.ui.Picture;
import java.util.ArrayList;
import java.util.List;

/**
 * Prince's one-time story encounter.
 *
 * <p>Approach the clock tower and press E. After the short conversation,
 * Prince flies down to the player, gives the Signal Locket, then vanishes in
 * a little magical poof. The reward is stored in the Archaeological Archive.</p>
 */
public final class CrowNpcAppState extends BaseAppState
        implements ActionListener {

    private static final String TOWER_NODE_NAME = "mosque";
    private static final String CROW_TEXTURE =
            "assets/Models/cute-crow-flying-sprite.png";
    private static final String DIALOGUE_PLATE_TEXTURE =
            "assets/Models/dialogue-plate.png";
    private static final String SHINY_OBJECT_TEXTURE =
            "assets/Models/shiny-object.png";
    private static final String TALK_MAPPING = "crow-prince-talk";

    private static final float INTERACTION_DISTANCE = 16f;
    private static final float CROW_SIZE = 5f;
    private static final float DELIVERY_SECONDS = 1.8f;
    private static final String FINAL_PRINCE_LINE =
            "Keep it safe. When it glows, follow the blue signal.";

    private enum EncounterPhase {
        PERCHED,
        FLYING_TO_PLAYER,
        DELIVERED,
        VANISHED
    }

    private final Spatial player;
    private final ArchaeologicalArchiveAppState archive;
    private final List<Material> poofMaterials = new ArrayList<>();

    private SimpleApplication simpleApplication;
    private Node crowNode;
    private Node spritePivot;
    private Node poofNode;
    private Vector3f perchPosition;
    private Vector3f deliveryStartPosition;
    private BitmapText promptText;
    private Picture dialoguePlate;
    private Picture rewardIcon;
    private BitmapText dialogueText;
    private EncounterPhase phase = EncounterPhase.PERCHED;
    private float idleTime;
    private float deliveryTime;
    private float poofTime;
    private boolean dialogueVisible;
    private int dialogueStep;

    public CrowNpcAppState(
            Spatial player,
            ArchaeologicalArchiveAppState archive) {
        if (player == null || archive == null) {
            throw new IllegalArgumentException(
                    "CrowNpcAppState needs both the player and the Archive."
            );
        }

        this.player = player;
        this.archive = archive;
    }

    @Override
    protected void initialize(Application application) {
        if (!(application instanceof SimpleApplication)) {
            throw new IllegalStateException(
                    "CrowNpcAppState requires a SimpleApplication."
            );
        }

        simpleApplication = (SimpleApplication) application;
        simpleApplication.getRootNode().updateGeometricState();
        perchPosition = findPerchPosition();

        crowNode = new Node("CrowPrince");
        crowNode.setLocalTranslation(perchPosition);
        crowNode.addControl(new BillboardControl());

        spritePivot = new Node("CrowPrinceSpritePivot");
        spritePivot.setLocalScale(CROW_SIZE, CROW_SIZE, CROW_SIZE);
        spritePivot.attachChild(createCrowSprite());
        crowNode.attachChild(spritePivot);
        simpleApplication.getRootNode().attachChild(crowNode);

        createGui();
        application.getInputManager().addMapping(
                TALK_MAPPING,
                new KeyTrigger(KeyInput.KEY_E)
        );
        application.getInputManager().addListener(this, TALK_MAPPING);

        System.out.println("CROW PRINCE PERCHED AT " + perchPosition);
    }

    @Override
    public void update(float tpf) {
        updatePoof(tpf);

        if (phase == EncounterPhase.VANISHED) {
            return;
        }

        if (dialogueVisible) {
            if (phase == EncounterPhase.PERCHED) {
                updatePerchedVisual(tpf);
            }
            return;
        }

        switch (phase) {
            case PERCHED -> updatePerched(tpf);
            case FLYING_TO_PLAYER -> updateDelivery(tpf);
            case DELIVERED, VANISHED -> {
                // Both are handled by the dialogue or early return above.
            }
            default -> throw new IllegalStateException("Unknown crow encounter phase.");
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!TALK_MAPPING.equals(name) || !isPressed) {
            return;
        }

        if (phase == EncounterPhase.PERCHED) {
            if (dialogueVisible) {
                advanceDialogue();
            } else if (isPlayerCloseToTower()) {
                startDialogue();
            }
        } else if (phase == EncounterPhase.DELIVERED && dialogueVisible) {
            poofAndVanish();
        }
    }

    @Override
    protected void cleanup(Application application) {
        application.getInputManager().removeListener(this);
        application.getInputManager().deleteMapping(TALK_MAPPING);

        if (crowNode != null) {
            crowNode.removeFromParent();
        }
        if (poofNode != null) {
            poofNode.removeFromParent();
        }
        if (promptText != null) {
            promptText.removeFromParent();
        }
        if (dialoguePlate != null) {
            dialoguePlate.removeFromParent();
        }
        if (rewardIcon != null) {
            rewardIcon.removeFromParent();
        }
        if (dialogueText != null) {
            dialogueText.removeFromParent();
        }
    }

    @Override
    protected void onEnable() {
        if (crowNode != null && phase != EncounterPhase.VANISHED) {
            crowNode.setCullHint(Spatial.CullHint.Inherit);
        }
    }

    @Override
    protected void onDisable() {
        if (crowNode != null) {
            crowNode.setCullHint(Spatial.CullHint.Always);
        }
        hidePrompt();
        hideDialogue();
    }

    public boolean isEncounterComplete() {
        return phase == EncounterPhase.VANISHED;
    }

    private Geometry createCrowSprite() {
        Texture texture = simpleApplication.getAssetManager().loadTexture(CROW_TEXTURE);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        float aspectRatio = (float) texture.getImage().getWidth()
                / texture.getImage().getHeight();
        Geometry sprite = new Geometry("CrowPrinceSprite", new Quad(aspectRatio, 1f));
        sprite.setLocalTranslation(-aspectRatio * 0.5f, -0.5f, 0f);

        Material material = new Material(
                simpleApplication.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"
        );
        material.setTexture("ColorMap", texture);
        material.setColor("Color", ColorRGBA.White);
        material.getAdditionalRenderState().setBlendMode(
                RenderState.BlendMode.Alpha
        );
        sprite.setMaterial(material);
        sprite.setQueueBucket(RenderQueue.Bucket.Transparent);
        return sprite;
    }

    private void createGui() {
        BitmapFont font = simpleApplication.getAssetManager().loadFont(
                "Interface/Fonts/Default.fnt"
        );

        promptText = new BitmapText(font);
        promptText.setSize(19f);
        promptText.setColor(new ColorRGBA(1f, 0.90f, 0.52f, 1f));
        promptText.setLocalTranslation(
                simpleApplication.getCamera().getWidth() * 0.5f - 128f,
                92f,
                0f
        );
        promptText.setCullHint(Spatial.CullHint.Always);
        simpleApplication.getGuiNode().attachChild(promptText);

        dialoguePlate = new Picture("DialoguePlate");
        dialoguePlate.setImage(
                simpleApplication.getAssetManager(),
                DIALOGUE_PLATE_TEXTURE,
                false
        );
        dialoguePlate.setPosition(
                simpleApplication.getCamera().getWidth() * 0.5f - 300f,
                185f
        );
        dialoguePlate.setWidth(600f);
        dialoguePlate.setHeight(300f);
        dialoguePlate.setCullHint(Spatial.CullHint.Always);
        simpleApplication.getGuiNode().attachChild(dialoguePlate);

        rewardIcon = new Picture("ShinyObjectRewardIcon");
        rewardIcon.setImage(
                simpleApplication.getAssetManager(),
                SHINY_OBJECT_TEXTURE,
                false
        );
        rewardIcon.setPosition(
                simpleApplication.getCamera().getWidth() * 0.5f + 300f,
                278f
        );
        rewardIcon.setWidth(120f);
        rewardIcon.setHeight(120f);
        rewardIcon.setCullHint(Spatial.CullHint.Always);
        simpleApplication.getGuiNode().attachChild(rewardIcon);

        dialogueText = new BitmapText(font);
        dialogueText.setSize(20f);
        dialogueText.setColor(new ColorRGBA(0.13f, 0.11f, 0.08f, 1f));
        dialogueText.setLocalTranslation(
                simpleApplication.getCamera().getWidth() * 0.5f - 244f,
                414f,
                0f
        );
        dialogueText.setCullHint(Spatial.CullHint.Always);
        simpleApplication.getGuiNode().attachChild(dialogueText);
    }

    private void updatePerched(float tpf) {
        updatePerchedVisual(tpf);

        if (isPlayerCloseToTower()) {
            idleTime = 0f;
            showPrompt("[ E ]  Talk to Prince");
        } else {
            hidePrompt();
        }
    }

    private void updatePerchedVisual(float tpf) {
        idleTime += tpf;
        float bob = FastMath.sin(idleTime * 2.5f) * 0.12f;
        crowNode.setLocalTranslation(perchPosition.x, perchPosition.y + bob,
                perchPosition.z);
        spritePivot.setLocalScale(CROW_SIZE, CROW_SIZE, CROW_SIZE);
    }

    private void startDialogue() {
        dialogueVisible = true;
        dialogueStep = 0;
        hidePrompt();
        hideRewardIcon();
        showDialogue(
                "CROW PRINCE\n"
                        + "Caw. You followed the blue signal too, didn't you?\n\n"
                        + "[ E ] Continue"
        );
    }

    private void advanceDialogue() {
        switch (dialogueStep) {
            case 0 -> {
                dialogueStep = 1;
                showDialogue(
                        "CROW PRINCE\n"
                                + "I found something bright where it should not be.\n"
                                + "It has been waiting for you.\n\n"
                                + "[ E ] Continue"
                );
            }
            case 1 -> {
                dialogueStep = 2;
                showDialogue(
                        "CROW PRINCE\n"
                                + "Stay there, explorer. I will bring it down.\n\n"
                                + "[ E ] Let Prince deliver it"
                );
            }
            case 2 -> beginDelivery();
            default -> throw new IllegalStateException("Unknown crow dialogue step.");
        }
    }

    private void beginDelivery() {
        dialogueVisible = false;
        hideDialogue();
        deliveryStartPosition = crowNode.getWorldTranslation().clone();
        deliveryTime = 0f;
        phase = EncounterPhase.FLYING_TO_PLAYER;
    }

    private void updateDelivery(float tpf) {
        deliveryTime += tpf;
        float amount = smoothStep(deliveryTime / DELIVERY_SECONDS);
        Vector3f playerTarget = player.getWorldTranslation().add(0f, 3.6f, 0f);
        Vector3f position = new Vector3f().interpolateLocal(
                deliveryStartPosition,
                playerTarget,
                amount
        );
        position.y += FastMath.sin(amount * FastMath.PI) * 4f;
        crowNode.setLocalTranslation(position);
        animateFlap(deliveryTime);

        if (deliveryTime >= DELIVERY_SECONDS) {
            crowNode.setLocalTranslation(playerTarget);
            spritePivot.setLocalScale(CROW_SIZE, CROW_SIZE, CROW_SIZE);
            deliverObject();
        }
    }

    private void deliverObject() {
        phase = EncounterPhase.DELIVERED;
        dialogueVisible = true;
        archive.addArtifact(
                "Signal Locket",
                "A golden locket with a cyan gem that answers the blue signal."
        );
        rewardIcon.setCullHint(Spatial.CullHint.Never);
        showDialogue(
                "CROW PRINCE\n"
                        + "Take the Signal Locket. " + FINAL_PRINCE_LINE + "\n\n"
                        + "Added to the Archaeological Archive. [ R ]\n"
                        + "[ E ] Say goodbye"
        );
        System.out.println("CROW PRINCE GAVE: Signal Locket");
    }

    private void poofAndVanish() {
        Vector3f poofPosition = crowNode.getWorldTranslation().clone();
        crowNode.removeFromParent();
        hideDialogue();
        createPoof(poofPosition);
        phase = EncounterPhase.VANISHED;
        dialogueVisible = false;
        System.out.println("CROW PRINCE VANISHED IN A POOF");
    }

    private void createPoof(Vector3f position) {
        poofNode = new Node("CrowPrincePoof");
        poofNode.setLocalTranslation(position);

        for (int index = 0; index < 8; index++) {
            Geometry sparkle = new Geometry(
                    "PrincePoofSparkle" + index,
                    new Sphere(8, 8, 0.24f)
            );
            float angle = index * FastMath.TWO_PI / 8f;
            sparkle.setLocalTranslation(
                    FastMath.cos(angle) * 0.9f,
                    (index % 3) * 0.45f,
                    FastMath.sin(angle) * 0.9f
            );

            Material material = new Material(
                    simpleApplication.getAssetManager(),
                    "Common/MatDefs/Misc/Unshaded.j3md"
            );
            ColorRGBA colour = index % 2 == 0
                    ? new ColorRGBA(0.20f, 0.85f, 1f, 1f)
                    : new ColorRGBA(1f, 0.78f, 0.20f, 1f);
            material.setColor("Color", colour);
            material.getAdditionalRenderState().setBlendMode(
                    RenderState.BlendMode.Alpha
            );
            sparkle.setMaterial(material);
            sparkle.setQueueBucket(RenderQueue.Bucket.Transparent);
            poofMaterials.add(material);
            poofNode.attachChild(sparkle);
        }

        simpleApplication.getRootNode().attachChild(poofNode);
        poofTime = 0f;
    }

    private void updatePoof(float tpf) {
        if (poofNode == null) {
            return;
        }

        poofTime += tpf;
        float progress = FastMath.clamp(poofTime / 0.85f, 0f, 1f);
        poofNode.setLocalScale(1f + progress * 3f);

        for (Material material : poofMaterials) {
            ColorRGBA colour = (ColorRGBA) material.getParam("Color").getValue();
            material.setColor(
                    "Color",
                    new ColorRGBA(colour.r, colour.g, colour.b, 1f - progress)
            );
        }

        if (progress >= 1f) {
            poofNode.removeFromParent();
            poofNode = null;
            poofMaterials.clear();
        }
    }

    private void showDialogue(String text) {
        dialoguePlate.setCullHint(Spatial.CullHint.Never);
        dialogueText.setText(text);
        dialogueText.setCullHint(Spatial.CullHint.Never);
    }

    private void hideDialogue() {
        if (dialoguePlate != null) {
            dialoguePlate.setCullHint(Spatial.CullHint.Always);
        }
        hideRewardIcon();
        if (dialogueText != null) {
            dialogueText.setCullHint(Spatial.CullHint.Always);
        }
    }

    private void hideRewardIcon() {
        if (rewardIcon != null) {
            rewardIcon.setCullHint(Spatial.CullHint.Always);
        }
    }

    private void showPrompt(String text) {
        promptText.setText(text);
        promptText.setCullHint(Spatial.CullHint.Never);
    }

    private void hidePrompt() {
        if (promptText != null) {
            promptText.setCullHint(Spatial.CullHint.Always);
        }
    }

    private boolean isPlayerCloseToTower() {
        Vector3f playerPosition = player.getWorldTranslation();
        float xDifference = playerPosition.x - perchPosition.x;
        float zDifference = playerPosition.z - perchPosition.z;
        return xDifference * xDifference + zDifference * zDifference
                <= INTERACTION_DISTANCE * INTERACTION_DISTANCE;
    }

    private Vector3f findPerchPosition() {
        Spatial tower = findSpatialByName(
                simpleApplication.getRootNode(),
                TOWER_NODE_NAME
        );

        if (tower != null && tower.getWorldBound() != null) {
            BoundingVolume bounds = tower.getWorldBound();
            Vector3f perch = bounds.getCenter().clone();

            if (bounds instanceof BoundingBox box) {
                perch.y += box.getYExtent() + CROW_SIZE * 0.35f;
            } else {
                perch.y += CROW_SIZE;
            }

            return perch;
        }

        System.err.println(
                "CROW PRINCE: Could not find 'mosque'; using the scene centre."
        );
        return fallbackPerchPosition();
    }

    private Vector3f fallbackPerchPosition() {
        BoundingVolume sceneBounds = simpleApplication.getRootNode().getWorldBound();

        if (sceneBounds instanceof BoundingBox box) {
            Vector3f fallback = box.getCenter().clone();
            fallback.y += box.getYExtent() + CROW_SIZE;
            return fallback;
        }

        return new Vector3f(0f, CROW_SIZE, 0f);
    }

    private Spatial findSpatialByName(Spatial current, String wantedName) {
        if (wantedName.equalsIgnoreCase(current.getName())) {
            return current;
        }

        if (current instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                Spatial match = findSpatialByName(child, wantedName);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    private void animateFlap(float time) {
        float flap = 1f + FastMath.sin(time * 17f) * 0.12f;
        spritePivot.setLocalScale(CROW_SIZE * flap, CROW_SIZE / flap, CROW_SIZE);
    }

    private float smoothStep(float value) {
        float clamped = FastMath.clamp(value, 0f, 1f);
        return clamped * clamped * (3f - 2f * clamped);
    }
}
