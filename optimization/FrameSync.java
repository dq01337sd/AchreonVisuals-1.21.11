/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ModInitializer
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.framesync;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameSync
implements ModInitializer {
    public static final String MOD_ID = "framesync";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"framesync");

    public void onInitialize() {
        LOGGER.info("FrameSync mod initialized!");
    }
}

