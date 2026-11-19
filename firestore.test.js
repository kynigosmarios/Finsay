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

test("Authenticated user can query users for verified lookup, but cannot overwrite another user's profile", async () => {
  await seedSharedHousehold();
  const creatorDb = testEnv.authenticatedContext(CREATOR_UID).firestore();
  await assertSucceeds(
    creatorDb.collection("users").where("email", "==", "editor@example.com").get()
  );
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

