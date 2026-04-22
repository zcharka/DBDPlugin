package pl.dbd.integration;

import org.bukkit.event.Listener;
import pl.dbd.DBDPlugin;

public class GetCaseIntegration implements Listener {
    private final DBDPlugin plugin;

    public GetCaseIntegration(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    // Tymczasowo wyłączono obsługę CaseOpenEvent, aby projekt mógł się skompilować bez API GetCase w Mavenie.
}