package com.snhu.weighttracker.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.snhu.weighttracker.data.UserRecord;
import com.snhu.weighttracker.data.UserStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * CS-499 Enhancement 1 (Software Design & Engineering) — NEW TEST CLASS.
 *
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: Unit tests for AuthService using an in-memory FakeUserStore (enabled by the
 *          UserStore interface introduced in Enhancement 1) so the authentication logic
 *          can be tested WITHOUT a real SQLite database. Coverage includes input
 *          validation, duplicate-user rejection, and — most importantly — the
 *          anti-user-enumeration guarantee that a bad username and a bad password return
 *          the SAME failure message (Outcome 5).
 *
 * NOTE: The login path calls PasswordHasher, which uses android.util.Base64, so this class
 *       runs under Robolectric. @Config(sdk = 34) pins it to an SDK Robolectric ships a
 *       runtime for, independent of the app's newer target SDK.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AuthServiceTest {

    /** In-memory UserStore standing in for DatabaseHelper during tests. */
    private static final class FakeUserStore implements UserStore {
        private final Map<String, UserRecord> users = new HashMap<>();

        @Override
        public boolean addUser(String username, String passwordHash, String salt) {
            if (users.containsKey(username)) return false;
            users.put(username, new UserRecord(username, passwordHash, salt));
            return true;
        }

        @Override
        public UserRecord getUser(String username) {
            return users.get(username);
        }

        @Override
        public boolean userExists(String username) {
            return users.containsKey(username);
        }

        /** Test helper to pre-seed a user without going through hashing. */
        void seed(String username, String hash, String salt) {
            users.put(username, new UserRecord(username, hash, salt));
        }
    }

    @Test
    public void registerRejectsInvalidUsername() {
        AuthService auth = new AuthService(new FakeUserStore());
        AuthService.AuthResult r = auth.registerUser("ab", "password1");
        assertFalse(r.isSuccess());
    }

    @Test
    public void registerRejectsWeakPassword() {
        AuthService auth = new AuthService(new FakeUserStore());
        AuthService.AuthResult r = auth.registerUser("validuser", "short");
        assertFalse(r.isSuccess());
    }

    @Test
    public void registerRejectsDuplicateUsername() {
        FakeUserStore store = new FakeUserStore();
        store.seed("existing", "somehash", "somesalt");
        AuthService auth = new AuthService(store);
        AuthService.AuthResult r = auth.registerUser("existing", "password1");
        assertFalse(r.isSuccess());
        assertEquals("Username already exists", r.getMessage());
    }

    @Test
    public void loginWithUnknownUserFailsGenerically() {
        AuthService auth = new AuthService(new FakeUserStore());
        AuthService.AuthResult r = auth.login("nobody", "password1");
        assertFalse(r.isSuccess());
        assertEquals(AuthService.GENERIC_LOGIN_FAILURE, r.getMessage());
    }

    @Test
    public void loginWithEmptyInputFailsGenerically() {
        AuthService auth = new AuthService(new FakeUserStore());
        assertEquals(AuthService.GENERIC_LOGIN_FAILURE, auth.login("", "").getMessage());
        assertEquals(AuthService.GENERIC_LOGIN_FAILURE, auth.login(null, null).getMessage());
    }

    /**
     * Anti-enumeration: an unknown username and a wrong password for an existing user
     * must yield the IDENTICAL message so an attacker cannot distinguish the two cases.
     * We seed a user with known hash/salt values; the wrong-password path returns the
     * generic failure without needing real hashing to match.
     */
    @Test
    public void loginDoesNotRevealWhetherUsernameExists() {
        FakeUserStore store = new FakeUserStore();
        store.seed("realuser", "storedhashvalue", "storedsaltvalue");
        AuthService auth = new AuthService(store);

        String unknownUserMsg = auth.login("ghost", "password1").getMessage();
        String wrongPassMsg = auth.login("realuser", "password1").getMessage();

        assertEquals(unknownUserMsg, wrongPassMsg);
        assertEquals(AuthService.GENERIC_LOGIN_FAILURE, unknownUserMsg);
    }
}
