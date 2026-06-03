# DBDPlugin Skript & Placeholder Reference

Ten dokument opisuje wszystkie dostępne placeholdery PlaceholderAPI oraz natywne warunki (conditions), wyrażenia (expressions) i efekty (effects) Skripta dostępne w DBDPlugin. Wiele starych placeholderów boolowskich zostało przeniesionych do czystych warunków Skripta dla lepszej czytelności.

## PlaceholderAPI (PAPI)

Placeholdery te mogą być użyte wszędzie gdzie wspierane jest PlaceholderAPI, np. w Action Barach, Scoreboardach czy przez `placeholder %string% from %player%` w Skripcie.

### Wartości tekstowe / liczbowe gracza
- `%dbd_souls%` / `%dbd_dusze%` — ilość dusz (liczba)
- `%dbd_dusze_formatted%` — dusze sformatowane (np. "12.5k")
- `%dbd_state%` / `%dbd_stan%` — stan zdrowia jako kolorowy tekst
- `%dbd_role%` / `%dbd_rola%` — rola jako tekst ("Killer", "Survivor", "Lobby")
- `%dbd_wins%` / `%dbd_wygrane%` — statystyki wygranych
- `%dbd_losses%` / `%dbd_przegrane%` — statystyki przegranych
- `%dbd_games%` / `%dbd_rozegrane%` — statystyki rozegranych gier

### Globalne (nie wymagają gracza)
- `%dbd_survivors%` — liczba ocalałych w grze
- `%dbd_generators%` — liczba generatorów do naprawy

### Ranking (Top/Pozycja)
- `%dbd_top_<typ>_<pozycja>_<value/name>%` (typy: souls, games, wins, losses) - zwraca wartość lub nick gracza z topki.
- `%dbd_pos_<typ>%` - zwraca pozycję gracza w rankingu.

---

## Natywny dodatek Skript (DBD Addon)

Poniższe elementy można używać bezpośrednio w plikach `.sk`. Zastępują one stare placeholdery boolowskie (np. `%dbd_is_healthy%`), pozwalając na czystszy kod.

### 🛡️ Warunki (Conditions)

Sprawdzanie stanu zdrowia gracza:
```applescript
%player% is healthy
%player% is injured
%player% is downed
%player% is hooked
%player% is carried
%player% is dead
%player% is in locker
%player% is escaped
```

Sprawdzanie roli gracza:
```applescript
%player% is a killer
%player% is a survivor
```

Sprawdzanie stanu gry:
```applescript
dbd game is lobby
dbd game is starting
dbd game is in game
dbd game is ended
```

Sprawdzanie statusów (Exhaustion itp.):
```applescript
%player% has dbd status "exhaustion"
%player% does not have dbd status "exposed"
```

### 📊 Wyrażenia (Expressions)

Statystyki:
```applescript
the dbd wins of player
the dbd losses of player
the dbd games of player
the dbd souls of player
```

Informacje:
```applescript
the dbd status of player
the dbd role of player
dbd active survivors count
dbd remaining generators count
```

Skillchecki (tylko w evencie `on dbd skill check`):
```applescript
skill check result # (zwraca "great", "good" lub "fail")
skill check type # (zwraca np. "GENERATOR", "CARRY", "HOOK_STRUGGLE")
```

### ⚡ Efekty Perk/Gra (Effects)

Skillcheck/Mechaniki:
```applescript
dbd set killer cooldown to 2 seconds
dbd set generator repair time to 90 seconds
dbd set hook stage time to 60 seconds
```

Generatory:
```applescript
dbd explode generator at {loc} losing 5 percent
dbd set generator progress at {loc} to 50
```

Stun/Oślepienie:
```applescript
dbd stun victim by attacker for 3 seconds
dbd blind victim for 2 seconds
dbd stun and blind victim for 3 seconds
```

Okna / Palety:
```applescript
dbd block window at {loc} for event-player for 15 seconds
dbd highlight window at {loc} for event-player for 5 seconds
```

Terror Radius / Scratch Marks (Aura / Undetectable / Ślady):
```applescript
dbd hide terror radius of event-player for 15 seconds # (Dla Killera: Niewykrywalny, Dla Surv: Niesłyszący)
dbd hide scratch marks of event-player for 10 seconds
dbd reveal aura of {target} to {viewer} for 5 seconds
```

Statusy (np. Przemęczenie, Narażenie):
```applescript
dbd apply "exhaustion" status to event-player for 40 seconds
dbd remove "exhaustion" status from event-player
```

Podnoszenie się (Recovery):
```applescript
dbd set recovery progress of event-player to 95
```

### 🎯 Eventy (Events)

- `on dbd skill check:` – wywoływane w momencie zakończenia skillchecka przez gracza. Dostępne wartości: `event-player`, `skill check result` (string), `skill check type` (string).
- Oraz wcześniejsze eventy z dbd: `on dbd game start:`, `on dbd generator complete:`, `on dbd player hook:` itd.
