const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CREATOR_UID = "creator_uid";
const EDITOR_UID = "editor_uid";
const REGISTAR_UID = "registar_uid";
const OUTSIDER_UID = "outsider_uid";
const HOUSEHOLD_ID = "hh_shared_1";

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

async function seedSharedHousehold() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(CREATOR_UID).set({
      uid: CREATOR_UID,
      email: "creator@example.com",
      displayName: "Creator User",
      emailVerifiedConfirmed: true,
    });
    await db.collection("users").doc(EDITOR_UID).set({
      uid: EDITOR_UID,
      email: "editor@example.com",
      displayName: "Editor User",
      emailVerifiedConfirmed: true,
    });
    await db.collection("users").doc(REGISTAR_UID).set({
      uid: REGISTAR_UID,
      email: "registar@example.com",
      displayName: "Registar User",
      emailVerifiedConfirmed: true,
    });
    await db.collection("households").doc(HOUSEHOLD_ID).set({
      id: HOUSEHOLD_ID,
      name: "Οικογένεια",
      ownerId: CREATOR_UID,
      inviteCode: "OIKO-1234",
      memberUids: [CREATOR_UID, EDITOR_UID, REGISTAR_UID],
      invitedEmails: ["editor@example.com", "registar@example.com"],
      memberRoles: {
        [CREATOR_UID]: "CREATOR",
        [EDITOR_UID]: "EDITOR",
        [REGISTAR_UID]: "REGISTAR",
      },
      memberEmails: {
        [EDITOR_UID]: "editor@example.com",
        [REGISTAR_UID]: "registar@example.com",
      },
      memberNames: {
        [EDITOR_UID]: "Editor User",
        [REGISTAR_UID]: "Registar User",
      },
    });
  });
}

test("Unauthenticated user cannot read users or households or subcollections", async () => {
  await seedSharedHousehold();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(CREATOR_UID).get());
  await assertFails(unauthDb.collection("households").doc(HOUSEHOLD_ID).get());
  await assertFails(unauthDb.collection("households").doc(HOUSEHOLD_ID).collection("expenses").get());
  await assertFails(unauthDb.collection("households").doc(HOUSEHOLD_ID).collection("members").get());
});

test("Authenticated user can query single user by email with limit(1), cannot enumerate users, and cannot overwrite another user's profile", async () => {
  await seedSharedHousehold();
  const creatorDb = testEnv.authenticatedContext(CREATOR_UID).firestore();
  await assertSucceeds(
    creatorDb.collection("users").where("email", "==", "editor@example.com").limit(1).get()
  );
  // Bulk enumeration of all users must be denied
  await assertFails(creatorDb.collection("users").get());
  await assertFails(creatorDb.collection("users").limit(5).get());
  await assertFails(
    creatorDb.collection("users").doc(EDITOR_UID).set({ displayName: "Hacked" }, { merge: true })
  );
});

test("Creator can create, rename, share, update roles, and delete household", async () => {
  await seedSharedHousehold();
  const creatorDb = testEnv.authenticatedContext(CREATOR_UID).firestore();

  await assertSucceeds(
    creatorDb.collection("households").doc("hh_new").set({
      id: "hh_new",
      name: "Νέο",
      ownerId: CREATOR_UID,
      inviteCode: "OIKO-9999",
      memberUids: [CREATOR_UID],
      memberRoles: { [CREATOR_UID]: "CREATOR" },
    })
  );

  await assertSucceeds(
    creatorDb.collection("households").doc(HOUSEHOLD_ID).update({
      name: "Ανανεωμένο",
      memberRoles: {
        [CREATOR_UID]: "CREATOR",
        [EDITOR_UID]: "REGISTAR",
        [REGISTAR_UID]: "EDITOR",
      },
    })
  );

  await assertSucceeds(
    creatorDb.collection("households").doc("hh_new").delete()
  );
});

