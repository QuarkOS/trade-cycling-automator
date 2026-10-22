package dev.tradecycling.automator.picker;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import java.util.Optional;

/**
 * Job-site memory is not reliably synced to the client. The block itself is.
 */
public final class JobSiteFinder {
    private static final int RADIUS = 6;

    private JobSiteFinder() {
    }

    public static BlockPos find(Minecraft client, Villager villager) {
        if (client.level == null || villager == null) {
            return null;
        }
        Player player = client.player;
        BlockPos origin = villager.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        boolean bestInReach = false;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = client.level.getBlockState(pos);
                    Optional<Holder<PoiType>> poi = PoiTypes.forState(state);
                    if (poi.isEmpty() || !villager.getVillagerData().profession().value().heldJobSite().test(poi.get())) {
                        continue;
                    }
                    boolean inReach = player != null && player.isWithinBlockInteractionRange(pos, 0.0);
                    double distance = villager.distanceToSqr(Vec3.atCenterOf(pos));
                    boolean better = best == null
                            || (inReach && !bestInReach)
                            || (inReach == bestInReach && distance < bestDistance);
                    if (better) {
                        best = pos.immutable();
                        bestDistance = distance;
                        bestInReach = inReach;
                    }
                }
            }
        }
        return best;
    }
}
