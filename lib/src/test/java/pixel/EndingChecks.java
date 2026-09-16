package pixel;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.system.JmeSystem;
import com.jme3.system.NullRenderer;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import java.lang.reflect.Field;
import java.util.Arrays;

/** Headless integration checks; no game save writes and no media window launched. */
public class EndingChecks extends SimpleApplication {
    public void simpleInitApp() { }
    static Object field(Object object, String name) throws Exception {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    static void set(Object object, String name, Object value) throws Exception {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); f.set(object, value);
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void phase(Object object, String value) throws Exception {
        set(object, "phase", Enum.valueOf((Class) field(object, "phase").getClass(), value));
    }
    static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        System.out.println("PASS " + label);
    }
    void runChecks(boolean yes) throws Exception {
        assetManager = JmeSystem.newAssetManager(JmeSystem.getPlatformAssetConfigURL());
        cam = new Camera(1280, 720);
        renderManager = new RenderManager(new NullRenderer());
        DummyMouseInput mouse = new DummyMouseInput(); mouse.initialize();
        DummyKeyInput keyboard = new DummyKeyInput(); keyboard.initialize();
        inputManager = new InputManager(mouse, keyboard, null, null);
        Node player = new Node("test-player");
        player.attachChild(new Geometry("body", new Box(.5f, 1, .5f)));
        rootNode.attachChild(player);
        Geometry campus = new Geometry("test-campus", new Box(200, 1, 200));
        rootNode.attachChild(campus);
        rootNode.updateGeometricState();
        BetterCharacterControl control = new BetterCharacterControl(.5f, 2, 80);
        player.addControl(control);
        PhysicsSpace physics = new PhysicsSpace();
        physics.add(control);
        ArchaeologicalArchiveAppState archive = new ArchaeologicalArchiveAppState();
        CrowNpcAppState crow = new CrowNpcAppState(player, archive);
        EpisodeTwoAppState two = new EpisodeTwoAppState(player, physics, () -> false, archive,
                player.getLocalTranslation(), player.getLocalTranslation());
        EpisodeThreeAppState three = new EpisodeThreeAppState(player, () -> false, () -> {});
        EpisodeFourAppState four = new EpisodeFourAppState(player, () -> false, () -> {});
        PrologueAppState progress = new PrologueAppState(campus, player);
        EndingAppState ending = new EndingAppState(player, control, () -> control.setWalkDirection(Vector3f.ZERO));
        BaseAppState[] states = { archive, crow, two, three, four, progress, ending };
        for (BaseAppState s : states) stateManager.attach(s);
        for (BaseAppState s : states) s.initialize(stateManager, this);
        phase(crow, "VANISHED"); phase(two, "DONE");
        set(three, "activated", true); set(four, "activated", true);
        boolean[] a = (boolean[]) field(three, "completed");
        boolean[] b = (boolean[]) field(four, "completed");
        Arrays.fill(a, true); Arrays.fill(b, 0, 3, true);
        check(progress.getRequiredCount() == 16 && progress.getCollectedCount() == 12, "shared 16-object tracker");
        ending.update(.3f);
        check(ending.getPhase() == EndingAppState.Phase.LOCKED, "hidden at 12");
        b[3] = true; ending.update(.3f);
        check(ending.isMusafirAvailable(), "available at 13");
        player.setLocalTranslation(ending.getFinalMusafirSpawnPosition()); rootNode.updateGeometricState();
        ending.update(.3f);
        check(!ending.controlsLocked(), "13-object interaction locked");
        b[4] = b[5] = true; ending.update(.3f);
        check(!ending.controlsLocked(), "15-object interaction locked");
        b[6] = true; set(four, "playing", true); ending.update(.3f);
        check(!ending.controlsLocked(), "wait for final memory video");
        set(four, "playing", false); ending.update(.3f);
        check(ending.getPhase() == EndingAppState.Phase.FINAL_DIALOGUE, "activate only after all 16");
        check(!progress.isEnabled() && !archive.isEnabled() && !three.isEnabled() && !four.isEnabled(), "HUD and signals suspended");
        for (int i = 0; i < 12; i++) {
            ending.update(.3f); ending.onAction("ending-continue", true, 0); ending.update(1.3f);
        }
        check(ending.getPhase() == EndingAppState.Phase.PLAYER_CHOICE, "exact conversation reaches choice");
        java.util.prefs.Preferences temporary = java.util.prefs.Preferences.userRoot().node("unravel-ending-test-" + System.nanoTime());
        set(ending, "save", temporary);
        ending.update(.3f); ending.onAction(yes ? "ending-yes" : "ending-no", true, 0);
        check(ending.getDecision() == (yes ? EndingAppState.Decision.CONTACT_THEM : EndingAppState.Decision.LEAVE_THEM_ALONE), "stored " + (yes ? "YES" : "NO"));
        check(temporary.get("finalMoralDecision", "").equals(ending.getDecision().name()), "persistent choice");
        temporary.removeNode(); temporary.flush();
        for (int i = 0; i < 3; i++) { ending.update(.3f); ending.onAction("ending-continue", true, 0); }
        check(ending.getPhase() == EndingAppState.Phase.GOALS_ACHIEVED, "both branches reach goals");
        ending.update(7.1f);
        check(ending.getPhase() == EndingAppState.Phase.GOALS_ACHIEVED, "goals hold before fade completes");
        ending.update(1f);
        check(ending.getPhase() == EndingAppState.Phase.SCRIPTED_WALK, "walk follows goals");
        Vector3f start = player.getLocalTranslation().clone();
        ending.update(3f);
        check(player.getLocalTranslation().distance(start) < 1f, "walk starts smoothly without teleport");
        ending.updateCamera(.1f);
        check(cam.getLocation().x == player.getLocalTranslation().x, "first-person camera tracks player");
        for (int i = 0; i < 6000 && ending.getPhase() == EndingAppState.Phase.SCRIPTED_WALK; i++) ending.update(.02f);
        check(ending.getPhase() == EndingAppState.Phase.READY_FOR_FINAL_VIDEO, "clean video handoff");
        check(player.getLocalTranslation().distance(ending.getFinalWalkDestination()) < .001f, "exact supplied walk endpoint");
        Node musafir = (Node) field(ending, "musafir");
        Vector3f delta = musafir.getLocalTranslation().subtract(player.getLocalTranslation()); delta.y = 0;
        check(Math.abs(delta.length() - 3.5f) < .001f, "side-by-side formation retained");
        check(EndingChecks.class.getResource("/assets/Models/end.mp4") != null, "ending video asset exists");
        physics.destroy();
    }
    public static void main(String[] args) throws Exception {
        new EndingChecks().runChecks(true);
        new EndingChecks().runChecks(false);
        System.out.println("ENDING CHECKS PASSED");
    }
    @org.junit.jupiter.api.Test void contactEnding() throws Exception { new EndingChecks().runChecks(true); }
    @org.junit.jupiter.api.Test void leaveAloneEnding() throws Exception { new EndingChecks().runChecks(false); }
}
