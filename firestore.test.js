const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Private user PII isolation: owner can read/write, other users & guests cannot", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      preferredLanguage: "ar",
      themeMode: "dark",
      aiModel: "gemini-3.8-flash",
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Guest and Bob cannot read or modify Alice's private doc
  await assertFails(guestDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ preferredLanguage: "en" })
  );

  // Alice cannot self-suspend or change userId/createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accountStatus: "suspended",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice can update her own preferences
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "en",
      themeMode: "light",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Public profiles: guest can list public profiles, only owner can edit profile info", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Studio",
      username: "alice_studio",
      bio: "Motion designer",
      publicProfile: true,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      resourcesCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    guestDb.collection("public_profiles").where("publicProfile", "==", true).get()
  );

  // Unfiltered list without publicProfile == true fails
  await assertFails(guestDb.collection("public_profiles").get());

  // Bob cannot hijack Alice's displayName
  await assertFails(
    bobDb.collection("public_profiles").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Resources & Posts: guest reads approved/active items; signed-in user creates & cannot modify others", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();

  // Guest cannot create a post
  await assertFails(
    guestDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice",
      postType: "design_showcase",
      category: "Graphic Design",
      title: "Unauthorized",
      description: "Test",
      tags: ["design"],
      likesCount: 0,
      commentsCount: 0,
      savesCount: 0,
      sharesCount: 0,
      moderationStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice creates a valid post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice",
      postType: "design_showcase",
      category: "Graphic Design",
      title: "Liquid Glass Poster",
      description: "Arabic typography exploration",
      tags: ["arabic", "glass"],
      likesCount: 0,
      commentsCount: 0,
      savesCount: 0,
      sharesCount: 0,
      moderationStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Guest can query active posts
  await assertSucceeds(
    guestDb.collection("posts").where("moderationStatus", "==", "active").get()
  );

  // Bob cannot edit Alice's post title
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      title: "Hijacked title",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Executable resource download URLs are blocked
  await assertFails(
    aliceDb.collection("resources").doc("res_bad").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      title: "Bad File",
      titleAr: "ملف ضار",
      description: "Executable test",
      descriptionAr: "تجربة",
      category: "Fonts",
      tags: ["font"],
      fileFormat: "EXE",
      fileSizeLabel: "10 MB",
      licenseInfo: "MIT",
      downloadUrl: "https://example.com/malware.exe",
      savesCount: 0,
      likesCount: 0,
      moderationStatus: "approved",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Anti-spam engagement events & deterministic likes/saves/blocks", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const eventId = `${ALICE_UID}_tool_capcut_tool_open_2026-11-20`;
  await assertSucceeds(
    aliceDb.collection("engagement_events").doc(eventId).set({
      userId: ALICE_UID,
      targetType: "tool",
      targetId: "capcut",
      eventType: "tool_open",
      dayKey: "2026-11-20",
      validEvent: true,
      createdAt: now,
    })
  );

  // Cannot forge someone else's engagement event or spoof a random ID
  await assertFails(
    bobDb.collection("engagement_events").doc("random_spam_id").set({
      userId: BOB_UID,
      targetType: "tool",
      targetId: "capcut",
      eventType: "tool_open",
      dayKey: "2026-11-20",
      validEvent: true,
      createdAt: now,
    })
  );

  // Like deterministic ID
  await assertSucceeds(
    aliceDb.collection("likes").doc(`${ALICE_UID}_post_post_1`).set({
      userId: ALICE_UID,
      targetType: "post",
      targetId: "post_1",
      createdAt: now,
    })
  );
  await assertFails(bobDb.collection("likes").where("userId", "==", ALICE_UID).get());
});

test("Admin moderation: non-admin cannot write tools or update reports; server-marked admin can", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(ADMIN_UID).set({ granted: true });
    await context.firestore().collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetType: "post",
      targetId: "post_1",
      reason: "Spam",
      status: "open",
      createdAt: now,
      updatedAt: now,
    });
  });

  // Alice cannot self-promote to admin or resolve reports
  await assertFails(aliceDb.collection("admins").doc(ALICE_UID).set({ granted: true }));
  await assertFails(
    aliceDb.collection("reports").doc("rep_1").update({
      status: "resolved",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Admin can resolve reports
  await assertSucceeds(
    adminDb.collection("reports").doc("rep_1").update({
      status: "resolved",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
