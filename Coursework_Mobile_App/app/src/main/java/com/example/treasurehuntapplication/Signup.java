package com.example.treasurehuntapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * New account registration: Firebase Auth user creation, initial {@code users/{email}} document
 * in Firestore (email, username placeholder, score 0), then return to {@link Login}.
 * <p>
 * Registration is refused unless the email is a valid {@code @my.ntu.ac.uk} student address
 * ({@link AuthInputValidator#isAllowedNtuLoginEmail}), same rule as {@link Login}.
 */
public class Signup extends AppCompatActivity {

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // --- Edge-to-edge insets ---
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

        // --- Services & fields ---
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        usernameInput = findViewById(R.id.signup_username);
        passwordInput = findViewById(R.id.signup_password);
        confirmPasswordInput = findViewById(R.id.signup_confirm_password);
        Button signupButton = findViewById(R.id.signup_create);
        Button backToLoginButton = findViewById(R.id.signup_back);

        signupButton.setOnClickListener(v -> createAccount());
        backToLoginButton.setOnClickListener(v -> startActivity(new Intent(Signup.this, Login.class)));
    }

    /** Validates input, creates Auth user, writes Firestore profile; rolls back Auth if Firestore fails. */
    private void createAccount() {
        String email = AuthInputValidator.normalizeEmail(usernameInput.getText().toString());
        String password = passwordInput.getText().toString();
        String confirm = confirmPasswordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, R.string.login_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!AuthInputValidator.isValidEmail(email)) {
            Toast.makeText(this, R.string.auth_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }
        // Align with Login: no Firebase account for Gmail, Outlook, staff-only ntu.ac.uk, etc.
        if (!AuthInputValidator.isAllowedNtuLoginEmail(email)) {
            Toast.makeText(this, R.string.auth_email_ntu_only, Toast.LENGTH_LONG).show();
            return;
        }
        if (!AuthInputValidator.isValidPasswordLength(password)) {
            Toast.makeText(this, getString(R.string.auth_password_too_short,
                    AuthInputValidator.MIN_PASSWORD_LENGTH), Toast.LENGTH_LONG).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    String rawId = user != null && user.getEmail() != null ? user.getEmail() : email;
                    String docId = AuthInputValidator.normalizeEmail(rawId);
                    Map<String, Object> profile = new HashMap<>();
                    if (user != null) {
                        profile.put("uid", user.getUid());
                    }
                    profile.put("email", docId);
                    profile.put("username", docId);
                    profile.put("score", 0L);

                    db.collection("users").document(docId).set(profile)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(Signup.this, R.string.account_created, Toast.LENGTH_SHORT).show();
                                goToLogin();
                            })
                            .addOnFailureListener(e -> {
                                if (user != null) {
                                    user.delete();
                                }
                                Toast.makeText(Signup.this, R.string.auth_firestore_profile_failed,
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(Signup.this, R.string.auth_signup_failed, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(Signup.this, R.string.auth_signup_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Returns to login without stacking duplicate activities. */
    private void goToLogin() {
        Intent i = new Intent(Signup.this, Login.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }
}
