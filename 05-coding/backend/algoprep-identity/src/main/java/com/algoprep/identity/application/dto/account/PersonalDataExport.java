package com.algoprep.identity.application.dto.account;

import com.algoprep.identity.domain.model.account.MyProfile;
import com.algoprep.identity.domain.model.account.MySettings;

/** What {@code identity} itself holds about the caller: no hash, no provider account details. */
public record PersonalDataExport(MyProfile profile, MySettings settings) {}
