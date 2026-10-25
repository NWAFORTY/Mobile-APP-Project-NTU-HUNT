package com.example.treasurehuntapplication;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Shared NTU-styled {@link MaterialAlertDialogBuilder} and window polish for custom views. */
public final class NtuMaterialDialogs {

    private NtuMaterialDialogs() {
    }

    @NonNull
    public static MaterialAlertDialogBuilder builder(@NonNull Context context) {
        return new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_NTU_MaterialAlertDialog);
    }

    /**
     * Use with {@link MaterialAlertDialogBuilder#setView} so the red-bordered card (or hint layout)
     * isn’t clipped by the default dialog panel.
     */
    public static void applyCustomContentWindow(@NonNull AlertDialog dialog, float dimAmount) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(dimAmount);
        }
    }
}
