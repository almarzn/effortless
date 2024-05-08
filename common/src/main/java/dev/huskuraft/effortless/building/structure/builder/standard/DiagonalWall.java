package dev.huskuraft.effortless.building.structure.builder.standard;

import java.util.ArrayList;
import java.util.stream.Stream;

import dev.huskuraft.effortless.api.core.BlockInteraction;
import dev.huskuraft.effortless.api.core.BlockPosition;
import dev.huskuraft.effortless.api.core.Player;
import dev.huskuraft.effortless.api.math.MathUtils;
import dev.huskuraft.effortless.building.Context;
import dev.huskuraft.effortless.building.structure.builder.AbstractBlockStructure;

public class DiagonalWall extends AbstractBlockStructure {

    // add diagonal wall from first to second
    public static Stream<BlockPosition> collectDiagonalWallBlocks(Context context) {
        var list = new ArrayList<BlockPosition>();

        var x1 = context.getPosition(0).x();
        var y1 = context.getPosition(0).y();
        var z1 = context.getPosition(0).z();
        var x2 = context.getPosition(1).x();
        var y2 = context.getPosition(1).y();
        var z2 = context.getPosition(1).z();
        var x3 = context.getPosition(2).x();
        var y3 = context.getPosition(2).y();
        var z3 = context.getPosition(2).z();

        // get diagonal line blocks
        var diagonalLineBlocks = DiagonalLine.collectPlaneDiagonalLineBlocks(context, 1).toList();

        int lowest = MathUtils.min(y1, y3);
        int highest = MathUtils.max(y1, y3);

        // copy diagonal line on y axis
        for (int y = lowest; y <= highest; y++) {
            for (BlockPosition blockPosition : diagonalLineBlocks) {
                list.add(new BlockPosition(blockPosition.x(), y, blockPosition.z()));
            }
        }

        return list.stream();
    }

    protected BlockInteraction trace(Player player, Context context, int index) {
        return switch (index) {
            case 0 -> Single.traceSingle(player, context);
            case 1 -> Floor.traceFloor(player, context);
            case 2 -> Line.traceLineY(player, context.getPosition(1));
            default -> null;
        };
    }

    protected Stream<BlockPosition> collect(Context context, int index) {
        return switch (index) {
            case 1 -> Single.collectSingleBlocks(context);
            case 2 -> DiagonalLine.collectPlaneDiagonalLineBlocks(context, 1);
            case 3 -> DiagonalWall.collectDiagonalWallBlocks(context);
            default -> Stream.empty();
        };
    }


    @Override
    public int traceSize(Context context) {
        return 3;
    }
}
