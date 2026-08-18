/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 *  SPDX-License-Identifier: BSD-3-Clause-Clear
 *
 */

package org.codeaurora.bluetooth.offload_testapp;
import java.util.*;

public class OffloadCharacteristics {

    String deviceAddress = null;
    UUID serviceUUID;
    List<UUID> charUUIDs;
    long endpointId;
    long hubId;
    int sessionId = 0;
}
