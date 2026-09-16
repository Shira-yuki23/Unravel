package pixel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import java.util.LinkedHashMap;
import java.util.Map;

/** Final encounter. Positions below are the user's supplied feet/ground positions. */
public final class EndingAppState extends BaseAppState implements ActionListener {
    private static final Vector3f FINAL_MUSAFIR_SPAWN_POSITION =
            new Vector3f(-197.6030f, 1.0355f, 63.0970f);
    private static final Vector3f FINAL_WALK_DESTINATION =
            new Vector3f(-67.6267f, 1.0400f, 64.9067f);
    private static final String ENDING_VIDEO = "end";
    private static final String CONTINUE = "ending-continue";
    private static final String YES = "ending-yes";
    private static final String NO = "ending-no";
    private static final String EXIT = "ending-exit";
    private static final float WALK_SPEED = 4f;
    private static final float APPROACH_RADIUS = 5f;
    // Relative formation/artwork offsets, never additional world destinations.
    private static final float SIDE_SPACING = 3.5f;
    private static final Vector3f ARTWORK_OFFSET = new Vector3f(0, 1.5f, 0);

    public enum Phase { LOCKED, MUSAFIR_AVAILABLE, FINAL_DIALOGUE, PLAYER_CHOICE,
        GOALS_ACHIEVED, SCRIPTED_WALK, READY_FOR_FINAL_VIDEO, VIDEO_PLAYING,
        VIDEO_FAILED, COMPLETE }
    public enum Decision { CONTACT_THEM, LEAVE_THEM_ALONE }
    private record Line(String speaker, String text, float pauseAfter) { }
    private static final Line[] CONVERSATION = {
        new Line("ALIEN", "Did humans survive?", 0),
        new Line("MUSAFIR", "Some signals continued.", 0),
        new Line("MUSAFIR", "Some disappeared.", 0),
        new Line("MUSAFIR", "And some... I was never able to trace.", 0),
        new Line("ALIEN", "So they survived?", 0),
        new Line("MUSAFIR", "That is not what I said.", 1.2f),
        new Line("MUSAFIR", "But perhaps survival was never the question.", 0),
        new Line("MUSAFIR", "You have seen what humans created.", 0),
        new Line("MUSAFIR", "You have seen what they destroyed.", 0),
        new Line("MUSAFIR", "And you have seen what they tried to save.", 0),
        new Line("MUSAFIR", "If humans are still out there...", 0.9f),
        new Line("MUSAFIR", "...do you think we should contact them?", 0)
    };
    private final Spatial player;
    private final BetterCharacterControl control;
    private final Runnable clearMovement;
    private final EpisodeThreeVideo video = new EpisodeThreeVideo();
    private final Node overlay = new Node("EndingOverlay");
    private final Node ticks = new Node("CompletedGoalChecks");
    private final Map<BaseAppState, Boolean> previousStates = new LinkedHashMap<>();
    private SimpleApplication app;
    private PrologueAppState progress;
    private EpisodeTwoAppState two;
    private EpisodeThreeAppState three;
    private EpisodeFourAppState four;
    private EpisodeTwoDialogue dialogue;
    private Node musafir;
    private BitmapText goals;
    private Geometry fade;
    private Phase phase = Phase.LOCKED;
    private Decision decision;
    private Line[] lines = CONVERSATION;
    private int lineIndex;
    private float elapsed;
    private float pauseRemaining;
    private float inputDelay;
    private Vector3f walkStart, sideOffset, musafirStart, facing;
    private float walkDuration;
    private float eyeHeight;
    private Spatial.CullHint previousPlayerCull;
    private boolean oldCursor;
    private boolean walkingOwnsPhysics;
    private boolean removed;
    private final java.util.prefs.Preferences save =
            java.util.prefs.Preferences.userNodeForPackage(EndingAppState.class);

    public EndingAppState(Spatial player, BetterCharacterControl control, Runnable clearMovement) {
        this.player = player;
        this.control = control;
        this.clearMovement = clearMovement;
    }

