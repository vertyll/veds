# Localized labels

How category and status names written by users are stored and served in several languages.

Category and status names are written by users, in whichever languages they need.

- **Completeness is required on the way in**, in `TranslationCompletenessValidator`, not in the
  aggregate constructor. Languages are seeded data — enforcing "all of them" during
  reconstitution would mean the day a language is added, every stored row becomes invalid and
  the next write to it throws.
- **On the way out** `resolveFor` returns the requested language or whatever the author wrote,
  and the response reports which in `nameLanguage`. There is no key to render here: a category
  is identified by a UUID, and an identifier on screen names nothing.
