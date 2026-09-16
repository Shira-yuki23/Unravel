package pixel;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.ui.Picture;

/** Episode 2 uses Prince's dialogue artwork and a separate top-centre prompt. */
final class EpisodeTwoDialogue {
    private final SimpleApplication app;
    private final Node root = new Node("EpisodeTwoGui");
    private final Node dialogue = new Node("EpisodeTwoDialogue");
    private final Node prompt = new Node("EpisodeTwoPrompt");
    private final Picture plate = new Picture("EpisodeTwoDialoguePlate");
    private final BitmapText text;
    private final BitmapText headline;
    private final BitmapText headlineWeight;
    private final Geometry background;
    private int width = -1;
    private int height = -1;

    EpisodeTwoDialogue(SimpleApplication app) {
        this.app = app;
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        plate.setImage(app.getAssetManager(), "assets/Models/dialogue-plate.png", true);
        dialogue.attachChild(plate);
        text = new BitmapText(font);
        text.setColor(new ColorRGBA(0.13f, 0.11f, 0.08f, 1f));
        dialogue.attachChild(text);

        background = new Geometry("EpisodeTwoBlackPrompt", new Quad(1, 1));
        Material material = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.Black);
        background.setMaterial(material);
        prompt.attachChild(background);
        headline = new BitmapText(font);
        headlineWeight = new BitmapText(font);
        headline.setColor(ColorRGBA.White);
        headlineWeight.setColor(ColorRGBA.White);
        // Offset a second copy by one pixel to give the bitmap font a bold weight.
        prompt.attachChild(headline);
        prompt.attachChild(headlineWeight);
        root.attachChild(dialogue);
        root.attachChild(prompt);
        root.setLocalTranslation(0, 0, 20);
        app.getGuiNode().attachChild(root);
        hideDialogue();
        showPrompt("");
        resize();
    }

    void showDialogue(String speaker, String line) {
        showDialogue(speaker, line, "[ E ] Continue");
    }

    void showDialogue(String speaker, String line, String hint) {
        text.setText(speaker + "\n" + line + (hint.isEmpty() ? "" : "\n\n" + hint));
        dialogue.setCullHint(Spatial.CullHint.Inherit);
    }

    void hideDialogue() {
        dialogue.setCullHint(Spatial.CullHint.Always);
    }

    void showPrompt(String message) {
        headline.setText(message);
        headlineWeight.setText(message);
        prompt.setCullHint(message.isEmpty()
                ? Spatial.CullHint.Always : Spatial.CullHint.Inherit);
        layoutPrompt();
    }

    void resize() {
        int nextWidth = app.getCamera().getWidth();
        int nextHeight = app.getCamera().getHeight();
        if (nextWidth == width && nextHeight == height) return;
        width = nextWidth;
        height = nextHeight;
        float scale = Math.min(1f, Math.min(width / 720f, height / 540f));
        float plateWidth = 600f * scale;
        float plateHeight = 300f * scale;
        float left = (width - plateWidth) / 2f;
        float bottom = Math.min(185f * scale, height * 0.20f);
        plate.setPosition(left, bottom);
        plate.setWidth(plateWidth);
        plate.setHeight(plateHeight);
        text.setSize(20f * scale);
        text.setBox(new Rectangle(0, 0, 488f * scale, 190f * scale));
        text.setLocalTranslation(left + 56f * scale, bottom + 229f * scale, 1);
        headline.setSize(26f * scale);
        headlineWeight.setSize(26f * scale);
        layoutPrompt();
    }

    private void layoutPrompt() {
        if (width < 0) return;
        float size = 26f * Math.min(1f, Math.min(width / 720f, height / 540f));
        headline.setSize(size);
        if (headline.getLineWidth() > width - 64f) {
            size *= (width - 64f) / headline.getLineWidth();
            headline.setSize(size);
        }
        headlineWeight.setSize(size);
        float promptWidth = headline.getLineWidth() + 48f;
        float promptHeight = headline.getLineHeight() + 24f;
        float left = (width - promptWidth) / 2f;
        float bottom = height - promptHeight - 24f;
        background.setLocalScale(promptWidth, promptHeight, 1);
        background.setLocalTranslation(left, bottom, 0);
        headline.setLocalTranslation(left + 24f, height - 36f, 1);
        headlineWeight.setLocalTranslation(left + 25f, height - 36f, 1);
    }

    void setVisible(boolean visible) {
        root.setCullHint(visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    void cleanup() {
        root.removeFromParent();
    }
}
