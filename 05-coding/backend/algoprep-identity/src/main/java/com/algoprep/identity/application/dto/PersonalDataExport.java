package com.algoprep.identity.application.dto;

import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.model.MySettings;

/** What {@code identity} itself holds about the caller: no hash, no provider account details. */
public record PersonalDataExport(MyProfile profile, MySettings settings) {}
