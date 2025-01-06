package com.limelight.computers;

import androidx.annotation.NonNull;

import com.limelight.nvstream.http.ComputerDetails;

public class ComputerUuidChangedException extends Exception {
    @NonNull
    public final ComputerDetails oldDetails;
    @NonNull
    public final ComputerDetails newDetails;

    ComputerUuidChangedException(@NonNull ComputerDetails oldDetails, @NonNull ComputerDetails newDetails) {
        this.oldDetails = oldDetails;
        this.newDetails = newDetails;
    }
}
