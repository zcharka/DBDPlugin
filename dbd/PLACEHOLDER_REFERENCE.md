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
# USTAWIANIE STANÓW GRACZA Z POZIOMU SKRIPTU (KOMENDA /dbdstate)
# ===========================================================================
#
# Placeholdery są ODCZYTYWALNE (read-only). Aby USTAWIĆ stan gracza
# (np. powalić go na ziemię), użyj komendy /dbdstate:
#
#   /dbdstate <gracz> <stan>
#
# Dostępne stany:
#   downed    — gracz leży/czołga się (DOWNED)
#   injured   — gracz jest ranny (INJURED)
#   healthy   — gracz jest zdrowy/uleczony (HEALTHY)
#   dead      — gracz jest martwy (DEAD)
#   hooked    — gracz na haku (HOOKED)
#   carried   — gracz niesiony (CARRIED)
#   in_locker — gracz w szafce (IN_LOCKER)
#
# Aliasy komendy: /dbdstan, /setstate
# Wymagane uprawnienia: dbd.admin
#
# ===========================================================================
# PRZYKŁADY UŻYCIA W SKRIPCIE
# ===========================================================================
#
# PRZYKŁAD 1: Natywne ustawianie stanu przez dedykowany Skript Effect (ZALECANE)
# -----------------------------------------------------------------------
#   # Składnia: dbd set state of %player% to %string%
#   dbd set state of player to "downed"
#   # Teraz %dbd_is_downed% zwróci "true" dla tego gracza
#
# PRZYKŁAD 2: Użycie komendy konsoli (ALTERNATYWA)
# -----------------------------------------------------------------------
#   execute console command "dbdstate %player% downed"
#
# ===========================================================================
# WŁASNE ZDARZENIA (EVENTS) W SKRIPCIE
# ===========================================================================
#
# Zamiast polegać na skomplikowanych skryptach działających w pętli,
# plugin natywnie wspiera następujące zdarzenia w Skrypcie:
#
# 1. Zdejmowanie z haka:
# -----------------------------------------------------------------------
#   on player unhook:
#       # event-player = osoba, która ratuje (Rescuer)
#       # unhooked player = osoba, która była powieszona (Target)
#       send "Uratowałeś %unhooked player%!" to event-player
#
# 2. Ukończenie naprawy generatora:
# -----------------------------------------------------------------------
#   on generator complete:
#       # event-player = osoba, która dokończyła naprawę (wbiła 100%)
#       broadcast "%event-player% uruchomił generator!"
# 3. Zranienie ocalałego przez killera:
# -----------------------------------------------------------------------
#   on player injured:
#       # event-player = ranny Ocalały
#       # event-killer = Killer, który zadał cios
#       send "Zostałeś zraniony przez %event-killer%!" to event-player
#
# 4. Powalenie ocalałego przez killera (leży na ziemi):
# -----------------------------------------------------------------------
#   on player downed:
#       # event-player = powalony Ocalały
#       # event-killer = Killer, który powalił
#       send "Zostałeś powalony przez %event-killer%!" to event-player
#
# 5. Śmierć gracza (na haku):
# -----------------------------------------------------------------------
#   on player death:
#       # event-player = Ocalały, który zginął
#       broadcast "Gracz %event-player% został poświęcony!"
#
# 6. Ogłuszenie Killera (np. paletą lub latarką z poziomu Javy):
# -----------------------------------------------------------------------
#   on killer stun:
#       # event-player = Ogłuszony Killer
#       # event-survivor = Ocalały, który go ogłuszył
#       send "Ogłuszyłeś Killera: %event-player%!" to event-survivor
#
# ===========================================================================
# SPECJALNE EFEKTY SKRYPTOWE (EFFECTS)
# ===========================================================================
#
# Oprócz zmieniania stanów "dbd set state...", dodano specjalne efekty
# wpływające bezpośrednio na grę.
#
# 1. Ogłuszanie Killera (Stun / Blind):
# -----------------------------------------------------------------------
# Twarde ogłuszenie (Slowness VI):
#   dbd stun %player% for 3 seconds
#   dbd stun %player% by %player% for 4 seconds
#
# Oślepienie latarką (Blindness):
#   dbd blind %player% by %player% for 2.5 seconds
#
# Kombo (Ogłuszenie + Oślepienie):
#   dbd stun and blind %player% by %player% for 5 seconds
#
# UWAGA: Użycie tych efektów, jeśli Killer kogoś niesie, automatycznie
# wyrzuci ofiarę na ziemię (tzw. Pallet / Flashlight Save) i uwolni gracza!
#
# ===========================================================================
# PRZYKŁAD 4: Kombinacja odczytu i zapisu
# -----------------------------------------------------------------------
#   on right click on player:
#       set {_isDowned} to placeholder "dbd_is_downed" from clicked player
#       set {_isSurvivor} to placeholder "dbd_is_survivor" from player
#       if {_isDowned} is "true":
#           if {_isSurvivor} is "true":
#               execute console command "dbdstate %clicked player% healthy"
#               send "Podnosisz gracza %clicked player%!" to player
#
# ===========================================================================
# ŹRÓDŁO KODU: src/main/java/pl/dbd/util/PAPIExpansion.java
#              src/main/java/pl/dbd/commands/StateCommand.java
#              src/main/java/pl/dbd/skript/SkriptEvents.java
# ===========================================================================
