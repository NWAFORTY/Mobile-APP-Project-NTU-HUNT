package com.example.treasurehuntapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * In-app support form: submits category + message to Firestore {@code support_reports}
 * when the user is signed in with Firebase Auth.
 */
public class SupportFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ensure the form scroll layer draws above the gradient header (matches design overlap).
        View scroll = view.findViewById(R.id.supportScroll);
        ViewGroup root = (ViewGroup) view;
        Runnable liftScrollLayer = () -> root.bringChildToFront(scroll);
        liftScrollLayer.run();
        scroll.post(liftScrollLayer);

        Spinner spinner = view.findViewById(R.id.supportCategorySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.support_categories, R.layout.support_spinner_item);
        adapter.setDropDownViewResource(R.layout.support_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        TextInputEditText messageInput = view.findViewById(R.id.supportMessageInput);
        MaterialButton submit = view.findViewById(R.id.supportSubmitButton);

        submit.setOnClickListener(v -> {
            CharSequence msg = messageInput.getText();
            String body = msg != null ? msg.toString().trim() : "";
            if (body.isEmpty()) {
                Toast.makeText(requireContext(), R.string.support_empty_message, Toast.LENGTH_SHORT).show();
                return;
            }
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(requireContext(), R.string.support_failed, Toast.LENGTH_LONG).show();
                return;
            }
            String category = spinner.getSelectedItem() != null
                    ? spinner.getSelectedItem().toString()
                    : "";

            Map<String, Object> report = new HashMap<>();
            report.put("category", category);
            report.put("message", body);
            report.put("userEmail", UserSession.getEmail(requireContext()));
            report.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance()
                    .collection("support_reports")
                    .add(report)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(requireContext(), R.string.support_sent, Toast.LENGTH_LONG).show();
                        messageInput.setText("");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), R.string.support_failed, Toast.LENGTH_LONG).show());
        });
    }
}
