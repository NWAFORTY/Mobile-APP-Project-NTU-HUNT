package com.example.treasurehuntapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Sign-in screen: Firebase Auth, optional legacy Firestore password, offline demo logins,
 * remember-me (username only), show-password toggle, and hand-off to {@link navigation}.
 * <p>
 * Email/password and password-reset requests that reach Firebase must use an {@code @my.ntu.ac.uk}
 * address ({@link AuthInputValidator#isAllowedNtuLoginEmail}); offline shortcuts ({@code a}/{@code a},
 * {@code admin}, {@link DevOfflineLogin}) skip that check.
 */
public class Login extends AppCompatActivity {

    private static final String TAG = "Login";

    // --- Firebase & UI ---
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private EditText usernameInput;
    private EditText passwordInput;
    private CheckBox rememberCheck;
    private Button enterButton;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // --- Edge-to-edge: pad root for system bars / display cutout ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    Math.max(bars.left, cutout.left),
                    Math.max(bars.top, cutout.top),
                    Math.max(bars.right, cutout.right),
                    Math.max(bars.bottom, cutout.bottom));
            return windowInsets;
        });

        // --- Services ---
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // --- Form views ---
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberCheck = findViewById(R.id.rememberCheck);
        CheckBox showPasswordCheck = findViewById(R.id.showPasswordCheck);
        enterButton = findViewById(R.id.login_enter);
        signupButton = findViewById(R.id.signupButton);

        // --- Remember me: pre-fill username from last session ---
        LoginRememberPreferences.loadIntoForm(this, usernameInput, rememberCheck);

        // --- Show password (plain text vs hidden) ---
        showPasswordCheck.setOnCheckedChangeListener((buttonView, isChecked) ->
                applyPasswordVisibility(isChecked));

        // --- Submit: empty check → offline "a"/a → dev → email validation → Firebase ---
        enterButton.setOnClickListener(v -> {
            String rawId = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (rawId.isEmpty() || password.isEmpty()) {
                Toast.makeText(Login.this, R.string.login_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            if ("a".equalsIgnoreCase(rawId) && "a".equals(password)) {
                UserSession.signIn(Login.this, "a");
                LoginRememberPreferences.save(Login.this, rememberCheck.isChecked(), rawId);
                Toast.makeText(Login.this, R.string.offline_login_success, Toast.LENGTH_SHORT).show();
                goToMain();
                return;
            }

            if ("admin".equalsIgnoreCase(rawId) && "admin123".equals(password)) {
                loginUser(rawId, password, rawId);
                return;
            }

            if (DevOfflineLogin.matches(rawId, password)) {
                UserSession.signIn(Login.this, DevOfflineLogin.OFFLINE_SESSION_EMAIL);
                LoginRememberPreferences.save(Login.this, rememberCheck.isChecked(), rawId);
                Toast.makeText(Login.this, R.string.dev_offline_login_success, Toast.LENGTH_SHORT).show();
                goToMain();
                return;
            }

            String email = AuthInputValidator.normalizeEmail(rawId);
            if (!AuthInputValidator.isValidEmail(email)) {
                Toast.makeText(Login.this, R.string.auth_invalid_email, Toast.LENGTH_SHORT).show();
                return;
            }
            // Block before Firebase: only NTU student mail host (my.ntu.ac.uk), not Gmail/Outlook/etc.
            if (!AuthInputValidator.isAllowedNtuLoginEmail(email)) {
                Toast.makeText(Login.this, R.string.auth_email_ntu_only, Toast.LENGTH_LONG).show();
                return;
            }

            loginUser(email, password, rawId);
        });

        signupButton.setOnClickListener(v ->
                startActivity(new Intent(Login.this, Signup.class)));

        TextView forgotPassword = findViewById(R.id.forgotPassword);
        forgotPassword.setOnClickListener(v -> {
            String rawId = usernameInput.getText().toString().trim();
            String email = AuthInputValidator.normalizeEmail(rawId);
            if (!AuthInputValidator.isValidEmail(email)) {
                Toast.makeText(Login.this, R.string.auth_invalid_email, Toast.LENGTH_SHORT).show();
                return;
            }
            // Same domain rule as sign-in so reset links are not sent to non-NTU inboxes.
            if (!AuthInputValidator.isAllowedNtuLoginEmail(email)) {
                Toast.makeText(Login.this, R.string.auth_email_ntu_only, Toast.LENGTH_LONG).show();
                return;
            }
            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(Login.this, R.string.auth_password_reset_sent, Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(Login.this, R.string.auth_password_reset_failed, Toast.LENGTH_LONG).show());
        });
    }

    private void applyPasswordVisibility(boolean visible) {
        int end = passwordInput.getSelectionEnd();
        if (visible) {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        int len = passwordInput.length();
        if (end >= 0 && end <= len) {
            passwordInput.setSelection(end);
        } else if (len > 0) {
            passwordInput.setSelection(len);
        }
    }

    private void loginUser(String email, String password, String rememberHint) {
        if ("admin".equalsIgnoreCase(email.trim()) && "admin123".equals(password)) {
            UserSession.signIn(this, email.trim().toLowerCase());
            LoginRememberPreferences.save(this, rememberCheck.isChecked(), rememberHint);
            Toast.makeText(this, R.string.offline_login_success, Toast.LENGTH_SHORT).show();
            goToMain();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    // Re-check using Firebase's canonical email (casing/provider quirks); reject if domain drifted.
                    String signedEmail = result.getUser() != null && result.getUser().getEmail() != null
                            ? result.getUser().getEmail()
                            : email;
                    if (!AuthInputValidator.isAllowedNtuLoginEmail(signedEmail)) {
                        auth.signOut();
                        UserSession.signOut(this);
                        Toast.makeText(this, R.string.auth_email_ntu_only, Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (result.getUser() != null && result.getUser().getEmail() != null) {
                        UserSession.signIn(this, result.getUser().getEmail());
                    } else {
                        UserSession.signIn(this, email);
                    }
                    LoginRememberPreferences.save(Login.this, rememberCheck.isChecked(), rememberHint);
                    Toast.makeText(Login.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    goToMain();
                })
                .addOnFailureListener(e -> tryLegacyFirestoreLogin(email, password, rememberHint, e));
    }

    /**
     * Supports accounts created before Firebase Auth (password stored only in Firestore).
     * Attempts silent migration to Firebase Auth for the next launch.
     */
    private void tryLegacyFirestoreLogin(String email, String password, String rememberHint,
            Exception authFailure) {
        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        toastAuthFailure(authFailure);
                        return;
                    }
                    String storedPassword = document.getString("password");
                    if (storedPassword != null && storedPassword.equals(password)) {
                        UserSession.signIn(Login.this, email);
                        establishFirebaseAuthAfterLegacyLogin(email, password, rememberHint);
                        return;
                    }
                    toastAuthFailure(authFailure);
                })
                .addOnFailureListener(netErr -> {
                    if (authFailure instanceof FirebaseNetworkException
                            || netErr.getCause() instanceof FirebaseNetworkException) {
                        Toast.makeText(Login.this, R.string.auth_network_error, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(Login.this, R.string.auth_login_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** User-facing errors for failed Firebase / network cases. */
    private void toastAuthFailure(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            Toast.makeText(this, R.string.auth_network_error, Toast.LENGTH_LONG).show();
        } else if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(this, R.string.auth_login_failed, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.auth_login_failed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Legacy accounts may validate via Firestore only. Firestore rules require {@code request.auth}
     * for profile/leaderboard updates, so we finish Firebase sign-in or create before opening the app.
     */
    private void establishFirebaseAuthAfterLegacyLogin(String email, String password, String rememberHint) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    // Same post-sign-in domain gate as loginUser (legacy row may predate this policy).
                    String signedEmail = result.getUser() != null && result.getUser().getEmail() != null
                            ? result.getUser().getEmail()
                            : email;
                    if (!AuthInputValidator.isAllowedNtuLoginEmail(signedEmail)) {
                        auth.signOut();
                        UserSession.signOut(Login.this);
                        Toast.makeText(Login.this, R.string.auth_email_ntu_only, Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (result.getUser() != null && result.getUser().getEmail() != null) {
                        UserSession.signIn(Login.this, result.getUser().getEmail());
                    }
                    LoginRememberPreferences.save(Login.this, rememberCheck.isChecked(), rememberHint);
                    Toast.makeText(Login.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    goToMain();
                })
                .addOnFailureListener(signInErr -> auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(result -> {
                            // Migration path: new Auth user must still be @my.ntu.ac.uk; else remove stray account.
                            String signedEmail = result.getUser() != null && result.getUser().getEmail() != null
                                    ? result.getUser().getEmail()
                                    : email;
                            if (!AuthInputValidator.isAllowedNtuLoginEmail(signedEmail)) {
                                if (result.getUser() != null) {
                                    result.getUser().delete();
                                }
                                auth.signOut();
                                UserSession.signOut(Login.this);
                                Toast.makeText(Login.this, R.string.auth_email_ntu_only, Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (result.getUser() != null && result.getUser().getEmail() != null) {
                                UserSession.signIn(Login.this, result.getUser().getEmail());
                            }
                            LoginRememberPreferences.save(Login.this, rememberCheck.isChecked(), rememberHint);
                            Toast.makeText(Login.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                            goToMain();
                        })
                        .addOnFailureListener(createErr -> {
                            if (createErr instanceof FirebaseAuthUserCollisionException) {
                                UserSession.signOut(Login.this);
                                Log.w(TAG, "Auth account exists but password did not match Firebase sign-in", signInErr);
                                Toast.makeText(Login.this, R.string.auth_login_failed, Toast.LENGTH_LONG).show();
                            } else {
                                Log.e(TAG, "Could not complete Firebase Auth after Firestore legacy login", createErr);
                                LoginRememberPreferences.save(Login.this, rememberCheck.isChecked(), rememberHint);
                                Toast.makeText(Login.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                                goToMain();
                            }
                        }));
    }

    /** Clears back stack and opens main shell ({@link navigation}). */
    private void goToMain() {
        Intent intent = new Intent(Login.this, navigation.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
