package com.limelight.computers;

import androidx.annotation.NonNull;

import com.limelight.nvstream.http.ComputerDetails;

/**
 * Same as {@link ComputerManagerListener} except with {@link #notifyComputerRemoved(ComputerDetails)}
 */
public interface ComputerManagerListener2 extends ComputerManagerListener{
    void notifyComputerRemoved(@NonNull ComputerDetails details);
}
