package com.yungnickyoung.minecraft.yungscavebiomes.entity.sand_snapper.goal;

import com.yungnickyoung.minecraft.yungscavebiomes.entity.sand_snapper.SandSnapperEntity;
import com.yungnickyoung.minecraft.yungscavebiomes.module.TagModule;
import com.yungnickyoung.minecraft.yungscavebiomes.sandstorm.ISandstormServerDataProvider;
import com.yungnickyoung.minecraft.yungscavebiomes.sandstorm.SandstormServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmergeGoal extends Goal {
    private final SandSnapperEntity sandSnapper;
    private final float distanceFromPlayer;
    private final float sandstormDistanceFromPlayer;
    private final int cooldown;

    private int ticksRunning;
    private long lastUseTime;

    public EmergeGoal(SandSnapperEntity sandSnapper, float distanceFromPlayer, float sandstormDistanceFromPlayer, int cooldown) {
        this.sandSnapper = sandSnapper;
        this.distanceFromPlayer = distanceFromPlayer;
        this.sandstormDistanceFromPlayer = sandstormDistanceFromPlayer;
        this.cooldown = cooldown;

        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public void tick() {
        this.ticksRunning++;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticksRunning = 0;
        this.sandSnapper.setEmerging(true);
        this.sandSnapper.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.sandSnapper.setEmerging(false);
        this.lastUseTime = this.sandSnapper.tickCount;
    }

    @Override
    public boolean canUse() {
        if (this.sandSnapper.tickCount - this.lastUseTime < this.cooldown) {
            return false;
        }

        if (!this.sandSnapper.getLevel().getBlockState(this.sandSnapper.getOnPos()).is(TagModule.SAND_SNAPPER_BLOCKS)) return false;

        float halfWidth = 0.8f;
        Vec3 startPos = new Vec3(this.sandSnapper.getX() - (double)halfWidth, this.sandSnapper.getY() - 2.0f, this.sandSnapper.getZ() - (double)halfWidth);
        Vec3 endPos = new Vec3(this.sandSnapper.getX() + (double)halfWidth, this.sandSnapper.getY() - 0.6f, this.sandSnapper.getZ() + (double)halfWidth);

        AtomicBoolean intersectsAir = new AtomicBoolean(false);
        AABB emergeBox = new AABB(startPos, endPos);
        BlockPos.betweenClosedStream(emergeBox).forEach((pos) -> {
            BlockState blockState = this.sandSnapper.level.getBlockState(pos);

            if (blockState.isAir()) {
                intersectsAir.set(true);
            }
        });

        if (intersectsAir.get()) return false;

        SandstormServerData sandstormServerData = ((ISandstormServerDataProvider) this.sandSnapper.level).getSandstormServerData();

        float dist = sandstormServerData.isSandstormActive() ? this.sandstormDistanceFromPlayer : this.distanceFromPlayer;
        AABB searchBox = this.sandSnapper.getBoundingBox().inflate(dist / 2, 4.0f, dist / 2);
        List<Player> nearbyPlayers = this.sandSnapper.level.getNearbyPlayers(TargetingConditions.DEFAULT, this.sandSnapper, searchBox);

        return nearbyPlayers.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        SandstormServerData sandstormServerData = ((ISandstormServerDataProvider) this.sandSnapper.level).getSandstormServerData();

        float dist = sandstormServerData.isSandstormActive() ? this.sandstormDistanceFromPlayer : this.distanceFromPlayer;
        AABB searchBox = this.sandSnapper.getBoundingBox().inflate(dist / 2, 4.0f, dist / 2);
        List<Player> nearbyPlayers = this.sandSnapper.level.getNearbyPlayers(TargetingConditions.DEFAULT, this.sandSnapper, searchBox);

        return nearbyPlayers.isEmpty() && this.ticksRunning <= 73;
    }
}
