package com.edwardthe1rst.quasidusting.mixin;

import com.edwardthe1rst.quasidusting.QuasiDustProperties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonExtensionBlock;
import net.minecraft.block.enums.PistonType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin {
    private Direction direction;
    private BlockState strippedState;
    private BlockState sourceState;
    
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
    private void modifyDefaultState(boolean sticky, Block.Settings settings, CallbackInfo ci) {
        PistonBlock self = (PistonBlock) (Object) this;

        BlockState defaultState = self.getDefaultState()
            .with(QuasiDustProperties.DUST_NORTH, false)
            .with(QuasiDustProperties.DUST_SOUTH, false)
            .with(QuasiDustProperties.DUST_EAST, false)
            .with(QuasiDustProperties.DUST_WEST, false)
            .with(QuasiDustProperties.DUST_UP, false)
            .with(QuasiDustProperties.DUST_DOWN, false);

        ((BlockAccessor) self).invokeSetDefaultState(defaultState);
    }

    @Inject(method = "onSyncedBlockEvent", at = @At("HEAD"), cancellable = true)
    private void getSourceState(BlockState state, World world, BlockPos pos, int type, int data, CallbackInfoReturnable<Boolean> cir) {
        this.direction = state.get(PistonBlock.FACING);
        this.strippedState = world.getBlockState(pos).with(PistonBlock.EXTENDED, false);
        this.sourceState = (BlockState)((BlockState)Blocks.MOVING_PISTON.getDefaultState().with(PistonExtensionBlock.FACING, direction)).with(PistonExtensionBlock.TYPE, (strippedState.getBlock() == Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT);
    }

    @Inject(method = "onSyncedBlockEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;updateNeighbors(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V"), cancellable = true)
    private void modifySourceState(BlockState state, World world, BlockPos pos, int type, int data, CallbackInfoReturnable<Boolean> cir) {
        world.removeBlockEntity(pos);
        world.addBlockEntity(PistonExtensionBlock.createBlockEntityPiston(pos, this.sourceState, this.strippedState, direction, false, true));

    }

    @Inject(method = "shouldExtend", at = @At("HEAD"), cancellable = true)
    private void modifyPistonPowerCheck(RedstoneView world, BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = world.getBlockState(pos);
        boolean powered = world.getReceivedRedstonePower(pos) > 0;

        // If no direct power, check for quasi-connectivity based on enabled dust sides
        if (!powered) {
            for (Direction dir : Direction.values()) {
                BooleanProperty property = getProperty(dir);
                if (property != null && state.contains(property) && state.get(property)) {
                    BlockPos checkPos = pos.offset(dir);
                    if (world.isReceivingRedstonePower(checkPos)) {
                        powered = true;
                        break;
                    }
                }
            }
        }

        cir.setReturnValue(powered);
    }

    /**
     * Returns the corresponding BooleanProperty for a given direction.
     */
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