    @Override protected void initialize(Application application) {
        app = (SimpleApplication) application;
        progress = getState(PrologueAppState.class);
        two = getState(EpisodeTwoAppState.class);
        three = getState(EpisodeThreeAppState.class);
        four = getState(EpisodeFourAppState.class);
        dialogue = new EpisodeTwoDialogue(app);
        goals = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
        overlay.setLocalTranslation(0, 0, 40);
        overlay.attachChild(goals);
        overlay.attachChild(ticks);
        fade = rectangle("EndingFade", 1, 1, ColorRGBA.Black);
        fade.setLocalTranslation(0, 0, 2);
        fade.setCullHint(Spatial.CullHint.Always);
        overlay.attachChild(fade);
        app.getGuiNode().attachChild(overlay);
        app.getInputManager().addMapping(CONTINUE, new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addMapping(YES, new KeyTrigger(KeyInput.KEY_Y), new KeyTrigger(KeyInput.KEY_1));
        app.getInputManager().addMapping(NO, new KeyTrigger(KeyInput.KEY_N), new KeyTrigger(KeyInput.KEY_2));
        app.getInputManager().addMapping(EXIT, new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(this, CONTINUE, YES, NO, EXIT);
        previousPlayerCull = player.getLocalCullHint();
        player.updateGeometricState();
        eyeHeight = ((com.jme3.bounding.BoundingBox) player.getWorldBound()).getYExtent() * 1.8f;
    }

    public Phase getPhase() { return phase; }
    public Decision getDecision() { return decision; }
    public boolean controlsLocked() { return phase.ordinal() >= Phase.FINAL_DIALOGUE.ordinal(); }
    public boolean isMusafirAvailable() { return phase == Phase.MUSAFIR_AVAILABLE; }
    public Vector3f getFinalMusafirSpawnPosition() { return FINAL_MUSAFIR_SPAWN_POSITION.clone(); }
    public Vector3f getFinalWalkDestination() { return FINAL_WALK_DESTINATION.clone(); }

    private boolean allCollected() {
        return progress.getRequiredCount() > 0
                && progress.getCollectedCount() == progress.getRequiredCount()
                && two.isComplete() && three.isComplete() && four.isComplete()
                && !three.isVideoPlaying() && !four.isVideoPlaying();
    }

    @Override public void update(float tpf) {
        dialogue.resize();
        elapsed += tpf;
        inputDelay = Math.max(0, inputDelay - tpf);
        switch (phase) {
            case LOCKED -> {
                if (two.isComplete() && progress.getCollectedCount() >= 13) {
                    musafir = two.takeFinalMusafir();
                    app.getRootNode().attachChild(musafir);
                    musafir.setLocalTranslation(FINAL_MUSAFIR_SPAWN_POSITION.add(ARTWORK_OFFSET));
                    musafir.setCullHint(Spatial.CullHint.Inherit);
                    enter(Phase.MUSAFIR_AVAILABLE);
                }
            }
            case MUSAFIR_AVAILABLE -> updateEncounter();
            case FINAL_DIALOGUE -> {
                if (pauseRemaining > 0) {
                    pauseRemaining -= tpf;
                    if (pauseRemaining <= 0) advanceDialogue();
                }
            }
            case GOALS_ACHIEVED -> updateGoals();
            case SCRIPTED_WALK -> updateWalk();
            case READY_FOR_FINAL_VIDEO -> {
                setFade(Math.min(1, elapsed / 1.5f));
                if (elapsed >= 2f) playEnding();
            }
            default -> { }
        }
    }

    private void updateEncounter() {
        if (three.isVideoPlaying() || four.isVideoPlaying()) {
            dialogue.showPrompt("");
            return;
        }
        if (player.getWorldTranslation().distance(FINAL_MUSAFIR_SPAWN_POSITION) > APPROACH_RADIUS) {
            dialogue.showPrompt("");
        } else if (allCollected()) {
            beginDialogue();
        } else {
            dialogue.showPrompt("MUSAFIR  |  " + progress.getCollectedCount() + "/"
                    + progress.getRequiredCount() + " OBJECTS  |  SIGNAL LOCKED");
        }
    }

    private void suspend(BaseAppState state) {
        if (state != null) {
            previousStates.put(state, state.isEnabled());
            state.setEnabled(false);
        }
    }

    private void beginDialogue() {
        clearMovement.run();
        app.setDisplayFps(false);
        app.setDisplayStatView(false);
        com.jme3.bullet.BulletAppState physics = getState(com.jme3.bullet.BulletAppState.class);
        if (physics != null) physics.setDebugEnabled(false);
        oldCursor = app.getInputManager().isCursorVisible();
        suspend(progress);
        suspend(getState(ArchaeologicalArchiveAppState.class));
        suspend(getState(CrowNpcAppState.class));
        suspend(two);
        suspend(three);
        suspend(four);
        app.getInputManager().setCursorVisible(false);
        dialogue.showPrompt("");
        enter(Phase.FINAL_DIALOGUE);
        showLine();
    }

    private void enter(Phase next) { phase = next; elapsed = 0; inputDelay = 0.25f; }
    private void showLine() {
        Line line = lines[lineIndex];
        dialogue.showDialogue(line.speaker(), line.text());
        inputDelay = 0.2f;
    }
    private void advanceDialogue() {
        if (++lineIndex < lines.length) {
            showLine();
        } else if (decision == null) {
            enter(Phase.PLAYER_CHOICE);
            dialogue.showDialogue("YOUR DECISION",
                    "[ Y / 1 ] YES - Contact them.\n\n[ N / 2 ] NO - Leave them alone.", "");
        } else {
            dialogue.hideDialogue();
            enter(Phase.GOALS_ACHIEVED);
        }
    }

    @Override public void onAction(String name, boolean pressed, float tpf) {
        if (!isEnabled() || !pressed || inputDelay > 0) return;
        if (EXIT.equals(name) && controlsLocked() && phase != Phase.VIDEO_PLAYING) {
            app.stop();
        } else if (phase == Phase.PLAYER_CHOICE && (YES.equals(name) || NO.equals(name))) {
            decision = YES.equals(name) ? Decision.CONTACT_THEM : Decision.LEAVE_THEM_ALONE;
            // Keep both an inspectable session decision and a persistent last-ending choice.
            player.setUserData("finalMoralDecision", decision.name());
            try { save.put("finalMoralDecision", decision.name()); save.flush(); }
            catch (Exception error) { System.err.println("Could not persist ending choice: " + error); }
            lines = new Line[] {
                new Line("ALIEN", decision == Decision.CONTACT_THEM ? "Yes." : "No.", 0),
                new Line("ALIEN", decision == Decision.CONTACT_THEM
                        ? "I think they're worth another signal."
                        : "Some stories should be left where we found them.", 0),
                new Line("MUSAFIR", "Then you have your answer.", 0)
            };
            lineIndex = 0;
            enter(Phase.FINAL_DIALOGUE);
            showLine();
        } else if (CONTINUE.equals(name) && phase == Phase.FINAL_DIALOGUE && pauseRemaining <= 0) {
            pauseRemaining = lines[lineIndex].pauseAfter();
            if (pauseRemaining > 0) dialogue.hideDialogue();
            else advanceDialogue();
        } else if (CONTINUE.equals(name) && phase == Phase.VIDEO_FAILED) {
            dialogue.hideDialogue();
            playEnding();
        }
    }

    private Geometry rectangle(String name, float width, float height, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Quad(width, height));
        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geometry.setMaterial(material);
        return geometry;
    }

    private void updateGoals() {
        String[] labels = { "WHO WERE THEY?", "WHAT HAPPENED TO THEM?", "ARE THEY WORTH CONTACTING?" };
        boolean achieved = elapsed >= 4.5f;
        float alpha = elapsed < 7f ? 1 : Math.max(0, 1 - (elapsed - 7f));
        float scale = Math.min(app.getCamera().getWidth() / 1280f, app.getCamera().getHeight() / 720f);
        goals.setSize(30f * scale);
        goals.setColor(new ColorRGBA(1, 1, 1, alpha));
        goals.setText(achieved ? "GOALS ACHIEVED" : labels[Math.min(2, (int) (elapsed / 1.5f))]);
        float x = (app.getCamera().getWidth() - goals.getLineWidth()) / 2f;
        float y = app.getCamera().getHeight() * 0.62f;
        goals.setLocalTranslation(x, y, 0);
        ticks.detachAllChildren();
        if (!achieved) {
            // Draw the check mark geometrically: Default.fnt has no Unicode tick glyph.
            Geometry shortStroke = rectangle("CheckShort", 14 * scale, 4 * scale, ColorRGBA.White);
            Geometry longStroke = rectangle("CheckLong", 30 * scale, 4 * scale, ColorRGBA.White);
            shortStroke.rotate(0, 0, -FastMath.PI / 4);
            longStroke.rotate(0, 0, FastMath.PI / 4);
            float center = app.getCamera().getWidth() / 2f;
            shortStroke.setLocalTranslation(center - 14 * scale, y - 55 * scale, 0);
            longStroke.setLocalTranslation(center - 4 * scale, y - 65 * scale, 0);
            ticks.attachChild(shortStroke);
            ticks.attachChild(longStroke);
        }
        if (elapsed >= 8f) {
            goals.setText("");
            ticks.detachAllChildren();
            beginWalk();
        }
    }

    private void beginWalk() {
        clearMovement.run();
        walkStart = player.getWorldTranslation().clone();
        musafirStart = musafir.getLocalTranslation().subtract(ARTWORK_OFFSET);
        facing = FINAL_WALK_DESTINATION.subtract(walkStart);
        facing.y = 0;
        facing.normalizeLocal();
        sideOffset = facing.cross(Vector3f.UNIT_Y).mult(SIDE_SPACING);
        walkDuration = Math.max(1, walkStart.distance(FINAL_WALK_DESTINATION) / WALK_SPEED);
        // Cinematic transform ownership avoids fighting Bullet or teleporting with warp each frame.
        // The path is derived solely from the actual encounter and supplied endpoint.
        control.setEnabled(false);
        walkingOwnsPhysics = true;
        player.setCullHint(Spatial.CullHint.Always);
        enter(Phase.SCRIPTED_WALK);
    }

    private void updateWalk() {
        // First ease Musafir alongside the actual player position, without snapping either actor.
        Vector3f formation = walkStart.add(sideOffset);
        if (elapsed < 2f) {
            float t = elapsed / 2f;
            t = t * t * (3 - 2 * t);
            musafir.setLocalTranslation(musafirStart.clone().interpolateLocal(formation, t).addLocal(ARTWORK_OFFSET));
            return;
        }
        float t = Math.min(1, (elapsed - 2f) / walkDuration);
        float eased = t * t * (3 - 2 * t);
        Vector3f feet = walkStart.clone().interpolateLocal(FINAL_WALK_DESTINATION, eased);
        player.setLocalTranslation(feet);
        musafir.setLocalTranslation(feet.add(sideOffset).addLocal(ARTWORK_OFFSET));
        if (t >= 1) enter(Phase.READY_FOR_FINAL_VIDEO);
    }

    /** Called after Main's normal movement update; never switch back to the chase camera. */
    public void updateCamera(float tpf) {
        if (!walkingOwnsPhysics) return;
        app.getCamera().setLocation(player.getLocalTranslation().add(0, eyeHeight, 0));
        app.getCamera().lookAtDirection(facing, Vector3f.UNIT_Y);
    }

    private void setFade(float alpha) {
        fade.setCullHint(Spatial.CullHint.Inherit);
        fade.setLocalScale(app.getCamera().getWidth(), app.getCamera().getHeight(), 1);
        fade.getMaterial().setColor("Color", new ColorRGBA(0, 0, 0, alpha));
    }

    // Clean handoff point for a future Impulse Field stage; no extra world trigger is invented.
    private void playEnding() {
        enter(Phase.VIDEO_PLAYING);
        setFade(1);
        video.play(ENDING_VIDEO, error -> app.enqueue(() -> {
            if (removed) return null;
            if (error == null) {
                enter(Phase.COMPLETE);
                fade.setCullHint(Spatial.CullHint.Always);
                dialogue.showPrompt("UNRAVEL  |  THE END  |  ESC TO EXIT");
            } else {
                System.err.println("Ending video: " + error);
                enter(Phase.VIDEO_FAILED);
                fade.setCullHint(Spatial.CullHint.Always);
                dialogue.showDialogue("ENDING VIDEO", "Could not play end.mp4.", "[ E ] Retry    [ ESC ] Exit");
            }
            return null;
        }));
    }

    @Override protected void onEnable() {
        if (dialogue != null) dialogue.setVisible(true);
        overlay.setCullHint(Spatial.CullHint.Inherit);
    }
    @Override protected void onDisable() {
        if (dialogue != null) dialogue.setVisible(false);
        overlay.setCullHint(Spatial.CullHint.Always);
        video.cancel();
    }
    @Override protected void cleanup(Application application) {
        removed = true;
        video.shutdown();
        app.getInputManager().removeListener(this);
        for (String mapping : new String[] { CONTINUE, YES, NO, EXIT }) app.getInputManager().deleteMapping(mapping);
        dialogue.cleanup();
        overlay.removeFromParent();
        if (musafir != null) musafir.removeFromParent();
        player.setCullHint(previousPlayerCull);
        if (walkingOwnsPhysics) {
            control.warp(player.getLocalTranslation());
            control.setEnabled(true);
        }
        if (controlsLocked()) app.getInputManager().setCursorVisible(oldCursor);
        previousStates.forEach(BaseAppState::setEnabled);
        clearMovement.run();
    }
}
