package pixel;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.ui.Picture;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

/** Image-backed main menu shown before any story states are attached. */
final class MainMenu {
    private static final String CLICK = "MainMenuStart";
    private final SimpleApplication app;
    private final Runnable start;
    private final Node gui = new Node("MainMenu");

    private final FrameBuffer savedFrameBuffer;
    private final Spatial pixelScreen;
    private final Spatial.CullHint savedPixelCull;

private final boolean savedCursor;
    private final boolean savedWorldEnabled;
    private final Texture background;
    private Material buttonMaterial;
    private float buttonX, buttonY, buttonWidth, buttonHeight;
    private int width, height;
    private boolean closed;
    private final ActionListener clickListener = this::onClick;

    MainMenu(SimpleApplication app, Spatial campus, Runnable start) {
        this.app = app;
        this.start = start;
        background = app.getAssetManager().loadTexture("assets/Models/menu_map.png");
        background.setMagFilter(Texture.MagFilter.Nearest);
        background.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        savedFrameBuffer = app.getViewPort().getOutputFrameBuffer();
        savedWorldEnabled = app.getViewPort().isEnabled();
        pixelScreen = app.getGuiNode().getChild("PixelScreen");
        savedPixelCull = pixelScreen == null ? Spatial.CullHint.Inherit : pixelScreen.getLocalCullHint();
        if (pixelScreen != null) pixelScreen.setCullHint(Spatial.CullHint.Always);
        app.getViewPort().setEnabled(false);
        savedCursor = app.getInputManager().isCursorVisible();
        app.getGuiNode().attachChild(gui);
        app.getInputManager().addMapping(CLICK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(clickListener, CLICK);
        app.getInputManager().setCursorVisible(true);
        layout();
    }

    private Geometry rectangle(String name, float x, float y, float w, float h,
                               float z, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Quad(w, h));
        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        geometry.setMaterial(material);
        geometry.setLocalTranslation(x, y, z);
        gui.attachChild(geometry);
        return geometry;
    }

    private BitmapText text(String value, float size) {
        BitmapText label = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
        label.setText(value);
        label.setSize(size);
        label.setColor(ColorRGBA.White);
        gui.attachChild(label);
        return label;
    }

    private void layout() {
        Camera camera = app.getCamera();
        width = camera.getWidth();
        height = camera.getHeight();
        gui.detachAllChildren();
        Picture picture = new Picture("MenuCampusPicture");
        picture.setTexture(app.getAssetManager(), (com.jme3.texture.Texture2D) background, false);
        float imageScale = Math.max((float) width / background.getImage().getWidth(),
                (float) height / background.getImage().getHeight());
        float imageWidth = background.getImage().getWidth() * imageScale;
        float imageHeight = background.getImage().getHeight() * imageScale;
        picture.setWidth(imageWidth);
        picture.setHeight(imageHeight);
        picture.setLocalTranslation((width - imageWidth) / 2, (height - imageHeight) / 2, 1);
        gui.attachChild(picture);
        float scale = Math.min(width / 1280f, height / 720f);
        float padding = 28f * scale;
        ColorRGBA panel = new ColorRGBA(0.035f, 0.065f, 0.075f, 1f);

        com.jme3.texture.Texture2D titleTexture = (com.jme3.texture.Texture2D)
                app.getAssetManager().loadTexture("assets/Models/menu-title.png");
        titleTexture.setMagFilter(Texture.MagFilter.Bilinear);
        titleTexture.setMinFilter(Texture.MinFilter.Trilinear);
        Picture title = new Picture("SmoothWhiteTitle");
        title.setTexture(app.getAssetManager(), titleTexture, true);
        float titleWidth = 570 * scale;
        float titleHeight = titleWidth * titleTexture.getImage().getHeight()
                / titleTexture.getImage().getWidth();
        title.setWidth(titleWidth);
        title.setHeight(titleHeight);
        title.setLocalTranslation((width - titleWidth) / 2,
                height * 0.54f - titleHeight / 2, 3);
        gui.attachChild(title);
        BitmapText credits = text("by( Musfirat-223, Zarin-249, Iftekhar-231)", 18 * scale);
        float creditsX = width - credits.getLineWidth() - padding;
        float creditsY = height - 18 * scale;
        rectangle("CreditsBackground", creditsX - 10 * scale,
                creditsY - credits.getLineHeight() - 6 * scale,
                credits.getLineWidth() + 20 * scale, credits.getLineHeight() + 12 * scale,
                2, ColorRGBA.Black);
        credits.setLocalTranslation(creditsX, creditsY, 3);

        buttonWidth = 220 * scale;
        buttonHeight = 64 * scale;
        buttonX = (width - buttonWidth) / 2;
        buttonY = height * 0.18f - buttonHeight / 2;
        rectangle("StartShadow", buttonX + 4 * scale, buttonY - 5 * scale,
                buttonWidth, buttonHeight, 2, panel);
        buttonMaterial = rectangle("StartButton", buttonX, buttonY, buttonWidth,
                buttonHeight, 3, new ColorRGBA(0.12f, 0.63f, 0.25f, 1f)).getMaterial();
        BitmapText startText = text("Start", 32 * scale);
        startText.setLocalTranslation((width - startText.getLineWidth()) / 2,
                buttonY + (buttonHeight + startText.getLineHeight()) / 2, 4);

        BitmapText hint = text("use arrows for player control", 22 * scale);
        rectangle("HintBackground", padding - 10 * scale, 18 * scale,
                hint.getLineWidth() + 20 * scale, 38 * scale, 2, panel);
        hint.setLocalTranslation(padding, 46 * scale, 3);


    }

    private boolean hovered() {
        Vector2f mouse = app.getInputManager().getCursorPosition();
        return mouse.x >= buttonX && mouse.x <= buttonX + buttonWidth
                && mouse.y >= buttonY && mouse.y <= buttonY + buttonHeight;
    }

    void update() {
        if (closed) return;
        if (width != app.getCamera().getWidth() || height != app.getCamera().getHeight()) layout();

        buttonMaterial.setColor("Color", hovered()
                ? new ColorRGBA(1f, 0.48f, 0.08f, 1f)
                : new ColorRGBA(0.12f, 0.63f, 0.25f, 1f));
    }

    private void onClick(String name, boolean pressed, float tpf) {
        if (closed || !pressed || !hovered()) return;
        closed = true;
        gui.removeFromParent();


        app.getInputManager().removeListener(clickListener);
        app.getInputManager().deleteMapping(CLICK);
        app.getInputManager().setCursorVisible(savedCursor);
        app.getViewPort().setOutputFrameBuffer(savedFrameBuffer);
        if (pixelScreen != null) pixelScreen.setCullHint(savedPixelCull);
        app.getViewPort().setEnabled(savedWorldEnabled);


        start.run();
    }
}