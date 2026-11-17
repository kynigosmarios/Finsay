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

  // Can leave household themselves
  await assertSucceeds(
    hRef.update({
      memberUids: [CREATOR_UID, EDITOR_UID],
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
});
