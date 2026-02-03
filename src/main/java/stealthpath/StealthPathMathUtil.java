package stealthpath;

import arc.graphics.Color;

import static mindustry.Vars.tilesize;

/**
 * Small math/format helpers used by StealthPath.
 *
 * StealthPath 的通用小工具（数值/坐标/颜色解析）。
 * 这些方法原本都堆在 {@link StealthPathMod} 里，抽出来后主类更短、更好维护；
 * 行为保持一致。
 */
final class StealthPathMathUtil{
    private StealthPathMathUtil(){}

    static boolean tryParseHexColor(String text, Color out){
        if(text == null) return false;
        String value = text.trim();
        if(value.startsWith("#")){
            value = value.substring(1);
        }

        int len = value.length();
        if(len != 6 && len != 8) return false;

        try{
            long parsed = Long.parseLong(value, 16);
            if(len == 6){
                float r = ((parsed >> 16) & 0xff) / 255f;
                float g = ((parsed >> 8) & 0xff) / 255f;
                float b = (parsed & 0xff) / 255f;
                out.set(r, g, b, 1f);
            }else{
                float r = ((parsed >> 24) & 0xff) / 255f;
                float g = ((parsed >> 16) & 0xff) / 255f;
                float b = ((parsed >> 8) & 0xff) / 255f;
                float a = (parsed & 0xff) / 255f;
                out.set(r, g, b, a);
            }
            return true;
        }catch(Throwable ignored){
            return false;
        }
    }

    static float tileToWorld(int tile){
        return tile * tilesize;
    }

    static int worldToTile(float world){
        return Math.round(world / tilesize);
    }

    static boolean inBounds(ThreatMap map, int x, int y){
        return x >= 0 && y >= 0 && x < map.width && y < map.height;
    }

    static int clamp(int value, int min, int max){
        return Math.max(min, Math.min(max, value));
    }
}

