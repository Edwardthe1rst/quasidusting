package com.edwardthe1rst.quasidusting.mixin;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.edwardthe1rst.quasidusting.QuasiDustProperties;

@Mixin(AbstractBlock.class)
public abstract class AbstractBlockMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void addRedstoneDust(
        BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir
    ) {
        if (!(state.getBlock() instanceof PistonBlock)) {
            return;
        }

        ItemStack heldItem = player.getMainHandStack(); // Get the item in the player's main hand

        // Check if the player is holding redstone dust
        if (heldItem.isOf(Items.REDSTONE)) {
            Direction clickedFace = hit.getSide(); // The face the player clicked

            // Determine the corresponding BooleanProperty for the clicked face
            BooleanProperty dustProperty = getProperty(clickedFace);
            if (dustProperty != null && !state.get(dustProperty)) { // Only apply if dust is not already there
                if (!world.isClient) {
                    // Update the block state to place dust on the clicked face
                    world.setBlockState(pos, state.with(dustProperty, true));

                    // Consume the redstone dust (except in creative mode)
                    if (!player.isCreative()) {
                        heldItem.decrement(1);
                    }

                    // Play placement sound (similar to redstone dust)
                    world.playSound(null, pos, state.getSoundGroup().getPlaceSound(), player.getSoundCategory(), 1.0F, 1.0F);
                }

                // Indicate the action was successful
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }

    /**
     * Returns the corresponding BooleanProperty for a given direction.
     */
    private BooleanProperty getProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> QuasiDustProperties.DUST_NORTH;
            case SOUTH -> QuasiDustProperties.DUST_SOUTH;
            case EAST -> QuasiDustProperties.DUST_EAST;
            case WEST -> QuasiDustProperties.DUST_WEST;
            case UP -> QuasiDustProperties.DUST_UP;
            case DOWN -> QuasiDustProperties.DUST_DOWN;
            default -> null;
        };
    }
}
