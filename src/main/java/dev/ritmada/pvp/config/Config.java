// Java 17+, runtime state + JSON persistence in .minecraft/config/ritmadapvp.json
package dev.ritmada.pvp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class Config {
    public static boolean enabled;
    public static boolean allowServers = true;
    public static Profile profile = Profile.PRO;
    public static PvpMode mode = PvpMode.BEAST;
    public static final Map<Profile, ProfileSettings> profiles = new EnumMap<>(Profile.class);
    public static final Map<PvpMode, Float> params = new EnumMap<>(PvpMode.class);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        for (Profile p : Profile.values()) profiles.put(p, p.defaults());
        for (PvpMode m : PvpMode.values()) params.put(m, m.def);
    }

    private static final class Data {
        String profile, mode;
        Boolean allowServers;
        Map<String, ProfileSettings> profiles = new HashMap<>();
        Map<String, Float> params = new HashMap<>();
    }

    private Config() {}

    public static ProfileSettings cur() { return profiles.get(profile); }
    public static float param(PvpMode m) { return params.get(m); }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("ritmadapvp.json"); }

    private static <E extends Enum<E>> E en(Class<E> c, String s, E def) {
        try { return Enum.valueOf(c, s); } catch (Exception e) { return def; }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d == null) return;
            if (d.allowServers != null) allowServers = d.allowServers;
            profile = en(Profile.class, d.profile, Profile.PRO);
            mode = en(PvpMode.class, d.mode, PvpMode.BEAST);
            d.profiles.forEach((k, v) -> { if (v != null) profiles.put(en(Profile.class, k, Profile.PRO), v); });
            d.params.forEach((k, v) -> { if (v != null) params.put(en(PvpMode.class, k, PvpMode.BEAST), v); });
        } catch (Exception ignored) { /* corrupt file -> keep defaults */ }
    }

    public static void save() {
        Data d = new Data();
        d.allowServers = allowServers;
        d.profile = profile.name();
        d.mode = mode.name();
        profiles.forEach((k, v) -> d.profiles.put(k.name(), v));
        params.forEach((k, v) -> d.params.put(k.name(), v));
        try (Writer w = Files.newBufferedWriter(file())) { GSON.toJson(d, w); } catch (Exception ignored) {}
    }
}
