# UNRAVEL ending

The final scene is configured in `lib/src/main/java/pixel/EndingAppState.java`.
The two world positions are exactly those supplied for the scene:

- Musafir: `(-197.6030, 1.0355, 63.0970)`.
- Player walk endpoint: `(-67.6267, 1.0400, 64.9067)`.

Musafir disappears after Episode 2 and returns at 13 collected objects. The radar
points to his final location. Approaching within five units starts the conversation
only when all 16 objects are collected and the memory videos have finished.
The existing flags remain authoritative: crow reward + language module + seven
Episode 3 memories + seven Episode 4 memories. No independent counter is stored.

Use **E** to advance dialogue, **Y / 1** for Contact them, or **N / 2** for Leave
them alone. Both branches lead to the same ending. The choice is available from
`getDecision()`, player user data `finalMoralDecision`, and Java Preferences under
the pixel package (the last completed choice is overwritten by a new choice).
The rest of the game has no new save/resume system.

After the goals and their fade, the camera switches to first person. Musafir eases
alongside the player's actual position, then both follow a smooth direct route.
The player stops at the supplied endpoint; Musafir stops 3.5 units to the right,
derived from the walk direction. His existing billboard artwork is reused, so this
does not add a new walking animation. Bullet movement is disabled during the
cinematic; no per-frame physics warps are used. The straight route and its relative
formation should be visually checked in the actual game from different approaches.

`READY_FOR_FINAL_VIDEO` is the future Impulse Field integration point. Currently it
pauses/fades for two seconds and opens `assets/Models/end.mp4` through the existing
JavaFX player. There is no invented Impulse Field location or extra world trigger.
Playback failure offers E to retry and Escape to exit. After playback or the existing
video skip action, the scene shows THE END and Escape exits.

`EndingChecks` contains headless tests for both choices, 12/13/15/16 progress gates,
the last-video lock, HUD suppression, goals timing, first-person camera, smooth
movement, exact endpoint, formation spacing and ending asset availability.
Run with the project's JDK 21: `gradlew.bat :lib:test --tests pixel.EndingChecks`.
The test class also has a standalone main for environments where Gradle cannot run.
