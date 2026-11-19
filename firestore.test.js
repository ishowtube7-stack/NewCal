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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "Alex Rivera",
    calorieGoal: 2200,
    proteinGoalG: 165,
    carbsGoalG: 220,
    fatGoalG: 73,
    waterGoalMl: 2500,
    currentWeightKg: 76.5,
    targetWeightKg: 72.0,
    heightCm: 178.0,
    age: 29,
    sex: "male",
    activityLevel: "moderate",
    goalType: "lose",
    useMetricUnits: true,
    onboardingDone: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validMealPayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    dateKey: "2026-11-19",
    name: "Grilled Salmon Quinoa Bowl",
    mealType: "lunch",
    calories: 640,
    proteinG: 44,
    carbsG: 52,
    fatG: 26,
    fiberG: 9,
    healthScore: 9,
    servingMultiplier: 1.0,
    inputSource: "photo",
    aiNotes: "High omega-3 protein with balanced complex carbs.",
    ingredients: ["Grilled Salmon (170g) • 350 kcal", "Quinoa (1 cup) • 220 kcal", "Avocado (1/4) • 70 kcal"],
    loggedAtEpochMs: Date.now() - 5000,
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user: cannot read or write any user collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("meals").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("profile").doc("goals").set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("meals").doc("meal_1").set(validMealPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("2026-11-19").set({
      userId: ALICE_UID,
      waterMl: 500,
      createdAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    })
  );
});

test("Profile goals: owner create, partial update, and security bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("goals");

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));

  await assertSucceeds(
    profileRef.update({
      calorieGoal: 2050,
      proteinGoalG: 170,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / shadow update
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject createdAt tampering
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("other_doc").set(
      validProfilePayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );
});

test("Meals: owner create, filtered dateKey query, partial update, delete, and @DocumentId 'id' rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const mealsCol = aliceDb.collection("users").doc(ALICE_UID).collection("meals");

  await assertSucceeds(mealsCol.doc("meal_1").set(validMealPayload(ALICE_UID)));

  // Mirror Kotlin repository query: .whereEqualTo("dateKey", dateKey)
  await assertSucceeds(mealsCol.where("dateKey", "==", "2026-11-19").get());

  // Mirror partial portion edit
  await assertSucceeds(
    mealsCol.doc("meal_1").update({
      servingMultiplier: 1.5,
      calories: 960,
      proteinG: 66,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject writing @DocumentId 'id' inside payload
  await assertFails(
    mealsCol.doc("meal_2").set({
      id: "meal_2",
      ...validMealPayload(ALICE_UID),
    })
  );

  // Reject invalid healthScore (> 10)
  await assertFails(
    mealsCol.doc("meal_3").set({
      ...validMealPayload(ALICE_UID),
      healthScore: 99,
    })
  );

  await assertSucceeds(mealsCol.doc("meal_1").delete());
});

test("Daily water logs & weights: owner CRUD and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const waterRef = aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("2026-11-19");
  await assertSucceeds(
    waterRef.set({
      userId: ALICE_UID,
      waterMl: 750,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    waterRef.update({
      waterMl: 1000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Weight log
  const weightRef = aliceDb.collection("users").doc(ALICE_UID).collection("weights").doc("w_1");
  await assertSucceeds(
    weightRef.set({
      userId: ALICE_UID,
      weightKg: 76.2,
      dateKey: "2026-11-19",
      note: "Morning weigh-in",
      recordedAtEpochMs: Date.now() - 3000,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user isolation: Bob cannot read or mutate Alice's profile, meals, water, or weights
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("meals").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("2026-11-19").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("weights").doc("w_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("2026-11-19").update({
      waterMl: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
