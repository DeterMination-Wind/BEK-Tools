package radialbuildmenu;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SettingsMenuDialog;

/**
 * RBM settings UI helpers (extracted from the monolithic mod class).
 *
 * RBM 设置界面相关的小组件。
 * 这些类原本是 {@link RadialBuildMenuMod} 的内部类，拆出来以减少主文件体积；
 * 行为保持一致，不改功能。
 */
final class SubHeaderSetting extends SettingsMenuDialog.SettingsTable.Setting{
    private final String titleKeyOrText;

    SubHeaderSetting(String titleKeyOrText){
        super("rbm-subheader");
        this.titleKeyOrText = titleKeyOrText;
    }

    @Override
    public void add(SettingsMenuDialog.SettingsTable table){
        table.row();
        table.add(titleKeyOrText.startsWith("@") ? Core.bundle.get(titleKeyOrText.substring(1)) : titleKeyOrText)
            .color(Color.gray)
            .padTop(8f)
            .padBottom(2f)
            .left()
            .growX()
            .minWidth(0f)
            .wrap();
        table.row();
    }
}

final class AdvancedButtonSetting extends SettingsMenuDialog.SettingsTable.Setting{
    private final RadialBuildMenuMod mod;

    AdvancedButtonSetting(RadialBuildMenuMod mod){
        super("rbm-advanced");
        this.mod = mod;
    }

    @Override
    public void add(SettingsMenuDialog.SettingsTable table){
        float prefWidth = RadialBuildMenuMod.prefWidth();
        Table root = table.table(Tex.button, t -> {
            t.left().margin(10f);
            t.image(mindustry.gen.Icon.settings).size(20f).padRight(8f);
            t.add(title, Styles.outlineLabel).left().growX().minWidth(0f).wrap();

            TextButton btn = t.button("@rbm.advanced.open", Styles.flatt, mod::showAdvancedDialog)
                .width(190f)
                .height(40f)
                .padLeft(10f)
                .get();

            btn.update(() -> btn.setDisabled(!Core.settings.getBool(RadialBuildMenuMod.keyProMode, false)));
        }).width(prefWidth).padTop(6f).get();

        addDesc(root);
        table.row();
    }
}

final class WideSliderSetting extends SettingsMenuDialog.SettingsTable.Setting{
    private final int def, min, max, step;
    private final SettingsMenuDialog.StringProcessor sp;

    WideSliderSetting(String name, int def, int min, int max, int step, SettingsMenuDialog.StringProcessor sp){
        super(name);
        this.def = def;
        this.min = min;
        this.max = max;
        this.step = step;
        this.sp = sp;
    }

    @Override
    public void add(SettingsMenuDialog.SettingsTable table){
        arc.scene.ui.Slider slider = new arc.scene.ui.Slider(min, max, step, false);
        slider.setValue(Core.settings.getInt(name, def));

        arc.scene.ui.Label value = new arc.scene.ui.Label("", Styles.outlineLabel);

        Table content = new Table();
        content.add(title, Styles.outlineLabel).left().growX().minWidth(0f).wrap();
        content.add(value).padLeft(10f).right();
        content.margin(3f, 33f, 3f, 33f);
        content.touchable = Touchable.disabled;

        slider.changed(() -> {
            Core.settings.put(name, (int)slider.getValue());
            value.setText(sp.get((int)slider.getValue()));
        });

        slider.change();

        // leave room for the vertical scrollbar on the right side
        addDesc(table.stack(slider, content).width(RadialBuildMenuMod.prefWidth() - 64f).left().padTop(4f).get());
        table.row();
    }
}

