package carpet.mixins;

import carpet.fakes.LevelInterface;
import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.world.level.redstone.NeighborUpdater;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(Level.class)
public abstract class Level_tickMixin implements LevelInterface
{
    @Shadow @Final public boolean isClientSide;
    @Shadow @Final protected NeighborUpdater neighborUpdater;
    CarpetProfiler.ProfilerToken currentSection;
    CarpetProfiler.ProfilerToken entitySection;

    Map<EntityType<?>, Entity> precookedMobs = new HashMap<>();

    @Override
    @Unique
    public NeighborUpdater getNeighborUpdater() {
        return this.neighborUpdater;
    }

    public Map<EntityType<?>, Entity> getPrecookedMobs()
    {
        return precookedMobs;
    }

    @Inject(method = "tickBlockEntities", at = @At("HEAD"))
    private void startBlockEntities(CallbackInfo ci) {
        currentSection = CarpetProfiler.start_section((Level) (Object) this, "Block Entities", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickBlockEntities", at = @At("TAIL"))
    private void endBlockEntities(CallbackInfo ci) {
        CarpetProfiler.end_current_section(currentSection);
    }
/*
    @Inject(method = "tickBlockEntities", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_5562;method_31704()Z",
            shift = At.Shift.BEFORE,
            ordinal = 0
    ))
    private void startTileEntitySection(CallbackInfo ci, Profiler profiler_1, Iterator i, class_5562 lv)
    {
        entitySection = CarpetProfiler.start_block_entity_section((World)(Object)this, (BlockEntity) lv, CarpetProfiler.TYPE.TILEENTITY);
    }

    @Redirect(method = "tickBlockEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_5562;method_31704()Z",
            ordinal = 0
    ))   // isRemoved()
    private boolean checkProcessTEs(class_5562 class_5562)
    {
        return class_5562.method_31704() || !TickSpeed.process_entities; // blockEntity can be NULL? happened once with fake player
    }

    @Inject(method = "tickBlockEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_5562;method_31703()V",
            shift = At.Shift.AFTER,
            ordinal = 0
    ))
    private void endTileEntitySection(CallbackInfo ci)
    {
         CarpetProfiler.end_current_entity_section(entitySection);
    }
*/
    @Inject(method = "guardEntityTick", at = @At("HEAD"), cancellable = true)
    private void startEntity(Consumer<Entity> consumer_1, Entity e, CallbackInfo ci)
    {
        // this shows that probably tick speed controller needs to be accessible through level referring to servers on server and client on clientLevel
        if (isClientSide) {
            if (!(TickSpeed.process_entitiesClient() || (e instanceof Player) || TickSpeed.isIs_superHotClient() && e.getControllingPassenger() instanceof Player))
            {
                ci.cancel();
            }
        } else {
            if (!(TickSpeed.gTRM().map(trm -> trm.process_entities).orElse(true) || (e instanceof Player)))
            {
                ci.cancel();
            }
        }
        entitySection =  CarpetProfiler.start_entity_section((Level) (Object) this, e, CarpetProfiler.TYPE.ENTITY);
    }

    @Inject(method = "guardEntityTick", at = @At("TAIL"))
    private void endEntity(Consumer<Entity> call, Entity e, CallbackInfo ci) {
        CarpetProfiler.end_current_entity_section(entitySection);
    }


}
