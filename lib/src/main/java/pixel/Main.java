package pixel;

import com.jme3.app.SimpleApplication;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.ui.Picture;

import com.jme3.system.AppSettings;

import com.jme3.texture.Texture2D;
import com.jme3.texture.Image;
import com.jme3.texture.FrameBuffer;

import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;

public class Main extends SimpleApplication {

    // =========================================================
    // PIXEL RESOLUTION
    // =========================================================
	//223-smooth // shifa-pixalating
    private static final int PIXEL_WIDTH = 1280;
    private static final int PIXEL_HEIGHT = 720;


    // =========================================================
    // CHARACTER
    // =========================================================

    private Spatial character;

    private BetterCharacterControl characterControl;

    private float characterSpeed = 5f;
    private static final Vector3f JUMP_FORCE = new Vector3f(0f, 12f, 0f);

    private boolean movingForward = false;
    private boolean movingBackward = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    //223
    private final Vector3f characterFacing =
            new Vector3f(0f, 0f, -1f);

  //  private static final float CAMERA_DISTANCE = 14f; 
    //223
    private float cameraDistance = 14f;

    private static final float MIN_CAMERA_DISTANCE = 6f;
    private static final float MAX_CAMERA_DISTANCE = 26f;
    private static final float CAMERA_ZOOM_SPEED = 20f;
    //
    private static final float CAMERA_HEIGHT = 7f;
    private static final float CAMERA_SMOOTHNESS = 8f;

    // =========================================================
    // PHYSICS
    // =========================================================

    private BulletAppState bulletAppState;
    private EpisodeTwoAppState episodeTwo;
    private EpisodeThreeAppState episodeThree;
    private EpisodeFourAppState episodeFour;
    private EndingAppState ending;
    private MainMenu mainMenu;

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        Main app = new Main();

        AppSettings settings = new AppSettings(true);

        // Use the monitor's native mode; keep the internal pixel framebuffer unchanged.
        java.awt.DisplayMode display = java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
        settings.setResolution(display.getWidth(), display.getHeight());
        settings.setFullscreen(true);
        settings.setTitle("Unravel");

        app.setSettings(settings);
        app.setShowSettings(false);

