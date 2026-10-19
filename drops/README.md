# Style drops

The app downloads `drops.json` from this folder about twice a day. Edit it and push to give every user:

- **styles**: new looks. Fields: `id`, `name`, `blurb`, `bg`, `bg2` (gradient end), `fg`, `accent`, `font` (`grotesk`, `serif`, `mono`), `radius`, `alpha` (100 = solid), `deco` (`gradient`, `glass`, `retro`, `rule`, `neon`, `scan`), and `until` (YYYY-MM-DD; after this the style leaves the picker, but widgets already using it keep it).
- **events**: popular countdowns shown in the countdown editor. Fields: `name`, `date` (YYYY-MM-DD), `regions` (optional: `IN`, `US`, `GB`, `CA`, `AU`, `EU`; leave out for everyone).

No app update needed. Keep the file valid JSON.
