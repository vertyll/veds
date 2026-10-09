# File scopes

How "how big may this be" has one answer per kind of file.

`FileScope` carries its own cap and type list, so "how big may this be" has one answer rather
than one per calling service. Attachments accept any type deliberately — a project may need a
format nobody anticipated — while avatars and icons do not. The declared size only produces an
early error; the real limit is signed in to the URL.