        app.start();
    }


    // =========================================================
    // INITIALIZATION
    // =========================================================

    @Override
    public void simpleInitApp() {

        // =====================================================
        // PHYSICS
        // =====================================================

        bulletAppState = new BulletAppState();

        stateManager.attach(bulletAppState);

        // Show collision geometry
        bulletAppState.setDebugEnabled(true);


        // =====================================================
        // SKY / BACKGROUND
        // =====================================================

        viewPort.setBackgroundColor(
                new ColorRGBA(
                        0.45f,
                        0.70f,
                        0.90f,
                        1f
                )
        );


        // =====================================================
        // LOAD CAMPUS
        // =====================================================

        Spatial campus = assetManager.loadModel(
                "assets/Models/IUT_Campus.glb"
        );

        rootNode.attachChild(campus);

        // Use the gate exported with the campus model as the spawn marker.
        rootNode.updateGeometricState();
        Spatial gate = findSpatialByName(campus, "gate");

        if (gate == null) {
            throw new IllegalStateException(
                    "Campus model is missing the required 'gate' node"
            );
        }

        Vector3f gateSpawnPosition =
                new Vector3f(
                		27.7568f, 1.0387f, 44.7698f
                );


        // =====================================================
        // CAMPUS COLLISION
        // =====================================================

        CollisionShape campusShape =
                CollisionShapeFactory.createMeshShape(campus);


        RigidBodyControl campusPhysics =
                new RigidBodyControl(
                        campusShape,
                        0
                );


        campus.addControl(campusPhysics);


        bulletAppState
                .getPhysicsSpace()
                .add(campusPhysics);


        // =====================================================
        // LOAD CHARACTER
        // =====================================================

        Spatial characterModel = assetManager.loadModel(
                "assets/Models/character2.glb"
        );


        // =====================================================
        // CHARACTER START POSITION
        // =====================================================

        // The exported model's origin is near its head. Keep physics at the feet,
        // and offset only the artwork inside a separate player node.
        character = createFeetAlignedCharacter(characterModel);
        character.updateGeometricState();
        float characterHeight = 2f * ((com.jme3.bounding.BoundingBox)
                character.getWorldBound()).getYExtent();
        character.setLocalTranslation(gateSpawnPosition);


        rootNode.attachChild(character);


        // =====================================================
        // CHARACTER PHYSICS
        // =====================================================

        characterControl =
                new BetterCharacterControl(
                        0.5f,
                        characterHeight,
                        80f
                );


        character.addControl(characterControl);


        bulletAppState
                .getPhysicsSpace()
                .add(characterControl);

        // Keep the physics body and the visible model aligned at the gate.
        characterControl.warp(gateSpawnPosition);


        // =====================================================
        // GRAVITY
        // =====================================================

        characterControl.setGravity(
                new Vector3f(
                        0f,
                        -20f,
                        0f
                )
        );
        characterControl.setJumpForce(JUMP_FORCE);


        // =====================================================
        // LIGHTING
        // =====================================================

        AmbientLight ambient =
                new AmbientLight();

        ambient.setColor(
                ColorRGBA.White.mult(0.8f)
        );

        rootNode.addLight(ambient);


        // Main light
        DirectionalLight mainLight =
                new DirectionalLight();

        mainLight.setDirection(
                new Vector3f(
                        -1f,
                        -1f,
                        -1f
                ).normalizeLocal()
        );

        mainLight.setColor(
                ColorRGBA.White.mult(0.7f)
        );

        rootNode.addLight(mainLight);


        // Fill light
        DirectionalLight fillLight =
                new DirectionalLight();

        fillLight.setDirection(
                new Vector3f(
                        1f,
                        -0.5f,
                        1f
                ).normalizeLocal()
        );

        fillLight.setColor(
                ColorRGBA.White.mult(0.4f)
        );

        rootNode.addLight(fillLight);


        // Top light
        DirectionalLight topLight =
                new DirectionalLight();

        topLight.setDirection(
                new Vector3f(
                        0f,
                        -1f,
                        0f
                )
        );

        topLight.setColor(
                ColorRGBA.White.mult(0.3f)
        );

        rootNode.addLight(topLight);
        
        //223chngcam
        flyCam.setEnabled(false);

        updateThirdPersonCamera(1f);
        /*

        // =====================================================
        // CAMERA
        // =====================================================

        flyCam.setEnabled(true);


        Vector3f cameraPosition =
                new Vector3f(
                        -45.994f,
                        119.322f,
                        300f
                );


        cam.setLocation(cameraPosition);


        Vector3f sceneCenter =
                new Vector3f(
                        -35.4052f,
                        -11.8757f,
                        1.30936f
                );


        Vector3f cameraDirection =
                sceneCenter
                        .subtract(cameraPosition)
                        .normalizeLocal();


        cam.lookAtDirection(
                cameraDirection,
                Vector3f.UNIT_Y
        );
		*/

        // =====================================================
        // LOW-RESOLUTION FRAMEBUFFER
        // =====================================================

        Texture2D colorTexture =
                new Texture2D(
                        PIXEL_WIDTH,
                        PIXEL_HEIGHT,
                        Image.Format.RGBA8
                );


        colorTexture.setMinFilter(
                Texture2D.MinFilter.NearestNoMipMaps
        );


        colorTexture.setMagFilter(
                Texture2D.MagFilter.Nearest
        );


        // =====================================================
        // DEPTH BUFFER
        // =====================================================

        FrameBuffer frameBuffer =
                new FrameBuffer(
                        PIXEL_WIDTH,
                        PIXEL_HEIGHT,
                        1
                );


        frameBuffer.setDepthBuffer(
                Image.Format.Depth24
        );


        frameBuffer.setColorTexture(
                colorTexture
        );


        // =====================================================
        // RENDER WORLD AT LOW RESOLUTION
        // =====================================================

        viewPort.setOutputFrameBuffer(
                frameBuffer
        );


        // =====================================================
        // DISPLAY PIXELATED IMAGE
        // =====================================================

        Picture pixelScreen =
                new Picture(
                        "PixelScreen"
                );


        pixelScreen.setTexture(
                assetManager,
                colorTexture,
                false
        );


        pixelScreen.setPosition(
                0,
                0
        );


        pixelScreen.setWidth(cam.getWidth());


        pixelScreen.setHeight(cam.getHeight());


        guiNode.attachChild(pixelScreen);


        // =====================================================
        // GUI VIEWPORT
        // =====================================================

        guiViewPort.setClearFlags(
                false,
                false,
                false
        );
        
        //
        //223-paschng
        bulletAppState.setDebugEnabled(false);
        bulletAppState.setEnabled(false);
        setDisplayFps(false);
        setDisplayStatView(false);
        mainMenu = new MainMenu(this, campus, () -> {
            mainMenu = null;
            clearPlayerMovement();
            bulletAppState.setEnabled(true);
            startStory(campus);
        });
        // =====================================================
        // INPUT
        // =====================================================

        // P prints the player feet position for placing story objects.
        inputManager.addMapping("PrintPosition", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addListener(actionListener, "PrintPosition");

        inputManager.addMapping("CharacterJump", new KeyTrigger(KeyInput.KEY_S));

        // A = Forward
        inputManager.addMapping(
                "CharacterForward",
                new KeyTrigger(
                       // KeyInput.KEY_A //fms
                		KeyInput.KEY_UP
                		
                )
        );


        // B = Backward
        inputManager.addMapping(
                "CharacterBackward",
                new KeyTrigger(
                      //fms//  KeyInput.KEY_B
                		KeyInput.KEY_DOWN
                )
        );


        // C = Left
        inputManager.addMapping(
                "CharacterLeft",
                new KeyTrigger(
                       //fms// KeyInput.KEY_C
                		KeyInput.KEY_LEFT
                )
        );


        // D = Right
        inputManager.addMapping(
                "CharacterRight",
                new KeyTrigger(
                       //fms// KeyInput.KEY_D
                		KeyInput.KEY_RIGHT
                )
        );


        inputManager.addListener(
                actionListener,

                "CharacterForward",
                "CharacterBackward",
                "CharacterLeft",
                "CharacterRight",
                "CharacterJump"
        );
        //223
        inputManager.addMapping(
                "CameraZoomIn",
                new MouseAxisTrigger(
                        MouseInput.AXIS_WHEEL,
                        false
                )
        );

        inputManager.addMapping(
                "CameraZoomOut",
                new MouseAxisTrigger(
                        MouseInput.AXIS_WHEEL,
                        true
                )
        );

        inputManager.addListener(
                zoomListener,
                "CameraZoomIn",
                "CameraZoomOut"
        );
        //
        
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public void simpleUpdate(float tpf) {
        if (mainMenu != null) {
            clearPlayerMovement();
            mainMenu.update();
            return;
        }
        // Ending owns movement/camera while dialogue, goals and the walk are active.
        if (ending != null && ending.controlsLocked()) {
            clearPlayerMovement();
            ending.updateCamera(tpf);
            return;
        }

        Vector3f walkDirection =
                new Vector3f(
                        0f,
                        0f,
                        0f
                );


        // -----------------------------------------------------
        // FORWARD
        // -----------------------------------------------------

        if (movingForward) {

            walkDirection.z -= characterSpeed;
        }


        // -----------------------------------------------------
        // BACKWARD
        // -----------------------------------------------------

        if (movingBackward) {

            walkDirection.z += characterSpeed;
        }


        // -----------------------------------------------------
        // LEFT
        // -----------------------------------------------------

        if (movingLeft) {

            walkDirection.x -= characterSpeed;
        }


        // -----------------------------------------------------
        // RIGHT
        // -----------------------------------------------------

        if (movingRight) {

            walkDirection.x += characterSpeed;
        }


        // -----------------------------------------------------
        // NORMALIZE DIAGONAL MOVEMENT
        // -----------------------------------------------------

        if (walkDirection.lengthSquared() > 0) {

            walkDirection.normalizeLocal();

            walkDirection.multLocal(characterSpeed);
        }


        // -----------------------------------------------------
        // CHARACTER MOVEMENT
        // -----------------------------------------------------

        if ((episodeTwo != null && episodeTwo.isDialogueVisible())
                || (episodeThree != null && episodeThree.isVideoPlaying())
                || (episodeFour != null && episodeFour.isVideoPlaying())) {
            walkDirection.set(0f, 0f, 0f);
        }
        characterControl.setWalkDirection(
                walkDirection
        );


        // -----------------------------------------------------
        // CHARACTER DIRECTION
        // -----------------------------------------------------
        
        if (walkDirection.lengthSquared() > 0) {

           /* characterControl.setViewDirection(
                    walkDirection.normalize()
            );*/
        	//223
        	characterFacing.set(walkDirection).normalizeLocal();

        	characterControl.setViewDirection(
        	        characterFacing
        	);
        }
        //223
        updateThirdPersonCamera(tpf);
    }
    
    //223
    private void updateThirdPersonCamera(float tpf) {

        Vector3f lookAtPoint =
                character.getWorldTranslation().add(
                        0f, 2.5f, 0f
                );

        Vector3f wantedCameraPosition =
                lookAtPoint
                        //223//.subtract(characterFacing.mult(CAMERA_DISTANCE))
                .subtract(characterFacing.mult(cameraDistance))
                //
                        .addLocal(0f, CAMERA_HEIGHT, 0f);

        float followAmount =
                Math.min(1f, CAMERA_SMOOTHNESS * tpf);

        Vector3f smoothCameraPosition =
                cam.getLocation().clone().interpolateLocal(
                        wantedCameraPosition,
                        followAmount
                );

        cam.setLocation(smoothCameraPosition);
        cam.lookAt(lookAtPoint, Vector3f.UNIT_Y);
    }

    private void startStory(Spatial campus) {
        stateManager.attach(new PrologueAppState(campus, character));
        ArchaeologicalArchiveAppState archive =
                new ArchaeologicalArchiveAppState();

        stateManager.attach(archive);

        CrowNpcAppState crow = new CrowNpcAppState(character, archive);
        stateManager.attach(crow);
        episodeTwo = new EpisodeTwoAppState(
                character, bulletAppState.getPhysicsSpace(), crow::isEncounterComplete, archive,
                new Vector3f(40.7807f, 0.3821f, 49.6151f),
                new Vector3f(45.4184f, 0.3847f, -48.7126f)
        );
        stateManager.attach(episodeTwo);
        episodeThree = new EpisodeThreeAppState(
                character,
                episodeTwo::isComplete,
                () -> {
                    movingForward = false;
                    movingBackward = false;
                    movingLeft = false;
                    movingRight = false;
                    characterControl.setWalkDirection(new Vector3f());
                }
        );

        stateManager.attach(episodeThree);

        episodeFour = new EpisodeFourAppState(
                character,
                episodeTwo::isComplete,
                () -> {
                    movingForward = false;
                    movingBackward = false;
                    movingLeft = false;
                    movingRight = false;
                    characterControl.setWalkDirection(new Vector3f());
                }
        );
        stateManager.attach(episodeFour);
        ending = new EndingAppState(character, characterControl, this::clearPlayerMovement);
        stateManager.attach(ending);
    }

    private void clearPlayerMovement() {
        movingForward = movingBackward = movingLeft = movingRight = false;
        characterControl.setWalkDirection(Vector3f.ZERO);
    }

    static Node createFeetAlignedCharacter(Spatial model) {
        model.updateGeometricState();
        if (!(model.getWorldBound() instanceof com.jme3.bounding.BoundingBox bounds)) {
            throw new IllegalStateException("Character model has no usable bounds");
        }
        Vector3f center = bounds.getCenter();
        model.move(-center.x, bounds.getYExtent() - center.y, -center.z);
        Node feet = new Node("PlayerFeet");
        feet.attachChild(model);
        feet.updateGeometricState();
        return feet;
    }

    private Spatial findSpatialByName(
            Spatial spatial,
            String name
    ) {
        if (name.equals(spatial.getName())) {
            return spatial;
        }

        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                Spatial match = findSpatialByName(child, name);

                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    // =========================================================
    // KEYBOARD INPUT
    // =========================================================

    private final ActionListener actionListener =
            new ActionListener() {

        @Override
        public void onAction(
                String name,
                boolean isPressed,
                float tpf) {

            if (mainMenu != null || (ending != null && ending.controlsLocked())) return;
            if ("PrintPosition".equals(name) && isPressed) {
                Vector3f position = character.getWorldTranslation();
                System.out.printf(java.util.Locale.ROOT,
                        "PLACEMENT POSITION: new Vector3f(%.4ff, %.4ff, %.4ff)%n",
                        position.x, position.y, position.z);
                return;
            }
            if ("CharacterJump".equals(name) && isPressed) {
                characterControl.jump();
                return;
            }
            if (name.equals(
                    "CharacterForward")) {

                movingForward = isPressed;
            }


            if (name.equals(
                    "CharacterBackward")) {

                movingBackward = isPressed;
            }


            if (name.equals(
                    "CharacterLeft")) {

                movingLeft = isPressed;
            }


            if (name.equals(
                    "CharacterRight")) {

                movingRight = isPressed;
            }
        }
      //fms
    };

    private final AnalogListener zoomListener =
            new AnalogListener() {

        @Override
        public void onAnalog(
                String name,
                float value,
                float tpf
        ) {

            if (mainMenu != null || (ending != null && ending.controlsLocked())) return;
            if (name.equals("CameraZoomIn")) {
                cameraDistance -=
                        value * CAMERA_ZOOM_SPEED;
            }

            if (name.equals("CameraZoomOut")) {
                cameraDistance +=
                        value * CAMERA_ZOOM_SPEED;
            }

            cameraDistance = Math.max(
                    MIN_CAMERA_DISTANCE,
                    Math.min(
                            MAX_CAMERA_DISTANCE,
                            cameraDistance
                    )
            );
        }
    };
}