test("Editor can CRUD expenses, incomes, members, and categories, but cannot rename or delete household", async () => {
  await seedSharedHousehold();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  const hRef = editorDb.collection("households").doc(HOUSEHOLD_ID);

  await assertSucceeds(hRef.collection("expenses").doc("e1").set({ title: "Coffee", amount: 3.5 }));
  await assertSucceeds(hRef.collection("expenses").doc("e1").update({ title: "Espresso", amount: 4.0 }));
  await assertSucceeds(hRef.collection("expenses").doc("e1").delete());

  await assertSucceeds(hRef.collection("incomes").doc("i1").set({ title: "Bonus", amount: 200 }));
  await assertSucceeds(hRef.collection("incomes").doc("i1").update({ title: "Bonus 2", amount: 250 }));
  await assertSucceeds(hRef.collection("incomes").doc("i1").delete());

  await assertSucceeds(hRef.collection("members").doc("m1").set({ name: "Maria", role: "Γονέας" }));
  await assertSucceeds(hRef.collection("members").doc("m1").update({ name: "Maria K." }));
  await assertSucceeds(hRef.collection("members").doc("m1").delete());

  await assertSucceeds(hRef.collection("categories").doc("c1").set({ name: "Market", iconKey: "cart" }));
  await assertSucceeds(hRef.collection("categories").doc("c1").update({ name: "Supermarket" }));
  await assertSucceeds(hRef.collection("categories").doc("c1").delete());

  await assertFails(hRef.update({ name: "Renamed By Editor" }));
  await assertFails(hRef.delete());
});

test("Registar can CRUD expenses and incomes and read members/categories, but CANNOT write members/categories or household settings", async () => {
  await seedSharedHousehold();
  const registarDb = testEnv.authenticatedContext(REGISTAR_UID).firestore();
  const hRef = registarDb.collection("households").doc(HOUSEHOLD_ID);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminH = ctx.firestore().collection("households").doc(HOUSEHOLD_ID);
    await adminH.collection("members").doc("m_seed").set({ name: "Dad", role: "Γονέας" });
    await adminH.collection("categories").doc("c_seed").set({ name: "Food", iconKey: "food" });
  });

  // Read allowed
  await assertSucceeds(hRef.get());
  await assertSucceeds(hRef.collection("members").get());
  await assertSucceeds(hRef.collection("categories").get());

  // Expenses & Incomes CRUD allowed
  await assertSucceeds(hRef.collection("expenses").doc("e_reg").set({ title: "Bread", amount: 2.0 }));
  await assertSucceeds(hRef.collection("expenses").doc("e_reg").update({ title: "Bakery", amount: 2.5 }));
  await assertSucceeds(hRef.collection("expenses").doc("e_reg").delete());

  await assertSucceeds(hRef.collection("incomes").doc("i_reg").set({ title: "Gift", amount: 50 }));
  await assertSucceeds(hRef.collection("incomes").doc("i_reg").update({ title: "Gift 2", amount: 60 }));
  await assertSucceeds(hRef.collection("incomes").doc("i_reg").delete());

  // Members & Categories CRUD strictly denied
  await assertFails(hRef.collection("members").doc("m_new").set({ name: "Intruder", role: "Άλλο" }));
  await assertFails(hRef.collection("members").doc("m_seed").update({ name: "Dad Renamed" }));
  await assertFails(hRef.collection("members").doc("m_seed").delete());

  await assertFails(hRef.collection("categories").doc("c_new").set({ name: "Gaming", iconKey: "cart" }));
  await assertFails(hRef.collection("categories").doc("c_seed").update({ name: "Food Edited" }));
  await assertFails(hRef.collection("categories").doc("c_seed").delete());

  // Cannot escalate own role or rename/delete household
  await assertFails(
    hRef.update({
      name: "Hijacked",
    })
  );
  await assertFails(
    hRef.update({
      memberRoles: {
        [CREATOR_UID]: "CREATOR",
        [EDITOR_UID]: "EDITOR",
        [REGISTAR_UID]: "CREATOR",
      },
    })
  );
  await assertFails(hRef.delete());

  // Cannot remove self (leave household) — ONLY creator can remove users
  await assertFails(
    hRef.update({
      memberUids: [CREATOR_UID, EDITOR_UID],
    })
  );
  // Cannot downgrade/change own role to EDITOR
  await assertFails(
    hRef.update({
      memberRoles: {
        [CREATOR_UID]: "CREATOR",
        [EDITOR_UID]: "EDITOR",
        [REGISTAR_UID]: "EDITOR",
      },
    })
  );
});

