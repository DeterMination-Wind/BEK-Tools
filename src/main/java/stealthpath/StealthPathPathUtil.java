package stealthpath;

import arc.struct.IntSeq;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.gen.Unit;

/**
 * Path utilities for StealthPath.
 *
 * StealthPath 的路径工具：把“路径压缩/坐标转换/路径哈希”等和主逻辑解耦，
 * 让 {@link StealthPathMod} 更专注于事件与状态管理。
 *
 * 注意：这里只做拆分与注释补充，不改变算法与输出格式。
 */
final class StealthPathPathUtil{
    private StealthPathPathUtil(){}

    static Seq<Pos> toWorldPoints(IntSeq tilePath, int width, Unit unit, Building target){
        Seq<Pos> out = new Seq<>();
        out.add(new Pos(unit.x, unit.y));

        for(int i = 0; i < tilePath.size; i++){
            int idx = tilePath.items[i];
            int tx = idx % width;
            int ty = idx / width;
            out.add(new Pos(StealthPathMathUtil.tileToWorld(tx), StealthPathMathUtil.tileToWorld(ty)));
        }

        out.add(new Pos(target.x, target.y));
        return out;
    }

    static Seq<Pos> toWorldPointsFromTiles(IntSeq tilePath, int width, Building target){
        Seq<Pos> out = new Seq<>();
        if(tilePath == null || tilePath.isEmpty()) return out;

        for(int i = 0; i < tilePath.size; i++){
            int idx = tilePath.items[i];
            int tx = idx % width;
            int ty = idx / width;
            out.add(new Pos(StealthPathMathUtil.tileToWorld(tx), StealthPathMathUtil.tileToWorld(ty)));
        }

        if(target != null){
            out.add(new Pos(target.x, target.y));
        }

        return out;
    }

    static Seq<Pos> toWorldPointsFromTilesWithStart(IntSeq tilePath, int width, float startWorldX, float startWorldY, Building target){
        Seq<Pos> out = new Seq<>();
        out.add(new Pos(startWorldX, startWorldY));

        if(tilePath != null && !tilePath.isEmpty()){
            for(int i = 0; i < tilePath.size; i++){
                int idx = tilePath.items[i];
                int tx = idx % width;
                int ty = idx / width;
                out.add(new Pos(StealthPathMathUtil.tileToWorld(tx), StealthPathMathUtil.tileToWorld(ty)));
            }
        }

        if(target != null){
            out.add(new Pos(target.x, target.y));
        }

        return out;
    }

    /**
     * Compact a tile path by keeping only the turning points.
     *
     * 将 tile 路径压缩为“转折点”序列（去掉同方向的中间点），用于减少 waypoint 数量与渲染点数。
     */
    static IntSeq compactPath(IntSeq path, int width){
        if(path == null || path.size <= 2) return path;

        IntSeq out = new IntSeq();
        out.add(path.items[0]);

        int prev = path.items[0];
        int prevDx = 0, prevDy = 0;

        for(int i = 1; i < path.size; i++){
            int cur = path.items[i];
            int px = prev % width;
            int py = prev / width;
            int cx = cur % width;
            int cy = cur / width;

            int dx = Integer.compare(cx, px);
            int dy = Integer.compare(cy, py);

            if(i == 1){
                prevDx = dx;
                prevDy = dy;
            }else if(dx != prevDx || dy != prevDy){
                out.add(prev);
                prevDx = dx;
                prevDy = dy;
            }

            prev = cur;
        }

        out.add(path.peek());
        return out;
    }

    /**
     * Hashes a tile path into a coarse signature for follow-mode deduping.
     *
     * 将路径压成一个“粗哈希”，用于跟随模式：如果路径基本没变，就不重复下发 RTS 指令。
     */
    static int hashWaypointPath(IntSeq tilePath, int width){
        if(tilePath == null || tilePath.isEmpty()) return 0;

        IntSeq compact = compactPath(tilePath, width);
        if(compact == null || compact.isEmpty()) compact = tilePath;

        int maxWaypoints = StealthPathMod.rtsMaxWaypoints();
        int step = Math.max(1, compact.size / maxWaypoints);

        int h = 1;
        for(int i = 0; i < compact.size; i += step){
            h = 31 * h + compact.items[i];
        }

        int last = compact.items[compact.size - 1];
        h = 31 * h + last;
        h = 31 * h + compact.size;
        return h;
    }
}

