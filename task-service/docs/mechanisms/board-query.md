# Board query

How one board page is read in a fixed number of statements.

`TaskQueryAdapter` is why CQRS is applied here. One row needs the task, its status label, its
category labels, its assignees and its comment count — five sources. The page is one statement
and the labels are resolved with three more **for the whole page**, not per row.

A label missing in the requested language falls back to whatever the author wrote, reported in
`nameLanguage`. Dropping the row instead would make the chip vanish and look like a deletion.