test("Editor CANNOT change own role, change others' roles, remove self, or remove others; ONLY creator can", async () => {
  await seedSharedHousehold();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  const creatorDb = testEnv.authenticatedContext(CREATOR_UID).firestore();
  const editorH = editorDb.collection("households").doc(HOUSEHOLD_ID);
  const creatorH = creatorDb.collection("households").doc(HOUSEHOLD_ID);

  // Editor cannot change own role to REGISTAR or CREATOR
  await assertFails(
    editorH.update({
      memberRoles: {
        [CREATOR_UID]: "CREATOR",
        [EDITOR_UID]: "REGISTAR",
        [REGISTAR_UID]: "REGISTAR",
      },
    })
  );
  await assertFails(
    editorH.update({
      memberRoles: {
        [CREATOR_UID]: "CREATOR",
        [EDITOR_UID]: "CREATOR",
        [REGISTAR_UID]: "REGISTAR",
      },
    })
  );

  // Editor cannot remove self or another user from memberUids
  await assertFails(
    editorH.update({
      memberUids: [CREATOR_UID, REGISTAR_UID],
    })
  );
  await assertFails(
    editorH.update({
      memberUids: [CREATOR_UID, EDITOR_UID],
    })
  );

  // Creator CAN change roles and remove users
  await assertSucceeds(
    creatorH.update({
      memberRoles: {
        [CREATOR_UID]: "CREATOR",
        [EDITOR_UID]: "REGISTAR",
        [REGISTAR_UID]: "EDITOR",
      },
    })
  );
  await assertSucceeds(
    creatorH.update({
      memberUids: [CREATOR_UID, EDITOR_UID],
      memberRoles: {
        [CREATOR_UID]: "CREATOR",
        [EDITOR_UID]: "REGISTAR",
      },
    })
  );
});

