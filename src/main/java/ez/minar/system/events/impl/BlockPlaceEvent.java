package ez.minar.system.events.impl;

import ez.minar.system.events.Event;
import lombok.Getter;
import net.minecraft.util.math.BlockPos;

public class BlockPlaceEvent extends Event {
    @Getter private final BlockPos pos;

    public BlockPlaceEvent(BlockPos pos) {
        this.pos = pos;
    }
}
