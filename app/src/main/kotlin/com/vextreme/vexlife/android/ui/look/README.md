# VexLook — how VexLife looks and moves

**Human situation:** VexLife is a companion you live with, so how it looks should be yours. A
person should be able to pick a mood (or grow one from a single colour), choose whether things
move, and have every feature follow at once. Nothing else needs to change.

**Current truth:** one `VexLookPreference(choice, motion, ambient)` State, owned by
`VexLookFamily` and saved on the phone through `VexLookStore`. ThemeMode still owns light/dark;
Look owns palette, display face, shape and motion.

## Pieces

| Source | Owns |
|---|---|
| `VexTone.kt` | OKLCH tone → sRGB with gamut mapping. All Look colour is generated, never hand-picked per role. |
| `VexLook.kt` | Presets (Nocturne, Aurora, Ember, Tide, Paper), `Custom(hue)`, Motion, the preference State, and `VexLookSpec` → full M3 `ColorScheme` / `Typography` / `Shapes`. |
| `VexLookFamily.kt` | Choose → stay or admit + `beginLatest(Save)` → `Act.Save(store)`. Store observation shares the selection fence. |
| `VexLookAccess.kt` | `LocalVexLook` (read-only) and `rememberVexLook()` (process-lifetime ViewModel). |
| `VexAmbientField.kt` | The soft light field behind `AppFrame`. Warms while music plays and drifts while navigating; still under Calm and absent when ambient is off. |

The Appearance surface is `SettingsScreen` (Elements `AppearanceSection`, `LookTile`, `HueSwatch`,
`MotionLively`, `MotionCalm`, `AmbientToggle`). Each Look tile is a miniature painted in its own
scheme and face, so the choice is made by looking, not by reading.

## Adding a Look

1. Add a `VexLookPreset` entry with name and mood strings.
2. Give it `VexLookSeeds` (primary/secondary/tertiary/neutral hue + chroma), a `VexDisplayFace`,
   a corner radius and an ambient intensity in `VexLookPreset.spec()`.
3. `VexLookTest` checks contrast for every preset in dark and light automatically; add a golden in
   `VexLookVisualGoldenTest` so the Look is seen, not just compiled.

## Proof

- `VexLookTest`: OKLCH round trip, gamut, text contrast ≥ 7 (body) / ≥ 4.5 (variants) for every
  Look in dark and light, no Material baseline colour leaks, key parsing.
- `VexLookFamilyTest`: immediate admission with a latest Save, settlement ordering, store
  commit/failure, and observation that never reverts an unsettled choice.
- `VexLookVisualGoldenTest`: the Appearance screen plus each Look driving Home.

Fonts (Sora, Space Grotesk, Fraunces) are OFL-licensed static instances; licences live in
`docs/licenses/fonts/`.
