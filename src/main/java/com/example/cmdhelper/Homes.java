package com.example.cmdhelper;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** 월드/서버별로 영구 저장되는 위치(홈) 기능 — sethome 플러그인과 비슷하게 사용 */
public final class Homes {
    /** 월드 키 → (이름 → 위치) */
    private static final Map<String, Map<String, Extras.Mark>> ALL = new LinkedHashMap<>();
    private static String currentKey = null;

    private Homes() {}

    static void init() { load(); }

    // ═════════════ 월드 구분 ═════════════
    /** 접속한 월드(싱글) 또는 서버 주소(멀티)를 구분하는 키. 리플렉션이라 버전이 바뀌어도 컴파일 오류가 없음 */
    private static String worldKey(Minecraft mc) {
        try {
            Object sp = mc.getClass().getMethod("getSingleplayerServer").invoke(mc);
            if (sp != null) {
                Object wd = sp.getClass().getMethod("getWorldData").invoke(sp);
                Object name = wd.getClass().getMethod("getLevelName").invoke(wd);
                return "싱글:" + clean(String.valueOf(name));
            }
        } catch (Throwable ignored) {}
        try {
            Object sd = mc.getClass().getMethod("getCurrentServer").invoke(mc);
            if (sd != null) {
                Object ip = sd.getClass().getField("ip").get(sd);
                return "서버:" + clean(String.valueOf(ip));
            }
        } catch (Throwable ignored) {}
        return "알수없음";
    }

    private static String clean(String s) { return s.replace('\t', ' ').replace('\n', ' ').trim(); }

    /** 월드가 바뀌었으면 그 월드의 저장된 위치를 불러옴 */
    static void refreshKey(Minecraft mc) {
        if (mc.player == null) return;
        String k = worldKey(mc);
        if (k.equals(currentKey)) return;
        currentKey = k;
        Extras.MARKS.clear();
        Map<String, Extras.Mark> m = ALL.get(k);
        if (m != null) Extras.MARKS.putAll(m);
    }

    static void persist() {
        if (currentKey == null) return;
        ALL.put(currentKey, new LinkedHashMap<>(Extras.MARKS));
        save();
    }

    // ═════════════ 명령어 ═════════════
    static void register(CommandDispatcher<FabricClientCommandSource> d) {
        for (String n : new String[] {"sethome", "mark"}) {
            d.register(ClientCommands.literal(n)
                .executes(c -> set(c.getSource(), "home"))
                .then(ClientCommands.argument("name", StringArgumentType.word())
                    .executes(c -> set(c.getSource(), StringArgumentType.getString(c, "name")))));
        }
        for (String n : new String[] {"home", "tpmark"}) {
            d.register(ClientCommands.literal(n)
                .executes(c -> go(c.getSource(), "home"))
                .then(ClientCommands.argument("name", StringArgumentType.word())
                    .executes(c -> go(c.getSource(), StringArgumentType.getString(c, "name")))));
        }
        for (String n : new String[] {"homes", "marks"}) {
            d.register(ClientCommands.literal(n).executes(c -> list(c.getSource())));
        }
        for (String n : new String[] {"delhome", "unmark"}) {
            d.register(ClientCommands.literal(n)
                .then(ClientCommands.literal("all").executes(c -> {
                    Minecraft mc = c.getSource().getClient();
                    refreshKey(mc);
                    int cnt = Extras.MARKS.size();
                    Extras.MARKS.clear();
                    persist();
                    Extras.msg(c.getSource(), "§7이 월드의 위치 " + cnt + "개를 모두 지웠습니다.");
                    return 1;
                }))
                .then(ClientCommands.argument("name", StringArgumentType.word()).executes(c -> {
                    refreshKey(c.getSource().getClient());
                    String name = StringArgumentType.getString(c, "name");
                    if (Extras.MARKS.remove(name) == null) { Extras.msg(c.getSource(), "§c그 이름의 위치가 없습니다."); return 0; }
                    persist();
                    Extras.msg(c.getSource(), "§7'" + name + "' 을(를) 삭제했습니다.");
                    return 1;
                })));
        }
    }

    private static int set(FabricClientCommandSource s, String name) {
        Minecraft mc = s.getClient();
        refreshKey(mc);
        if (name.length() > 32) { Extras.msg(s, "§c이름은 32글자 이하로 하세요."); return 0; }
        Player p = s.getPlayer();
        boolean overwrite = Extras.MARKS.containsKey(name);
        Extras.MARKS.put(name, new Extras.Mark(p.getX(), p.getY(), p.getZ(), p.level().dimension().identifier().toString()));
        persist();
        Extras.msg(s, "§a'" + name + "' 위치를 " + (overwrite ? "덮어써서 " : "") + "저장했습니다. §7(이 월드에 영구 저장, /home " + name + ")");
        return 1;
    }

    private static int go(FabricClientCommandSource s, String name) {
        Minecraft mc = s.getClient();
        refreshKey(mc);
        Extras.Mark m = Extras.MARKS.get(name);
        if (m == null) {
            Extras.msg(s, "§c'" + name + "' 위치가 없습니다. §7저장된 위치: " + (Extras.MARKS.isEmpty() ? "없음" : String.join(", ", Extras.MARKS.keySet())));
            return 0;
        }
        String cur = s.getPlayer().level().dimension().identifier().toString();
        if (!cur.equals(m.dim())) {
            Extras.msg(s, "§c그 위치는 다른 차원(" + m.dim() + ")에 있습니다. 그 차원으로 이동한 뒤 사용하세요.");
            return 0;
        }
        Extras.run(s, "tp @s " + m.x() + " " + m.y() + " " + m.z());
        Extras.msg(s, "§a'" + name + "' (으)로 이동합니다. §7(tp 명령어라 OP 권한 필요)");
        return 1;
    }

    private static int list(FabricClientCommandSource s) {
        Minecraft mc = s.getClient();
        refreshKey(mc);
        if (Extras.MARKS.isEmpty()) { Extras.msg(s, "§7이 월드에 저장된 위치가 없습니다. /sethome 이름"); return 1; }
        String cur = s.getPlayer().level().dimension().identifier().toString();
        Extras.msg(s, "§6[" + currentKey + "] 위치 " + Extras.MARKS.size() + "개");
        Extras.MARKS.forEach((k, v) -> Extras.msg(s,
            (cur.equals(v.dim()) ? "§b" : "§8") + k + " §f" + Extras.f(v.x()) + " " + Extras.f(v.y()) + " " + Extras.f(v.z()) + " §7(" + v.dim() + ")"));
        return 1;
    }

    // ═════════════ 파일 저장/불러오기 ═════════════
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("cmdhelper_homes.txt"); }

    private static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            for (String line : Files.readAllLines(f)) {
                String[] a = line.split("\t");
                if (a.length < 6) continue;
                ALL.computeIfAbsent(a[0], k -> new LinkedHashMap<>())
                   .put(a[1], new Extras.Mark(Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4]), a[5]));
            }
        } catch (Exception ignored) {}
    }

    private static void save() {
        try {
            StringBuilder sb = new StringBuilder();
            ALL.forEach((key, marks) -> marks.forEach((name, m) ->
                sb.append(key).append('\t').append(name).append('\t')
                  .append(m.x()).append('\t').append(m.y()).append('\t').append(m.z()).append('\t').append(m.dim()).append('\n')));
            Files.writeString(file(), sb.toString());
        } catch (Exception ignored) {}
    }
}
