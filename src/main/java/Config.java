import com.moandjiezana.toml.Toml;

public class Config {

    private static final Toml toml = new Toml().read(Config.class.getResourceAsStream("app.secret.toml"));

    public static String getToken() {
        String environment = System.getenv("env");

        return toml.getString("discord.token." + environment);
    }
}
