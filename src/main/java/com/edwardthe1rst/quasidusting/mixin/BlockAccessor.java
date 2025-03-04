package com.edwardthe1rst.quasidusting.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Block.class)
public interface BlockAccessor {

    /**
     * Allows mixins to call setDefaultState, which is normally protected.
     */
    @Invoker("setDefaultState")
    void invokeSetDefaultState(BlockState state);
}