test("CREATOR, EDITOR, and REGISTAR can each set multiple default payers for themselves, but non-owners cannot modify others or roles", async () => {
  await seedSharedHousehold();
  const creatorH = testEnv.authenticatedContext(CREATOR_UID).firestore().collection("households").doc(HOUSEHOLD_ID);
  const editorH = testEnv.authenticatedContext(EDITOR_UID).firestore().collection("households").doc(HOUSEHOLD_ID);
  const registarH = testEnv.authenticatedContext(REGISTAR_UID).firestore().collection("households").doc(HOUSEHOLD_ID);

  // Creator sets 2 default payers for self
  await assertSucceeds(
    creatorH.update({
      [`memberDefaultPayers.${CREATOR_UID}`]: "10,20",
      updatedAt: new Date(),
    })
  );

  // Editor sets 2+ default payers for self
  await assertSucceeds(
    editorH.update({
      [`memberDefaultPayers.${EDITOR_UID}`]: "10,20,30",
      updatedAt: new Date(),
    })
  );

  // Registar sets 2 default payers for self, and can clear to ""
  await assertSucceeds(
    registarH.update({
      [`memberDefaultPayers.${REGISTAR_UID}`]: "20,30",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    registarH.update({
      [`memberDefaultPayers.${REGISTAR_UID}`]: "",
      updatedAt: new Date(),
    })
  );

  // Non-owners CANNOT modify another user's default payers
  await assertFails(
    editorH.update({
      [`memberDefaultPayers.${CREATOR_UID}`]: "30",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    registarH.update({
      [`memberDefaultPayers.${EDITOR_UID}`]: "10",
      updatedAt: new Date(),
    })
  );

  // Non-owners CANNOT piggyback role or memberUids changes onto a default payer update
  await assertFails(
    registarH.update({
      [`memberDefaultPayers.${REGISTAR_UID}`]: "10,20",
      memberUids: [CREATOR_UID, EDITOR_UID],
    })
  );
  await assertFails(
    editorH.update({
      [`memberDefaultPayers.${EDITOR_UID}`]: "10,20",
      memberRoles: {
        [CREATOR_UID]: "CREATOR",
        [EDITOR_UID]: "CREATOR",
        [REGISTAR_UID]: "REGISTAR",
      },
    })
  );

  // Creator CAN still assign default payers for shared users
  await assertSucceeds(
    creatorH.update({
      [`memberDefaultPayers.${REGISTAR_UID}`]: "10,30",
      updatedAt: new Date(),
    })
  );
});

test("Outsider cannot read or write any household data", async () => {
  await seedSharedHousehold();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const hRef = outsiderDb.collection("households").doc(HOUSEHOLD_ID);

  await assertFails(hRef.get());
  await assertFails(hRef.collection("expenses").get());
  await assertFails(hRef.collection("expenses").doc("e_x").set({ title: "Bad", amount: 1 }));
  await assertFails(hRef.collection("members").doc("m_x").set({ name: "Bad" }));
  await assertFails(hRef.collection("categories").doc("c_x").set({ name: "Bad" }));
  await assertFails(hRef.collection("activity").get());
  await assertFails(
    hRef.collection("activity").doc("a_x").set({
      actorUid: OUTSIDER_UID,
      actionType: "EXPENSE_ADDED",
      summaryEl: "Fake",
    })
  );
});

test("Savings goals and recurring expenses allow CREATOR/EDITOR writes and block REGISTAR writes", async () => {
  await seedSharedHousehold();
  const creatorDb = testEnv.authenticatedContext(CREATOR_UID).firestore();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  const registarDb = testEnv.authenticatedContext(REGISTAR_UID).firestore();

  const creatorH = creatorDb.collection("households").doc(HOUSEHOLD_ID);
  const editorH = editorDb.collection("households").doc(HOUSEHOLD_ID);
  const registarH = registarDb.collection("households").doc(HOUSEHOLD_ID);

  await assertSucceeds(creatorH.collection("savings_goals").doc("g1").set({ title: "Trip", targetAmount: 1000 }));
  await assertSucceeds(editorH.collection("savings_goals").doc("g1").update({ currentAmount: 250 }));
  await assertSucceeds(registarH.collection("savings_goals").get());
  await assertFails(registarH.collection("savings_goals").doc("g2").set({ title: "Blocked", targetAmount: 100 }));
  await assertFails(registarH.collection("savings_goals").doc("g1").update({ currentAmount: 500 }));
  await assertFails(registarH.collection("savings_goals").doc("g1").delete());

  await assertSucceeds(creatorH.collection("recurring_expenses").doc("r1").set({ title: "Rent", amount: 600 }));
  await assertSucceeds(editorH.collection("recurring_expenses").doc("r1").update({ amount: 650 }));
  await assertSucceeds(registarH.collection("recurring_expenses").get());
  await assertFails(registarH.collection("recurring_expenses").doc("r2").set({ title: "Blocked", amount: 50 }));
  await assertFails(registarH.collection("recurring_expenses").doc("r1").update({ amount: 700 }));
  await assertFails(registarH.collection("recurring_expenses").doc("r1").delete());
});

test("Household activity log allows member append with matching actorUid and blocks spoofing/modification", async () => {
  await seedSharedHousehold();
  const registarDb = testEnv.authenticatedContext(REGISTAR_UID).firestore();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();

  const regAct = registarDb.collection("households").doc(HOUSEHOLD_ID).collection("activity");
  const edAct = editorDb.collection("households").doc(HOUSEHOLD_ID).collection("activity");

  await assertSucceeds(
    regAct.doc("act_1").set({
      actorUid: REGISTAR_UID,
      actorName: "Registar User",
      actionType: "EXPENSE_ADDED",
      targetType: "EXPENSE",
      summaryEl: "Καταχώρησε έξοδο Καφές",
      summaryEn: "Added expense Coffee",
      amount: 3.5,
      createdAt: Date.now(),
    })
  );

  await assertSucceeds(edAct.get());

  // Spoofed actorUid rejected
  await assertFails(
    regAct.doc("act_spoof").set({
      actorUid: CREATOR_UID,
      actionType: "EXPENSE_ADDED",
      summaryEl: "Spoof",
    })
  );

  // Update or delete rejected (append-only)
  await assertFails(regAct.doc("act_1").update({ summaryEl: "Tampered" }));
  await assertFails(edAct.doc("act_1").delete());
});

test("phone_password_resets collection is completely locked for both unauthenticated and authenticated clients", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const authDb = testEnv.authenticatedContext(CREATOR_UID).firestore();

  await assertFails(unauthDb.collection("phone_password_resets").doc("6900000000").set({ code: "123456" }));
  await assertFails(unauthDb.collection("phone_password_resets").doc("6900000000").get());
  await assertFails(authDb.collection("phone_password_resets").doc("6900000000").set({ code: "123456" }));
  await assertFails(authDb.collection("phone_password_resets").doc("6900000000").get());
});

test("Clients cannot self-grant or tamper with isPremium on /users or /households, while normal updates succeed", async () => {
  await seedSharedHousehold();
  const creatorDb = testEnv.authenticatedContext(CREATOR_UID).firestore();
  const newUserUid = "new_user_uid";
  const newUserDb = testEnv.authenticatedContext(newUserUid).firestore();

  // Cannot create a user profile with isPremium: true
  await assertFails(
    newUserDb.collection("users").doc(newUserUid).set({
      uid: newUserUid,
      email: "new@example.com",
      displayName: "New User",
      isPremium: true,
    })
  );

  // Can create a user profile with isPremium: false
  await assertSucceeds(
    newUserDb.collection("users").doc(newUserUid).set({
      uid: newUserUid,
      email: "new@example.com",
      displayName: "New User",
      isPremium: false,
    })
  );

  // Cannot update own user profile to escalate isPremium to true
  await assertFails(
    newUserDb.collection("users").doc(newUserUid).update({ isPremium: true })
  );
  await assertFails(
    creatorDb.collection("users").doc(CREATOR_UID).set({ isPremium: true }, { merge: true })
  );

  // Normal user profile update still succeeds
  await assertSucceeds(
    creatorDb.collection("users").doc(CREATOR_UID).set({ displayName: "Creator Updated" }, { merge: true })
  );

  // Cannot create a household with isPremium: true
  await assertFails(
    creatorDb.collection("households").doc("hh_fake_pro").set({
      id: "hh_fake_pro",
      name: "Fake Pro",
      ownerId: CREATOR_UID,
      inviteCode: "OIKO-7777",
      memberUids: [CREATOR_UID],
      memberRoles: { [CREATOR_UID]: "CREATOR" },
      isPremium: true,
    })
  );

  // Cannot update existing household to set isPremium: true
  await assertFails(
    creatorDb.collection("households").doc(HOUSEHOLD_ID).update({ isPremium: true })
  );

  // Normal household rename still succeeds
  await assertSucceeds(
    creatorDb.collection("households").doc(HOUSEHOLD_ID).update({ name: "Renamed Safely" })
  );
});

test("Sensitive profile PII lives exclusively in owner-only /users/{uid}/private/profile and is blocked on /users/{uid}", async () => {
  await seedSharedHousehold();
  const creatorDb = testEnv.authenticatedContext(CREATOR_UID).firestore();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const newUid = "privacy_new_uid";
  const newDb = testEnv.authenticatedContext(newUid).firestore();

  // 1. Creating a user profile with sensitive PII on root /users/{uid} must FAIL
  await assertFails(
    newDb.collection("users").doc(newUid).set({
      uid: newUid,
      email: "privacy@example.com",
      displayName: "Privacy User",
      phoneNumber: "+306900000000",
      birthDate: "01/01/1990",
      occupation: "Engineer",
      city: "Athens",
    })
  );

  // 2. Creating clean public profile on /users/{uid} + private PII in /users/{uid}/private/profile must SUCCEED
  await assertSucceeds(
    newDb.collection("users").doc(newUid).set({
      uid: newUid,
      email: "privacy@example.com",
      displayName: "Privacy User",
      firstName: "Privacy",
      lastName: "User",
      isPremium: false,
    })
  );
  await assertSucceeds(
    newDb.collection("users").doc(newUid).collection("private").doc("profile").set({
      phoneNumber: "+306900000000",
      birthDate: "01/01/1990",
      occupation: "Engineer",
      city: "Athens",
    })
  );

  // 3. Owner can read and update their own /users/{uid}/private/profile
  await assertSucceeds(
    newDb.collection("users").doc(newUid).collection("private").doc("profile").get()
  );
  await assertSucceeds(
    newDb.collection("users").doc(newUid).collection("private").doc("profile").update({
      city: "Thessaloniki",
    })
  );

  // 4. Other authenticated users (even household members) and unauthenticated users CANNOT read or write /users/{uid}/private/profile
  await assertFails(
    creatorDb.collection("users").doc(newUid).collection("private").doc("profile").get()
  );
  await assertFails(
    editorDb.collection("users").doc(newUid).collection("private").doc("profile").get()
  );
  await assertFails(
    outsiderDb.collection("users").doc(newUid).collection("private").doc("profile").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(newUid).collection("private").doc("profile").get()
  );
  await assertFails(
    outsiderDb.collection("users").doc(newUid).collection("private").doc("profile").set({ city: "Hacked" })
  );

  // 5. Updating /users/{uid} to add sensitive PII must FAIL
  await assertFails(
    newDb.collection("users").doc(newUid).update({ phoneNumber: "+306911111111" })
  );
  await assertFails(
    newDb.collection("users").doc(newUid).set({ city: "Athens" }, { merge: true })
  );

  // 6. Legacy migration: existing user with legacy PII on /users/{uid} can migrate to /private/profile and scrub root fields
  const legacyUid = "legacy_user_uid";
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(legacyUid).set({
      uid: legacyUid,
      email: "legacy@example.com",
      displayName: "Legacy User",
      phoneNumber: "+306922222222",
      birthDate: "15/05/1988",
      occupation: "Teacher",
      city: "Patras",
    });
  });
  const legacyDb = testEnv.authenticatedContext(legacyUid).firestore();
  await assertSucceeds(
    legacyDb.collection("users").doc(legacyUid).collection("private").doc("profile").set({
      phoneNumber: "+306922222222",
      birthDate: "15/05/1988",
      occupation: "Teacher",
      city: "Patras",
    })
  );
  // Pre-migration unrelated preference update still succeeds
  await assertSucceeds(
    legacyDb.collection("users").doc(legacyUid).set({ themeMode: "DARK" }, { merge: true })
  );
  // Deleting legacy PII fields from root via rewrite without those keys succeeds
  await assertSucceeds(
    legacyDb.collection("users").doc(legacyUid).set({
      uid: legacyUid,
      email: "legacy@example.com",
      displayName: "Legacy User",
      themeMode: "DARK",
    })
  );

  // 7. Household sharing email lookup still succeeds on public /users/{uid}
  await assertSucceeds(
    creatorDb.collection("users").where("email", "==", "privacy@example.com").limit(1).get()
  );

  // 8. Owner can delete /users/{uid}/private/profile on account deletion
  await assertSucceeds(
    newDb.collection("users").doc(newUid).collection("private").doc("profile").delete()
  );
});



