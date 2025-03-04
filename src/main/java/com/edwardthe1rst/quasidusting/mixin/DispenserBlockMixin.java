package com.edwardthe1rst.quasidusting.mixin;

import com.edwardthe1rst.quasidusting.QuasiDustProperties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {

    @Inject(method = "appendProperties", at = @At("TAIL"))
    private void addCustomProperties(StateManager.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(
            QuasiDustProperties.DUST_NORTH,
            QuasiDustProperties.DUST_SOUTH,
            QuasiDustProperties.DUST_EAST,
            QuasiDustProperties.DUST_WEST,
            QuasiDustProperties.DUST_UP,
            QuasiDustProperties.DUST_DOWN
        );
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void modifyDefaultState(Block.Settings settings, CallbackInfo ci) {
        DispenserBlock self = (DispenserBlock) (Object) this;

        BlockState defaultState = self.getDefaultState()
            .with(QuasiDustProperties.DUST_NORTH, false)
            .with(QuasiDustProperties.DUST_SOUTH, false)
            .with(QuasiDustProperties.DUST_EAST, false)
            .with(QuasiDustProperties.DUST_WEST, false)
            .with(QuasiDustProperties.DUST_UP, false)
            .with(QuasiDustProperties.DUST_DOWN, false);

        ((BlockAccessor) self).invokeSetDefaultState(defaultState);
    }

    @Inject(method = "neighborUpdate", at = @At("HEAD"), cancellable = true)
    private void modifyDispenserPowerCheck(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify, CallbackInfo ci) {
        boolean bl = world.isReceivingRedstonePower(pos) || 
            (state.get(QuasiDustProperties.DUST_NORTH) ? world.isReceivingRedstonePower(pos.north()) : false) ||
            (state.get(QuasiDustProperties.DUST_SOUTH) ? world.isReceivingRedstonePower(pos.south()) : false) ||
            (state.get(QuasiDustProperties.DUST_EAST) ? world.isReceivingRedstonePower(pos.east()) : false) ||
            (state.get(QuasiDustProperties.DUST_WEST) ? world.isReceivingRedstonePower(pos.west()) : false) ||
            (state.get(QuasiDustProperties.DUST_UP) ? world.isReceivingRedstonePower(pos.up()) : false) ||
            (state.get(QuasiDustProperties.DUST_DOWN) ? world.isReceivingRedstonePower(pos.down()) : false);
        boolean bl2 = (Boolean)state.get(DispenserBlock.TRIGGERED);
        if (bl && !bl2) {
           world.scheduleBlockTick(pos, state.getBlock(), 4);
           world.setBlockState(pos, (BlockState)state.with(DispenserBlock.TRIGGERED, true), 2);
        } else if (!bl && bl2) {
           world.setBlockState(pos, (BlockState)state.with(DispenserBlock.TRIGGERED, false), 2);
        }
        ci.cancel();
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void addRedstoneDust(
        BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir
    ) {
        ItemStack heldItem = player.getMainHandStack();

        if (heldItem.isOf(Items.REDSTONE)) {
            Direction clickedFace = hit.getSide();

            BooleanProperty dustProperty = getProperty(clickedFace);
            if (dustProperty != null && !state.get(dustProperty)) {
                if (!world.isClient) {
                    world.setBlockState(pos, state.with(dustProperty, true));

                    if (!player.isCreative()) {
                        heldItem.decrement(1);
                    }

                    world.playSound(null, pos, state.getSoundGroup().getPlaceSound(), player.getSoundCategory(), 1.0F, 1.0F);
                }

                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }

    @Unique
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
    @Inject(method = "rotate", at = @At("HEAD"), cancellable = true)
    private void rotateState(BlockState state, BlockRotation rotation, CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(state
            .with(QuasiDustProperties.DUST_NORTH, state.get(QuasiDustProperties.DUST_WEST))
            .with(QuasiDustProperties.DUST_SOUTH, state.get(QuasiDustProperties.DUST_EAST))
            .with(QuasiDustProperties.DUST_EAST, state.get(QuasiDustProperties.DUST_NORTH))
            .with(QuasiDustProperties.DUST_WEST, state.get(QuasiDustProperties.DUST_SOUTH))
            .with(QuasiDustProperties.DUST_UP, state.get(QuasiDustProperties.DUST_UP)) // No change for up/down
            .with(QuasiDustProperties.DUST_DOWN, state.get(QuasiDustProperties.DUST_DOWN)) // No change for up/down
        );
    }

    @Inject(method = "mirror", at = @At("HEAD"), cancellable = true)
    private void mirrorState(BlockState state, BlockMirror mirror, CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(state
            .with(QuasiDustProperties.DUST_NORTH, state.get(QuasiDustProperties.DUST_SOUTH))
            .with(QuasiDustProperties.DUST_SOUTH, state.get(QuasiDustProperties.DUST_NORTH))
            .with(QuasiDustProperties.DUST_EAST, state.get(QuasiDustProperties.DUST_WEST))
            .with(QuasiDustProperties.DUST_WEST, state.get(QuasiDustProperties.DUST_EAST))
        );
    }
}
