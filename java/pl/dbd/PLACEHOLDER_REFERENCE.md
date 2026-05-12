# ===========================================================================
# REFERENCJA PLACEHOLDERÓW DBD – DLA AI AGENTÓW I SKRYPTÓW
# ===========================================================================
#
# Ten plik opisuje placeholdery zarejestrowane w pluginie Java (DBDPlugin)
# poprzez PlaceholderAPI. Każdy placeholder jest dostępny w Skripcie
# bez potrzeby skript-reflect.
#
# WYMAGANE PLUGINY: PlaceholderAPI, skript-placeholders (lub mvdw-placeholders)
#
# ===========================================================================
# JAK UŻYWAĆ PLACEHOLDERÓW W SKRIPCIE:
# ===========================================================================
#
# Sposób 1 (skript-placeholders):
#   set {_val} to placeholder "dbd_is_injured" from player
#   if {_val} is "true":
#       # gracz jest ranny
#
# Sposób 2 (parse placeholder):
#   set {_val} to "%parse placeholder ""dbd_is_injured"" from player%"
#   if {_val} is "true":
#       # gracz jest ranny
#
# UWAGA: Wszystkie boolowskie placeholdery zwracają TEKST "true" lub "false"
#         (nie typ boolean). Porównuj zawsze z tekstem "true".
#
# ===========================================================================
# BOOLOWSKIE PLACEHOLDERY – STANY ZDROWIA GRACZA
# ===========================================================================
#
# ┌─────────────────────┬───────────────────────────────────────────────────┐
# │ Placeholder         │ Opis                                             │
# ├─────────────────────┼───────────────────────────────────────────────────┤
# │ %dbd_is_healthy%    │ true gdy gracz jest ZDROWY (HEALTHY)              │
# │ %dbd_is_injured%    │ true gdy gracz jest RANNY (INJURED)               │
# │ %dbd_is_downed%     │ true gdy gracz jest POWALONY/LEŻY (DOWNED)        │
# │ %dbd_is_hooked%     │ true gdy gracz jest NA HAKU (HOOKED)              │
# │ %dbd_is_carried%    │ true gdy gracz jest NIESIONY (CARRIED)            │
# │ %dbd_is_dead%       │ true gdy gracz jest MARTWY (DEAD)                 │
# │ %dbd_is_in_locker%  │ true gdy gracz jest W SZAFCE (IN_LOCKER)          │
# │ %dbd_is_escaped%    │ true gdy gracz UCIEKŁ (w escapedPlayers)          │
# └─────────────────────┴───────────────────────────────────────────────────┘
#
# ===========================================================================
# BOOLOWSKIE PLACEHOLDERY – ROLA GRACZA
# ===========================================================================
#
# ┌─────────────────────┬───────────────────────────────────────────────────┐
# │ Placeholder         │ Opis                                             │
# ├─────────────────────┼───────────────────────────────────────────────────┤
# │ %dbd_is_survivor%   │ true gdy gracz jest Survivorem                    │
# │ %dbd_is_killer%     │ true gdy gracz jest Killerem                      │
# │ %dbd_is_in_game%    │ true gdy gracz jest w meczu (Killer LUB Survivor) │
# └─────────────────────┴───────────────────────────────────────────────────┘
#
# ===========================================================================
# BOOLOWSKIE PLACEHOLDERY – STAN MECZU (GLOBALNY)
# ===========================================================================
#
# ┌──────────────────────────┬──────────────────────────────────────────────┐
# │ Placeholder              │ Opis                                        │
# ├──────────────────────────┼──────────────────────────────────────────────┤
# │ %dbd_game_is_lobby%      │ true gdy mecz jest w stanie LOBBY            │
# │ %dbd_game_is_starting%   │ true gdy mecz jest w stanie STARTING         │
# │ %dbd_game_is_in_game%    │ true gdy mecz jest w stanie IN_GAME          │
# │ %dbd_game_is_ended%      │ true gdy mecz jest w stanie ENDED            │
# └──────────────────────────┴──────────────────────────────────────────────┘
#
# ===========================================================================
# TEKSTOWE / LICZBOWE PLACEHOLDERY
# ===========================================================================
#
# ┌──────────────────────────┬──────────────────────────────────────────────┐
# │ Placeholder              │ Opis                                        │
# ├──────────────────────────┼──────────────────────────────────────────────┤
# │ %dbd_souls%              │ Ilość dusz gracza (liczba)                   │
# │ %dbd_dusze%              │ Alias dla %dbd_souls%                        │
# │ %dbd_dusze_formatted%    │ Dusze sformatowane (np. "12.5k", "3M")       │
# │ %dbd_state%              │ Stan zdrowia jako kolorowy tekst              │
# │ %dbd_stan%               │ Alias dla %dbd_state%                        │
# │ %dbd_role%               │ Rola: "Killer", "Survivor" lub "Lobby"       │
# │ %dbd_rola%               │ Alias dla %dbd_role%                         │
# │ %dbd_wins%               │ Ilość wygranych                              │
# │ %dbd_losses%             │ Ilość przegranych                            │
# │ %dbd_games%              │ Ilość rozegranych gier                       │
# └──────────────────────────┴──────────────────────────────────────────────┘
#
# ===========================================================================
# KOMPLETNE PRZYKŁADY DLA AI AGENTÓW
# ===========================================================================
#
# PRZYKŁAD 1: Sprawdzenie czy mecz trwa (zamiast {dbd::state::game_status})
# -----------------------------------------------------------------------
# Placeholder: %dbd_game_is_in_game%
# Jak zastosować w skripcie: porównaj wynik placeholder z tekstem "true"
# Efekt: warunek jest spełniony TYLKO gdy mecz jest aktywny (IN_GAME)
#
#   every 1 second:
#       loop all players:
#           set {_inGame} to placeholder "dbd_game_is_in_game" from loop-player
#           if {_inGame} is "true":
#               # ta logika wykonuje się tylko gdy mecz trwa
#
# PRZYKŁAD 2: Sprawdzenie czy gracz jest Killerem (zamiast dbd_getPlayerState)
# -----------------------------------------------------------------------
# Placeholder: %dbd_is_killer%
# Jak zastosować w skripcie: porównaj wynik placeholder z tekstem "true"
# Efekt: warunek jest spełniony TYLKO gdy gracz jest Killerem w aktywnym meczu
#
#   on right click:
#       set {_isKiller} to placeholder "dbd_is_killer" from player
#       if {_isKiller} is "true":
#           # logika dla Killera
#
# PRZYKŁAD 3: Sprawdzenie stanu zdrowia (zamiast {dbd::state::player::%p%::health})
# -----------------------------------------------------------------------
# Placeholder: %dbd_is_downed%
# Jak zastosować w skripcie: porównaj wynik placeholder z tekstem "true"
# Efekt: warunek jest spełniony TYLKO gdy gracz jest powalony (leży na ziemi)
#
#   on right click on player:
#       set {_isDowned} to placeholder "dbd_is_downed" from event-player
#       if {_isDowned} is "true":
#           # gracz leży - można go podnieść
#
# PRZYKŁAD 4: Kombinacja warunków
# -----------------------------------------------------------------------
#   on damage of player:
#       set {_gameOn} to placeholder "dbd_game_is_in_game" from attacker
#       set {_attackerIsKiller} to placeholder "dbd_is_killer" from attacker
#       set {_victimIsSurvivor} to placeholder "dbd_is_survivor" from victim
#       if {_gameOn} is "true":
#           if {_attackerIsKiller} is "true":
#               if {_victimIsSurvivor} is "true":
#                   # atak Killera na Survivora w trakcie meczu
#
# ===========================================================================
# ŹRÓDŁO KODU: src/main/java/pl/dbd/util/PAPIExpansion.java
# ===========================================================================
