package pixel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple player-facing list of every shiny object received from NPCs.
 * Press R to open or close the Archaeological Archive.
 */
public final class ArchaeologicalArchiveAppState extends BaseAppState
        implements ActionListener {

    private static final String ARCHIVE_TOGGLE = "archaeological-archive-toggle";

    private final List<Artifact> artifacts = new ArrayList<>();
    private SimpleApplication simpleApplication;
    private Node archiveGui;
    private BitmapText archiveText;
    private boolean visible;

    @Override
    protected void initialize(Application application) {
        if (!(application instanceof SimpleApplication)) {
            throw new IllegalStateException(
                    "ArchaeologicalArchiveAppState requires a SimpleApplication."
            );
        }

        simpleApplication = (SimpleApplication) application;
        BitmapFont font = application.getAssetManager().loadFont(
                "Interface/Fonts/Default.fnt"
        );

        archiveGui = new Node("ArchaeologicalArchiveGui");
        archiveText = new BitmapText(font);
        archiveText.setSize(20f);
        archiveText.setColor(new ColorRGBA(1f, 0.89f, 0.54f, 1f));
        archiveText.setLocalTranslation(
                32f,
                simpleApplication.getCamera().getHeight() - 48f,
                0f
        );
        archiveGui.attachChild(archiveText);
        simpleApplication.getGuiNode().attachChild(archiveGui);

        setArchiveVisible(false);
        renderArchive();

        application.getInputManager().addMapping(
                ARCHIVE_TOGGLE,
                new KeyTrigger(KeyInput.KEY_R)
        );
        application.getInputManager().addListener(this, ARCHIVE_TOGGLE);
    }

    /** Adds one new reward to the Archive. */
    public void addArtifact(String name, String description) {
        artifacts.add(new Artifact(name, description));
        renderArchive();
    }

    public int getArtifactCount() {
        return artifacts.size();
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isEnabled() && ARCHIVE_TOGGLE.equals(name) && isPressed) {
            setArchiveVisible(!visible);
        }
    }

    @Override
    protected void cleanup(Application application) {
        application.getInputManager().removeListener(this);
        application.getInputManager().deleteMapping(ARCHIVE_TOGGLE);

        if (archiveGui != null) {
            archiveGui.removeFromParent();
        }
    }

    @Override
    protected void onEnable() {
        setArchiveVisible(visible);
    }

    @Override
    protected void onDisable() {
        if (archiveGui != null) {
            archiveGui.setCullHint(Spatial.CullHint.Always);
        }
    }

    private void setArchiveVisible(boolean shouldBeVisible) {
        visible = shouldBeVisible;

        if (archiveGui != null) {
            archiveGui.setCullHint(shouldBeVisible
                    ? Spatial.CullHint.Never
                    : Spatial.CullHint.Always);
        }
    }

    private void renderArchive() {
        if (archiveText == null) {
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("ARCHAEOLOGICAL ARCHIVE  [ R ]\n\n");

        if (artifacts.isEmpty()) {
            text.append("No strange objects catalogued yet.");
        } else {
            for (int index = 0; index < artifacts.size(); index++) {
                Artifact artifact = artifacts.get(index);
                text.append(index + 1)
                        .append(". ")
                        .append(artifact.name)
                        .append("\n   ")
                        .append(artifact.description)
                        .append("\n\n");
            }
        }

        archiveText.setText(text.toString());
    }

    private static final class Artifact {
        private final String name;
        private final String description;

        private Artifact(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
}
