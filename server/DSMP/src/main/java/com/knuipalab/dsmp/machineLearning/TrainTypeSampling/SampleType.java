package com.knuipalab.dsmp.machineLearning.TrainTypeSampling;

import lombok.Getter;

@Getter
public enum SampleType {

    TRAIN("train"),VALID("valid"),TEST("test");

    private final String typeString;

    SampleType(String typeString){
        this.typeString = typeString;
    }

}
