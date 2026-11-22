const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_captain_1";
const BOB_UID = "bob_skipper_2";
const CHARLIE_UID = "charlie_intruder_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function buildUserProfilePayload(uid, now) {
  return {
    userId: uid,
    displayName: "Virat Striker",
    matchesPlayed: 0,
    matchesWon: 0,
    totalRuns: 0,
    totalWickets: 0,
    highScore: 0,
    sixesHit: 0,
    foursHit: 0,
    careerLevel: 1,
    rating: 1000,
    preferredTeamId: "ind_tigers",
    createdAt: now,
    updatedAt: now,
  };
}

function buildMatchRecordPayload(uid, now) {
  return {
    userId: uid,
    mode: "T20",
    homeTeam: "India Tigers",
    awayTeam: "Australia Sharks",
    homeScore: "184/4 (20.0)",
    awayScore: "172/8 (20.0)",
    resultSummary: "India Tigers won by 12 runs",
    won: true,
    playerRuns: 184,
    playerWickets: 8,
    stadiumName: "Emerald Bowl",
    createdAt: now,
  };
}

function buildLeaderboardPayload(uid, now) {
  return {
    userId: uid,
    displayName: "Virat Striker",
    teamCode: "IND",
    rating: 1240,
    matchesWon: 12,
    totalRuns: 1450,
    sixesHit: 64,
    publicVisible: true,
    updatedAt: now,
  };
}

function buildOnlineRoomPayload(hostUid, now) {
  return {
    roomCode: "RCC482",
    hostUid,
    hostName: "Alice",
    guestUid: "",
    guestName: "",
    participantUids: [hostUid],
    status: "WAITING",
    format: "QUICK_5",
    oversLimit: 5,
    stadiumId: "emerald_bowl",
    hostTeamId: "ind_tigers",
    guestTeamId: "aus_sharks",
    currentInnings: 1,
    battingSide: "HOST",
    hostRuns: 0,
    hostWickets: 0,
    hostBalls: 0,
    guestRuns: 0,
    guestWickets: 0,
    guestBalls: 0,
    lastBallEvent: "Waiting for challenger",
    pendingDelivery: "",
    turnNumber: 0,
    winnerUid: "",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated context: denied on all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(buildUserProfilePayload(ALICE_UID, past))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("matchHistory").get()
  );
  await assertFails(
    unauthDb.collection("leaderboard").where("publicVisible", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("onlineRooms").where("status", "==", "WAITING").get()
  );
});

test("UserProfile: owner create, partial update, cross-user denial, ghost field & future timestamp rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildUserProfilePayload(ALICE_UID, past))
  );

  // Partial update succeeds while preserving createdAt and userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      matchesPlayed: 1,
      matchesWon: 1,
      totalRuns: 78,
      highScore: 78,
      rating: 1030,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ rating: 9999 }));

  // Immutable createdAt mutation denied
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field & @DocumentId 'id' rejected on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...buildUserProfilePayload(BOB_UID, past),
      id: BOB_UID,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...buildUserProfilePayload(BOB_UID, past),
      isAdmin: true,
    })
  );

  // Tampered future timestamp rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set(
      buildUserProfilePayload(BOB_UID, new Date(Date.now() + 60000))
    )
  );
});

test("MatchHistory: owner create and list succeed; update immutable; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("matchHistory")
      .doc("match_001")
      .set(buildMatchRecordPayload(ALICE_UID, past))
  );

  // Subcollection list query without resource.data filter succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matchHistory").get()
  );

  // Immutable update rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("matchHistory")
      .doc("match_001")
      .update({ won: false })
  );

  // Cross-user read & write denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("matchHistory").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("matchHistory")
      .doc("match_002")
      .set(buildMatchRecordPayload(BOB_UID, past))
  );
});

test("Leaderboard: owner write & filtered public query succeed; unfiltered query & spoofing fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set(buildLeaderboardPayload(ALICE_UID, past))
  );

  // Filtered query matching rule succeeds
  await assertSucceeds(
    bobDb.collection("leaderboard").where("publicVisible", "==", true).get()
  );

  // Unfiltered list query denied
  await assertFails(bobDb.collection("leaderboard").get());

  // Cross-user overwrite denied
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({ rating: 9000 })
  );
});

test("OnlineRooms: create, join, authoritative score bounds, and terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("onlineRooms").doc("room_101").set(buildOnlineRoomPayload(ALICE_UID, past))
  );

  // Filtered waiting query & participant query succeed; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("onlineRooms").where("status", "==", "WAITING").get()
  );
  await assertSucceeds(
    aliceDb.collection("onlineRooms").where("participantUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("onlineRooms").get());

  // Bob joins waiting room
  await assertSucceeds(
    bobDb.collection("onlineRooms").doc("room_101").update({
      guestUid: BOB_UID,
      guestName: "Bob",
      guestTeamId: "eng_lions",
      participantUids: [ALICE_UID, BOB_UID],
      status: "IN_PROGRESS",
      lastBallEvent: "Bob joined! Match underway.",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Non-participant Charlie cannot read or update in-progress room
  await assertFails(charlieDb.collection("onlineRooms").doc("room_101").get());
  await assertFails(
    charlieDb.collection("onlineRooms").doc("room_101").update({
      hostRuns: 6,
      turnNumber: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Server-authoritative score validation: jumping +12 runs in one ball fails
  await assertFails(
    aliceDb.collection("onlineRooms").doc("room_101").update({
      hostRuns: 12,
      hostBalls: 1,
      turnNumber: 1,
      lastBallEvent: "Cheated 12 runs",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Legitimate +6 runs in 1 ball succeeds
  await assertSucceeds(
    aliceDb.collection("onlineRooms").doc("room_101").update({
      hostRuns: 6,
      hostBalls: 1,
      turnNumber: 1,
      lastBallEvent: "SIX! Straight over the sightscreen!",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Finish match
  await assertSucceeds(
    aliceDb.collection("onlineRooms").doc("room_101").update({
      status: "FINISHED",
      winnerUid: ALICE_UID,
      lastBallEvent: "Alice won!",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: cannot update finished room
  await assertFails(
    bobDb.collection("onlineRooms").doc("room_101").update({
      guestRuns: 6,
      turnNumber: 2,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});
