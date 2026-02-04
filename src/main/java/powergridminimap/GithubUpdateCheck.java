package powergridminimap;

import arc.Core;
import arc.util.Http;
import arc.util.Log;
import arc.util.Strings;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.ui.dialogs.BaseDialog;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks this mod's version against the version declared in the GitHub repository.
 * Runs asynchronously and should be triggered once per game startup.
 */
final class GithubUpdateCheck{
    private static final String owner = "DeterMination-Wind";
    private static final String repo = "Power-Grid-Minimap-";
    private static final String modName = "powergrid-minimap";

    private static final Pattern numberPattern = Pattern.compile("\\d+");
    private static boolean checked;

    private static final String keyUpdateCheckEnabled = "pgmm-updatecheck";
    private static final String keyUpdateCheckShowDialog = "pgmm-updatecheck-dialog";
    private static final String keyUpdateCheckLastAt = "pgmm-updatecheck-lastAt";
    private static final String keyUpdateCheckIgnoreVersion = "pgmm-updatecheck-ignore";
    //avoid hammering GitHub; also prevents repeated prompts when user returns to main menu etc.
    private static final long checkIntervalMs = 6L * 60L * 60L * 1000L; //6 hours

    private GithubUpdateCheck(){
    }

    static void applyDefaults(){
        Core.settings.defaults(keyUpdateCheckEnabled, true);
        Core.settings.defaults(keyUpdateCheckShowDialog, true);
    }

    static String enabledKey(){
        return keyUpdateCheckEnabled;
    }

    static String showDialogKey(){
        return keyUpdateCheckShowDialog;
    }

    static void checkOnce(){
        if(checked) return;
        checked = true;

        if(Vars.headless) return;
        if(Vars.mods == null) return;

        applyDefaults();
        if(!Core.settings.getBool(keyUpdateCheckEnabled, true)) return;

        long now = System.currentTimeMillis();
        long last = Core.settings.getLong(keyUpdateCheckLastAt, 0L);
        if(last > 0L && now - last < checkIntervalMs) return;
        Core.settings.put(keyUpdateCheckLastAt, now);

        Mods.LoadedMod mod = Vars.mods.getMod(modName);
        if(mod == null || mod.meta == null) return;
        String current = Strings.stripColors(mod.meta.version);
        if(current == null || current.isEmpty()) return;

        String ignored = Strings.stripColors(Core.settings.getString(keyUpdateCheckIgnoreVersion, ""));

        //Prefer GitHub releases API (includes html_url and tag), fall back to raw mod.json for older repos.
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";
        Http.get(apiUrl)
        .error(e -> {
            //ignore; fallback below
            checkFromRawModJson(mod, current, ignored);
        })
        .submit(res -> {
            try{
                Jval json = Jval.read(res.getResultAsString());
                String tag = Strings.stripColors(json.getString("tag_name", ""));
                String htmlUrl = Strings.stripColors(json.getString("html_url", ""));
                if(htmlUrl == null || htmlUrl.isEmpty()){
                    htmlUrl = "https://github.com/" + owner + "/" + repo + "/releases/latest";
                }

                String latest = tag == null ? "" : tag.trim();
                if(latest.startsWith("v") || latest.startsWith("V")){
                    latest = latest.substring(1);
                }
                if(latest.isEmpty()){
                    //Some repos may not use tags; try reading version from name/body as a last resort.
                    latest = Strings.stripColors(json.getString("name", ""));
                }

                if(latest == null || latest.isEmpty()){
                    checkFromRawModJson(mod, current, ignored);
                    return;
                }

                if(ignored != null && !ignored.isEmpty() && compareVersions(latest, ignored) == 0){
                    return;
                }

                if(compareVersions(latest, current) > 0){
                    notifyUpdate(mod, current, latest, htmlUrl);
                }
            }catch(Throwable t){
                checkFromRawModJson(mod, current, ignored);
            }
        });
    }

    private static void checkFromRawModJson(Mods.LoadedMod mod, String current, String ignored){
        String url = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/main/src/main/resources/mod.json";
        Http.get(url)
        .error(e -> {
            //No internet/offline/etc. -> silently skip.
        })
        .submit(res -> {
            try{
                Jval json = Jval.read(res.getResultAsString());
                String latest = Strings.stripColors(json.getString("version", ""));
                if(latest == null || latest.isEmpty()) return;
                if(ignored != null && !ignored.isEmpty() && compareVersions(latest, ignored) == 0) return;

                if(compareVersions(latest, current) > 0){
                    String releasesUrl = "https://github.com/" + owner + "/" + repo + "/releases/latest";
                    notifyUpdate(mod, current, latest, releasesUrl);
                }
            }catch(Throwable ignoredErr){
                //Bad JSON/format/etc. -> skip.
            }
        });
    }

    private static void notifyUpdate(Mods.LoadedMod mod, String current, String latest, String url){
        Log.info("[{0}] Update available: {1} -> {2}", mod.meta.displayName, current, latest);

        Core.app.post(() -> {
            if(Vars.ui == null) return;

            if(!Core.settings.getBool(keyUpdateCheckShowDialog, true)){
                Vars.ui.showInfoToast(mod.meta.displayName + ": " + current + " -> " + latest + " (GitHub)", 8f);
                return;
            }

            BaseDialog dialog = new BaseDialog(mod.meta.displayName + " 更新");
            dialog.cont.margin(12f);
            dialog.cont.add("发现新版本： " + current + " -> " + latest).padBottom(10f).row();
            dialog.cont.add("是否打开 GitHub Release 页面下载？").width(520f).wrap().row();

            dialog.buttons.defaults().size(180f, 54f).pad(6f);
            dialog.buttons.button("打开", () -> Core.app.openURI(url));
            dialog.buttons.button("忽略此版本", () -> {
                Core.settings.put(keyUpdateCheckIgnoreVersion, latest);
                dialog.hide();
            });
            dialog.addCloseButton();
            dialog.show();
        });
    }

    private static int compareVersions(String a, String b){
        int[] pa = parseVersionParts(a);
        int[] pb = parseVersionParts(b);
        int max = Math.max(pa.length, pb.length);
        for(int i = 0; i < max; i++){
            int ai = i < pa.length ? pa[i] : 0;
            int bi = i < pb.length ? pb[i] : 0;
            if(ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private static int[] parseVersionParts(String v){
        if(v == null) return new int[0];
        Matcher m = numberPattern.matcher(v);
        ArrayList<Integer> parts = new ArrayList<>();
        while(m.find()){
            try{
                parts.add(Integer.parseInt(m.group()));
            }catch(Throwable ignored){
            }
        }
        int[] out = new int[parts.size()];
        for(int i = 0; i < parts.size(); i++) out[i] = parts.get(i);
        return out;
    }
}
