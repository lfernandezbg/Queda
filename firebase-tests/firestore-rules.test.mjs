import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc, deleteDoc, writeBatch, Timestamp } from 'firebase/firestore';

const environment = await initializeTestEnvironment({
  projectId: 'demo-queda-s1',
  firestore: { rules: readFileSync('../firestore.rules', 'utf8') },
});

try {
  await environment.clearFirestore();
  await environment.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'households/home-a'), { ownerUid: 'owner', name: 'Casa A', createdAt: Timestamp.now() });
    await setDoc(doc(db, 'households/home-a/members/owner'), { joinedAt: Timestamp.now() });
    await setDoc(doc(db, 'households/home-a/members/alice'), { joinedAt: Timestamp.now() });
    await setDoc(doc(db, 'households/home-b'), { ownerUid: 'bob', name: 'Casa B', createdAt: Timestamp.now() });
    await setDoc(doc(db, 'households/home-b/members/bob'), { joinedAt: Timestamp.now() });
  });

  const owner = environment.authenticatedContext('owner').firestore();
  const alice = environment.authenticatedContext('alice').firestore();
  const bob = environment.authenticatedContext('bob').firestore();
  const anonymous = environment.unauthenticatedContext().firestore();
  const itemExact = 'households/home-a/items/stock-1';
  const itemPresence = 'households/home-a/items/stock-2';

  // Membership & Access Isolation
  await assertSucceeds(getDoc(doc(owner, 'households/home-a')));
  await assertSucceeds(getDoc(doc(alice, 'households/home-a')));
  await assertFails(getDoc(doc(bob, 'households/home-a')));
  await assertFails(getDoc(doc(anonymous, 'households/home-a')));
  await assertFails(getDoc(doc(alice, 'households/home-b')));

  // Shopping is shared only with members; state transitions cannot forge ownership or metadata.
  const shoppingPath = 'households/home-a/shopping/entry-1';
  await assertSucceeds(setDoc(doc(alice, shoppingPath), {
    displayName: 'Pan', normalizedName: 'pan', purchased: false,
    createdAt: Date.now(), revision: 1, updatedBy: 'alice',
  }));
  await assertSucceeds(getDoc(doc(owner, shoppingPath)));
  await assertFails(getDoc(doc(bob, shoppingPath)));
  await assertFails(setDoc(doc(bob, 'households/home-a/shopping/entry-2'), {
    displayName: 'Leche', normalizedName: 'leche', purchased: false,
    createdAt: Date.now(), revision: 1, updatedBy: 'bob',
  }));
  await assertFails(updateDoc(doc(owner, shoppingPath), { displayName: 'Otro', revision: 2, updatedBy: 'owner' }));
  await assertFails(updateDoc(doc(owner, shoppingPath), { purchased: true, revision: 9, updatedBy: 'owner' }));
  await assertSucceeds(updateDoc(doc(owner, shoppingPath), { purchased: true, revision: 2, updatedBy: 'owner' }));
  assert.equal((await getDoc(doc(alice, shoppingPath))).data().purchased, true);
  await assertSucceeds(updateDoc(doc(alice, shoppingPath), { purchased: false, revision: 3, updatedBy: 'alice' }));

  // Create Valid EXACT Item
  await assertSucceeds(setDoc(doc(alice, itemExact), {
    productId: 'product-1', displayName: 'Leche', normalizedName: 'leche',
    barcode: '8412345678905', trackingMode: 'EXACT', quantityAmount: '2',
    quantityUnit: 'LITER', isPresent: null, revision: 1, updatedBy: 'alice',
  }));

  // Create Valid PRESENCE Item
  await assertSucceeds(setDoc(doc(alice, itemPresence), {
    productId: 'product-2', displayName: 'Sal', normalizedName: 'sal',
    barcode: null, trackingMode: 'PRESENCE', quantityAmount: null,
    quantityUnit: null, isPresent: true, revision: 1, updatedBy: 'alice',
  }));

  // Rejection of Invalid Document Creations
  // Negative quantity on create
  await assertFails(setDoc(doc(alice, 'households/home-a/items/stock-bad1'), {
    productId: 'p-bad1', displayName: 'Bad1', normalizedName: 'bad1',
    barcode: null, trackingMode: 'EXACT', quantityAmount: '-5',
    quantityUnit: 'UNIT', isPresent: null, revision: 1, updatedBy: 'alice',
  }));
  // Zero quantity on create
  await assertFails(setDoc(doc(alice, 'households/home-a/items/stock-bad2'), {
    productId: 'p-bad2', displayName: 'Bad2', normalizedName: 'bad2',
    barcode: null, trackingMode: 'EXACT', quantityAmount: '0',
    quantityUnit: 'UNIT', isPresent: null, revision: 1, updatedBy: 'alice',
  }));
  // Invalid unit on create
  await assertFails(setDoc(doc(alice, 'households/home-a/items/stock-bad3'), {
    productId: 'p-bad3', displayName: 'Bad3', normalizedName: 'bad3',
    barcode: null, trackingMode: 'EXACT', quantityAmount: '1',
    quantityUnit: 'KILOS', isPresent: null, revision: 1, updatedBy: 'alice',
  }));
  // Inconsistent state: EXACT with isPresent != null
  await assertFails(setDoc(doc(alice, 'households/home-a/items/stock-bad4'), {
    productId: 'p-bad4', displayName: 'Bad4', normalizedName: 'bad4',
    barcode: null, trackingMode: 'EXACT', quantityAmount: '1',
    quantityUnit: 'UNIT', isPresent: true, revision: 1, updatedBy: 'alice',
  }));
  // Inconsistent state: PRESENCE with quantityAmount
  await assertFails(setDoc(doc(alice, 'households/home-a/items/stock-bad5'), {
    productId: 'p-bad5', displayName: 'Bad5', normalizedName: 'bad5',
    barcode: null, trackingMode: 'PRESENCE', quantityAmount: '1',
    quantityUnit: null, isPresent: true, revision: 1, updatedBy: 'alice',
  }));
  // Invalid barcode format
  await assertFails(setDoc(doc(alice, 'households/home-a/items/stock-bad6'), {
    productId: 'p-bad6', displayName: 'Bad6', normalizedName: 'bad6',
    barcode: '123', trackingMode: 'EXACT', quantityAmount: '1',
    quantityUnit: 'UNIT', isPresent: null, revision: 1, updatedBy: 'alice',
  }));

  // Rejection of Negative and Invalid Updates
  // Non-member update fails
  await assertFails(updateDoc(doc(bob, itemExact), { quantityAmount: '9', revision: 2, updatedBy: 'bob' }));
  // Member updating quantityAmount = "-5" fails!
  await assertFails(updateDoc(doc(owner, itemExact), { quantityAmount: '-5', revision: 2, updatedBy: 'owner' }));
  // Member updating invalid decimal format (4 decimals) fails!
  await assertFails(updateDoc(doc(owner, itemExact), { quantityAmount: '1.2345', revision: 2, updatedBy: 'owner' }));
  // Member updating invalid unit fails!
  await assertFails(updateDoc(doc(owner, itemExact), { quantityUnit: 'BOXES', revision: 2, updatedBy: 'owner' }));
  // Member adding isPresent to EXACT item fails!
  await assertFails(updateDoc(doc(owner, itemExact), { isPresent: true, revision: 2, updatedBy: 'owner' }));
  // Member adding quantityAmount to PRESENCE item fails!
  await assertFails(updateDoc(doc(owner, itemPresence), { quantityAmount: '5', revision: 2, updatedBy: 'owner' }));

  // Valid Updates Succeed
  await assertSucceeds(updateDoc(doc(owner, itemExact), { quantityAmount: '1.5', revision: 2, updatedBy: 'owner' }));
  assert.equal((await getDoc(doc(alice, itemExact))).data().quantityAmount, '1.5');

  await assertSucceeds(updateDoc(doc(owner, itemPresence), { isPresent: false, revision: 2, updatedBy: 'owner' }));
  assert.equal((await getDoc(doc(alice, itemPresence))).data().isPresent, false);

  // Invite and Join Flow Validation
  const validCode = '0123456789abcdef0123456789abcdef';
  const expiry = Timestamp.fromMillis(Date.now() + 86400000);
  await assertFails(setDoc(doc(bob, `invites/${validCode}`), { householdId: 'home-a', createdBy: 'bob', expiresAt: expiry }));
  await assertSucceeds(setDoc(doc(owner, `invites/${validCode}`), { householdId: 'home-a', createdBy: 'owner', expiresAt: expiry }));
  const guest = environment.authenticatedContext('guest').firestore();
  await assertFails(getDoc(doc(guest, 'households/home-a')));
  await assertFails(setDoc(doc(guest, 'households/home-a/members/guest'), { joinedAt: Timestamp.now() }));
  const join = writeBatch(guest);
  join.set(doc(guest, 'households/home-a/members/guest'), { joinedAt: Timestamp.now(), inviteCode: validCode });
  join.set(doc(guest, 'users/guest'), { householdId: 'home-a' });
  await assertSucceeds(join.commit());
  await assertSucceeds(getDoc(doc(guest, itemExact)));

  // Expired Code Rejection
  const expiredCode = 'ffffffffffffffffffffffffffffffff';
  await environment.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, `invites/${expiredCode}`), {
      householdId: 'home-a', createdBy: 'owner', expiresAt: Timestamp.fromMillis(Date.now() - 3600000),
    });
  });
  const lateGuest = environment.authenticatedContext('late-guest').firestore();
  await assertFails(setDoc(doc(lateGuest, 'households/home-a/members/late-guest'), {
    joinedAt: Timestamp.now(), inviteCode: expiredCode,
  }));

  // Full consumption preserves a valid zero balance; creation still requires positive stock.
  await assertSucceeds(updateDoc(doc(owner, itemExact), { quantityAmount: '0', revision: 3, updatedBy: 'owner' }));
  assert.equal((await getDoc(doc(alice, itemExact))).data().quantityAmount, '0');
  const prepared = 'households/home-a/items/stock-rations';
  await assertSucceeds(setDoc(doc(owner, prepared), {
    productId: 'prepared-product', displayName: 'Lentejas', normalizedName: 'lentejas',
    barcode: null, trackingMode: 'EXACT', quantityAmount: '2',
    quantityUnit: 'RATION', isPresent: null, foodType: 'PREPARED',
    revision: 1, updatedBy: 'owner',
  }));
  await assertSucceeds(updateDoc(doc(alice, prepared), {
    quantityAmount: '1', revision: 2, updatedBy: 'alice',
  }));
  await assertFails(updateDoc(doc(bob, prepared), {
    quantityAmount: '0', revision: 3, updatedBy: 'bob',
  }));

  const renameItem = 'households/home-a/items/stock-rename';
  const oldClaim = `households/home-a/names/${createHash('sha256').update('yogur').digest('hex')}`;
  const newClaim = `households/home-a/names/${createHash('sha256').update('yogur natural').digest('hex')}`;
  await assertSucceeds(setDoc(doc(owner, renameItem), {
    productId: 'rename-product', displayName: 'Yogur', normalizedName: 'yogur',
    barcode: null, trackingMode: 'EXACT', quantityAmount: '2',
    quantityUnit: 'UNIT', isPresent: null, revision: 1, updatedBy: 'owner',
  }));
  await assertSucceeds(setDoc(doc(owner, oldClaim), { stockItemId: 'stock-rename' }));
  await assertFails(deleteDoc(doc(owner, oldClaim)));
  const rename = writeBatch(owner);
  rename.update(doc(owner, renameItem), {
    displayName: 'Yogur natural', normalizedName: 'yogur natural', revision: 2, updatedBy: 'owner',
  });
  rename.set(doc(owner, newClaim), { stockItemId: 'stock-rename' });
  rename.delete(doc(owner, oldClaim));
  await assertSucceeds(rename.commit());
  assert.equal((await getDoc(doc(alice, renameItem))).data().displayName, 'Yogur natural');
  assert.equal((await getDoc(doc(owner, oldClaim))).exists(), false);
  await assertFails(updateDoc(doc(bob, renameItem), {
    displayName: 'Ajeno', normalizedName: 'ajeno', revision: 3, updatedBy: 'bob',
  }));

  const locationPath = 'households/home-a/locations/fridge';
  await assertFails(setDoc(doc(bob, locationPath), {
    name: 'Nevera', normalizedName: 'nevera', archived: false, revision: 1, updatedBy: 'bob',
  }));
  await assertSucceeds(setDoc(doc(owner, locationPath), {
    name: 'Nevera', normalizedName: 'nevera', archived: false, revision: 1, updatedBy: 'owner',
  }));
  await assertSucceeds(updateDoc(doc(owner, itemExact), { locationId: 'fridge', revision: 4, updatedBy: 'owner' }));
  await assertSucceeds(updateDoc(doc(owner, locationPath), { archived: true, revision: 2 }));
  // Existing food keeps its archived location, but new assignments are forbidden.
  await assertSucceeds(updateDoc(doc(owner, itemExact), { label: 'Leche del desayuno', revision: 5, updatedBy: 'owner' }));
  await assertFails(updateDoc(doc(owner, itemPresence), { locationId: 'fridge', revision: 3, updatedBy: 'owner' }));
  await assertFails(updateDoc(doc(owner, itemExact), { locationId: 'missing', revision: 6, updatedBy: 'owner' }));
  await assertFails(updateDoc(doc(owner, itemExact), {
    foodType: 'PREPARED', preparedOn: '2026-10-03', bestBefore: '2026-10-02', revision: 6, updatedBy: 'owner',
  }));
  await assertFails(updateDoc(doc(owner, itemPresence), { foodType: 'PREPARED', revision: 3, updatedBy: 'owner' }));
  // A bad write rolls back the entire Firestore batch.
  const consumption = writeBatch(owner);
  consumption.update(doc(owner, itemExact), { quantityAmount: '1', revision: 6, updatedBy: 'owner' });
  consumption.update(doc(owner, itemPresence), { quantityAmount: '-1', revision: 3, updatedBy: 'owner' });
  await assertFails(consumption.commit());
  assert.equal((await getDoc(doc(owner, itemExact))).data().quantityAmount, '0');

  console.log('Firestore rules: PASS (validation of amounts, units, modes, negative quantities, invites)');
} finally {
  await environment.cleanup();
}
