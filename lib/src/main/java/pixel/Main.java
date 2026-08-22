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
	//223-smooth
    private static final int PIXEL_WIDTH = 1280;
    private static final int PIXEL_HEIGHT = 720;


    // =========================================================
    // CHARACTER
    // =========================================================

    private Spatial character;

    private BetterCharacterControl characterControl;

    private float characterSpeed = 5f;

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


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        Main app = new Main();

        AppSettings settings = new AppSettings(true);

        settings.setResolution(1280, 720);
        settings.setTitle("Unravel");

        app.setSettings(settings);

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

        character = assetManager.loadModel(
                "assets/Models/character2.glb"
        );


        // =====================================================
        // CHARACTER START POSITION
        // =====================================================

        character.setLocalTranslation(
                -70.9211f,
                9.00049f,
                2.81667f
        );


        rootNode.attachChild(character);


        // =====================================================
        // CHARACTER PHYSICS
        // =====================================================

        characterControl =
                new BetterCharacterControl(
                        0.5f,
                        1.8f,
                        80f
                );


        character.addControl(characterControl);


        bulletAppState
                .getPhysicsSpace()
                .add(characterControl);


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


        pixelScreen.setWidth(
                1280
        );


        pixelScreen.setHeight(
                720
        );


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
        stateManager.attach(new PrologueAppState());
        ArchaeologicalArchiveAppState archive =
                new ArchaeologicalArchiveAppState();

        stateManager.attach(archive);

        stateManager.attach(
                new CrowNpcAppState(character, archive)
        );

        // =====================================================
        // INPUT
        // =====================================================

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
                "CharacterRight"
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