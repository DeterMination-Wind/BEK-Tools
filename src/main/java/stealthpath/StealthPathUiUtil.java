package stealthpath;

import arc.Core;

/**
 * UI layout helpers for StealthPath.
 *
 * StealthPath 的 UI 布局小工具：把一些“纯 UI 尺寸计算”从主 Mod 类里拆出去，
 * 让核心逻辑文件更容易维护，同时不改变任何功能。
 */
final class StealthPathUiUtil{
    private StealthPathUiUtil(){}

    static float prefWidth(){
        return Math.min(Core.graphics.getWidth() / 1.2f, 560f);
    }
}

