package com.razer.bianca;

/**
 * AIDL interface function definitions.
 * This aidl file needs to sync to the server side, which is the Nexus application.
 */
interface IControllerAIDLService {
    boolean doVibrate(int lowFreqMotor, int highFreqMotor);
    void onStartNeuronGame();
    void onStopNeuronGame();
}