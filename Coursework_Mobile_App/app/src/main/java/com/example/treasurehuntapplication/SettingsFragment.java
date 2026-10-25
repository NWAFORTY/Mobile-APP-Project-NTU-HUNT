package com.example.treasurehuntapplication;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Editable profile screen: name, location, course, pronouns; name/location sync to Firestore for leaderboard.
 */
public class SettingsFragment extends Fragment {

    private TextView nameView;
    private TextView locationView;
    private TextView courseView;
    private TextView pronounsView;
    private ImageButton editNameBtn;
    private ImageButton editLocationBtn;
    private ImageButton editCourseBtn;
    private ImageButton editPronounsBtn;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameView = view.findViewById(R.id.Profile_Name);
        locationView = view.findViewById(R.id.Profile_Location);
        courseView = view.findViewById(R.id.Profile_Course);
        pronounsView = view.findViewById(R.id.Profile_Pronouns);
        editNameBtn = view.findViewById(R.id.Name_Edit_Button);
        editLocationBtn = view.findViewById(R.id.Location_Edit_button);
        editCourseBtn = view.findViewById(R.id.Course_Edit_Button);
        editPronounsBtn = view.findViewById(R.id.Pronouns_Edit_Button);

        MaterialToolbar toolbar = view.findViewById(R.id.settingsToolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        editNameBtn.setOnClickListener(v -> showTextEditDialog(Field.NAME));
        editLocationBtn.setOnClickListener(v -> showTextEditDialog(Field.LOCATION));
        editCourseBtn.setOnClickListener(v -> showTextEditDialog(Field.COURSE));
        editPronounsBtn.setOnClickListener(v -> showTextEditDialog(Field.PRONOUNS));

        bindUiFromPrefs();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindUiFromPrefs();
    }

    private enum Field {
        NAME,
        LOCATION,
        COURSE,
        PRONOUNS
    }

    private void bindUiFromPrefs() {
        bindPrefText(nameView, ProfilePreferences.getDisplayName(requireContext()));
        bindPrefText(locationView, ProfilePreferences.getLocation(requireContext()));
        bindPrefText(courseView, ProfilePreferences.getCourse(requireContext()));
        bindPrefText(pronounsView, ProfilePreferences.getPronouns(requireContext()));
    }

    private void bindPrefText(TextView view, String value) {
        view.setText(value == null || value.isEmpty() ? getString(R.string.settings_not_set) : value);
    }

    private void showTextEditDialog(Field field) {
        int titleRes;
        String current;
        switch (field) {
            case NAME:
                titleRes = R.string.settings_edit_name_title;
                current = ProfilePreferences.getDisplayName(requireContext());
                break;
            case LOCATION:
                titleRes = R.string.settings_edit_location_title;
                current = ProfilePreferences.getLocation(requireContext());
                break;
            case COURSE:
                titleRes = R.string.settings_edit_course_title;
                current = ProfilePreferences.getCourse(requireContext());
                break;
            case PRONOUNS:
            default:
                titleRes = R.string.settings_edit_pronouns_title;
                current = ProfilePreferences.getPronouns(requireContext());
                break;
        }

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(current);
        input.setSelection(input.getText().length());

        NtuMaterialDialogs.builder(requireContext())
                .setTitle(titleRes)
                .setView(input)
                .setPositiveButton(R.string.settings_ok, (d, w) -> {
                    String v = input.getText() != null ? input.getText().toString().trim() : "";
                    switch (field) {
                        case NAME:
                            ProfilePreferences.setDisplayName(requireContext(), v);
                            UserProfileFirestoreSync.pushDisplayName(requireContext(), v);
                            break;
                        case LOCATION:
                            ProfilePreferences.setLocation(requireContext(), v);
                            UserProfileFirestoreSync.pushProfileLocation(requireContext(), v);
                            break;
                        case COURSE:
                            ProfilePreferences.setCourse(requireContext(), v);
                            break;
                        case PRONOUNS:
                            ProfilePreferences.setPronouns(requireContext(), v);
                            break;
                    }
                    bindUiFromPrefs();
                })
                .setNegativeButton(R.string.settings_cancel, (d, w) -> d.cancel())
                .show();
    }
}
